/*
 * Copyright 2019 ThingsCloud.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package link.thingscloud.xswitch.esl.inbound;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import link.thingscloud.xswitch.esl.IEslClient;
import link.thingscloud.xswitch.esl.IEslEventListener;
import link.thingscloud.xswitch.esl.constant.EslConstants;
import link.thingscloud.xswitch.esl.internal.EslClientConfig;
import link.thingscloud.xswitch.esl.internal.IEslProtocolListener;
import link.thingscloud.xswitch.esl.transport.CommandResponse;
import link.thingscloud.xswitch.esl.transport.SendMsg;
import link.thingscloud.xswitch.esl.transport.event.EslEvent;
import link.thingscloud.xswitch.esl.transport.message.EslMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : <a href="mailto:ant.zhou@aliyun.com">zhouhailin</a>
 */
public class EslInboundClient implements IEslClient {


    private AtomicBoolean authenticatorResponded = new AtomicBoolean(false);

    private boolean authenticated;
    private Channel channel;

    private final List<IEslEventListener> eventListeners = new CopyOnWriteArrayList<>();

    private final Executor eventListenerExecutor;
    private final Executor backgroundJobListenerExecutor;

    private final Bootstrap bootstrap = new Bootstrap();

    private final EventLoopGroup workerGroup;

    private final EslClientConfig clientConfig;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public EslInboundClient(EslClientConfig clientConfig) {

        this.clientConfig = clientConfig;

        eventListenerExecutor = new ScheduledThreadPoolExecutor(clientConfig.getEventListenerThreads(),
                new BasicThreadFactory.Builder().namingPattern("EslEventNotifier-%d").daemon(true).build());

        backgroundJobListenerExecutor = new ScheduledThreadPoolExecutor(clientConfig.getBackgroudJobListenerThreads(),
                new BasicThreadFactory.Builder().namingPattern("EslBackgroundJobNotifier-%d").daemon(true).build());

        this.workerGroup = new NioEventLoopGroup(clientConfig.getWorkerThreads(), new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("Esl-worker-group-%s", this.threadIndex.incrementAndGet()));
            }
        });

    }

    @Override
    public IEslClient start() {
        if (this.canSend()) {
            return this;
        }
        this.bootstrap.group(this.workerGroup)
                .channel(NioSocketChannel.class)
                .remoteAddress(this.clientConfig.getHost(), this.clientConfig.getPort())
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.clientConfig.getConnectTimeoutMillis())
                .option(ChannelOption.SO_SNDBUF, this.clientConfig.getSndBufSize())
                .option(ChannelOption.SO_RCVBUF, this.clientConfig.getRcvBufSize())
                .handler(new InboundClientChannelInitializer(clientConfig.getPassword(), protocolListener));

        connect();

        return this;
    }

    @Override
    public IEslClient stop() {

        this.authenticatorResponded.set(false);

        if (this.channel != null && this.channel.isActive()) {
            this.channel.close();
        }

        this.channel = null;
        return this;

    }

    @Override
    public IEslClient shutdown() {
        stop();

        workerGroup.shutdownGracefully();
        return this;
    }

    @Override
    public boolean canSend() {
        return channel != null && channel.isActive() && authenticated;
    }


    private void connect() {
        log.info("begin to success remote host[{}:{}] asynchronously", clientConfig.getHost(), clientConfig.getPort());

        // Attempt connection
        bootstrap.connect().addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                channel = future.channel();
                log.info("begin to success remote host[{} -> {}] success.", channel.localAddress(), channel.remoteAddress());
            } else {
                channel = null;
                log.info("begin to success remote host[{}:{}] failed, cause : ", clientConfig.getHost(), clientConfig.getPort(), future.cause());
                connect();
            }
        });
    }

    @Override
    public IEslClient addEventListener(IEslEventListener listener) {
        if (listener != null) {
            eventListeners.add(listener);
        }
        return this;
    }


    /**
     * Sends a FreeSWITCH API command to the server and blocks, waiting for an immediate response from the
     * server.
     * <p/>
     * The outcome of the command from the server is retured in an {@link EslMessage} object.
     *
     * @param command API command to send
     * @param arg     command arguments
     * @return an {@link EslMessage} containing command results
     */
    public EslMessage sendSyncApiCommand(String command, String arg) {
        checkConnected();
        InboundClientHandler handler = (InboundClientHandler) channel.pipeline().last();
        StringBuilder sb = new StringBuilder();
        if (command != null && !command.isEmpty()) {
            sb.append("api ");
            sb.append(command);
        }
        if (arg != null && !arg.isEmpty()) {
            sb.append(' ');
            sb.append(arg);
        }

        return handler.sendSyncSingleLineCommand(channel, sb.toString());
    }

    /**
     * Submit a FreeSWITCH API command to the server to be executed in background mode. A synchronous
     * response from the server provides a UUID to identify the job execution results. When the server
     * has completed the job execution it fires a BACKGROUND_JOB Event with the execution results.<p/>
     * Note that this Client must be subscribed in the normal way to BACKGOUND_JOB Events, in order to
     * receive this event.
     *
     * @param command API command to send
     * @param arg     command arguments
     * @return String Job-UUID that the server will tag result event with.
     */
    public String sendAsyncApiCommand(String command, String arg) {
        checkConnected();
        InboundClientHandler handler = (InboundClientHandler) channel.pipeline().last();
        StringBuilder sb = new StringBuilder();
        if (command != null && !command.isEmpty()) {
            sb.append("bgapi ");
            sb.append(command);
        }
        if (arg != null && !arg.isEmpty()) {
            sb.append(' ');
            sb.append(arg);
        }

        return handler.sendAsyncCommand(channel, sb.toString());
    }

    /**
     * Set the current event subscription for this connection to the server.  Examples of the events
     * argument are:
     * <pre>
     *   ALL
     *   CHANNEL_CREATE CHANNEL_DESTROY HEARTBEAT
     *   CUSTOM conference::maintenance
     *   CHANNEL_CREATE CHANNEL_DESTROY CUSTOM conference::maintenance sofia::register sofia::expire
     * </pre>
     * Subsequent calls to this method replaces any previous subscriptions that were set.
     * </p>
     * Note: current implementation can only process 'plain' events.
     *
     * @param format can be { plain | xml }
     * @param events { all | space separated list of events }
     * @return a {@link CommandResponse} with the server's response.
     */
    public CommandResponse setEventSubscriptions(String format, String events) {
        // temporary hack
        if (!StringUtils.equals(EslConstants.PLAIN, format)) {
            throw new IllegalStateException("Only 'plain' event format is supported at present");
        }

        checkConnected();
        InboundClientHandler handler = (InboundClientHandler) channel.pipeline().last();
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(format)) {
            sb.append("event ");
            sb.append(format);
        }
        if (StringUtils.isNotBlank(events)) {
            sb.append(' ');
            sb.append(events);
        }
        EslMessage response = handler.sendSyncSingleLineCommand(channel, sb.toString());

        return new CommandResponse(sb.toString(), response);
    }

    /**
     * Cancel any existing event subscription.
     *
     * @return a {@link CommandResponse} with the server's response.
     */
    public CommandResponse cancelEventSubscriptions() {
        checkConnected();
        InboundClientHandler handler = (InboundClientHandler) channel.pipeline().last();
        EslMessage response = handler.sendSyncSingleLineCommand(channel, "noevents");

        return new CommandResponse("noevents", response);
    }

    /**
     * Add an event filter to the current set of event filters on this connection. Any of the event headers
     * can be used as a filter.
     * </p>
     * Note that event filters follow 'filter-in' semantics. That is, when a filter is applied
     * only the filtered values will be received. Multiple filters can be added to the current
     * connection.
     * </p>
     * Example filters:
     * <pre>
     *    eventHeader        valueToFilter
     *    ----------------------------------
     *    Event-Name         CHANNEL_EXECUTE
     *    Channel-State      CS_NEW
     * </pre>
     *
     * @param eventHeader   to filter on
     * @param valueToFilter the value to match
     * @return a {@link CommandResponse} with the server's response.
     */
    public CommandResponse addEventFilter(String eventHeader, String valueToFilter) {
        checkConnected();
        InboundClientHandler handler = (InboundClientHandler) channel.pipeline().last();
        StringBuilder sb = new StringBuilder();
        if (eventHeader != null && !eventHeader.isEmpty()) {
            sb.append("filter ");
            sb.append(eventHeader);
        }
        if (valueToFilter != null && !valueToFilter.isEmpty()) {
            sb.append(' ');
            sb.append(valueToFilter);
        }
        EslMessage response = handler.sendSyncSingleLineCommand(channel, sb.toString());

        return new CommandResponse(sb.toString(), response);
    }

    /**
     * Delete an event filter from the current set of event filters on this connection.  See
     *
     * @param eventHeader   to remove
     * @param valueToFilter to remove
     * @return a {@link CommandResponse} with the server's response.
     */
    public CommandResponse deleteEventFilter(String eventHeader, String valueToFilter) {
        checkConnected();
        InboundClientHandler handler = (InboundClientHandler) channel.pipeline().last();
        StringBuilder sb = new StringBuilder();
        if (eventHeader != null && !eventHeader.isEmpty()) {
            sb.append("filter delete ");
            sb.append(eventHeader);
        }
        if (valueToFilter != null && !valueToFilter.isEmpty()) {
            sb.append(' ');
            sb.append(valueToFilter);
        }
        EslMessage response = handler.sendSyncSingleLineCommand(channel, sb.toString());

        return new CommandResponse(sb.toString(), response);
    }

    /**
     * Send a {@link SendMsg} command to FreeSWITCH.  This client requires that the {@link SendMsg}
     * has a call UUID parameter.
     *
     * @param sendMsg a {@link SendMsg} with call UUID
     * @return a {@link CommandResponse} with the server's response.
     */
    public CommandResponse sendMessage(SendMsg sendMsg) {
        checkConnected();
        InboundClientHandler handler = (InboundClientHandler) channel.pipeline().last();
        EslMessage response = handler.sendSyncMultiLineCommand(channel, sendMsg.getMsgLines());

        return new CommandResponse(sendMsg.toString(), response);
    }

    /**
     * Enable log output.
     *
     * @param level using the same values as in console.conf
     * @return a {@link CommandResponse} with the server's response.
     */
    public CommandResponse setLoggingLevel(String level) {
        checkConnected();
        InboundClientHandler handler = (InboundClientHandler) channel.pipeline().last();
        StringBuilder sb = new StringBuilder();
        if (level != null && !level.isEmpty()) {
            sb.append("log ");
            sb.append(level);
        }
        EslMessage response = handler.sendSyncSingleLineCommand(channel, sb.toString());

        return new CommandResponse(sb.toString(), response);
    }

    /**
     * Disable any logging previously enabled with setLogLevel().
     *
     * @return a {@link CommandResponse} with the server's response.
     */
    public CommandResponse cancelLogging() {
        checkConnected();
        InboundClientHandler handler = (InboundClientHandler) channel.pipeline().last();
        EslMessage response = handler.sendSyncSingleLineCommand(channel, "nolog");

        return new CommandResponse("nolog", response);
    }

    /**
     * Close the socket connection
     *
     * @return a {@link CommandResponse} with the server's response.
     */
    public CommandResponse close() {
        checkConnected();
        InboundClientHandler handler = (InboundClientHandler) channel.pipeline().last();
        EslMessage response = handler.sendSyncSingleLineCommand(channel, "exit");

        return new CommandResponse("exit", response);
    }

    /**
     * Internal observer of the ESL protocol
     */
    private final IEslProtocolListener protocolListener = new IEslProtocolListener() {
        @Override
        public void authResponseReceived(CommandResponse response) {
            authenticatorResponded.set(true);
            authenticated = response.isOk();
            log.debug("Auth response success={}, message=[{}]", authenticated, response.getReplyText());
            // 订阅消息
            setEventSubscriptions("plain", "all");
        }

        @Override
        public void eventReceived(final EslEvent event) {
            log.debug("Event received [{}]", event);
            /*
             *  Notify listeners in a different thread in order to:
             *    - not to block the IO threads with potentially long-running listeners
             *    - generally be defensive running other people's code
             *  Use a different worker thread pool for async job results than for event driven
             *  events to keep the latency as low as possible.
             */
            if (event.getEventName().equals(EslConstants.BACKGROUND_JOB)) {
                for (final IEslEventListener listener : eventListeners) {
                    backgroundJobListenerExecutor.execute(() -> {
                        try {
                            listener.backgroundJobResultReceived(event);
                        } catch (Throwable t) {
                            log.error("Error caught notifying listener of job result [" + event + ']', t);
                        }
                    });
                }
            } else {
                for (final IEslEventListener listener : eventListeners) {
                    eventListenerExecutor.execute(() -> {
                        try {
                            listener.eventReceived(event);
                        } catch (Throwable t) {
                            log.error("Error caught notifying listener of event [" + event + ']', t);
                        }
                    });
                }
            }
        }

        @Override
        public void disconnected() {
            log.info("Disconnected ..");
            connect();
        }
    };

    private void checkConnected() {
        if (!canSend()) {
            throw new IllegalStateException("Not connected to FreeSWITCH Event Socket");
        }
    }

}
