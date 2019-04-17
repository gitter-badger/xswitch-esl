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

package link.thingscloud.xswitch.esl.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import link.thingscloud.xswitch.esl.transport.event.EslEvent;
import link.thingscloud.xswitch.esl.transport.message.EslHeaders;
import link.thingscloud.xswitch.esl.transport.message.EslMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author : <a href="mailto:ant.zhou@aliyun.com">zhouhailin</a>
 */
public abstract class AbstractEslClientHandler extends SimpleChannelInboundHandler<EslMessage> {

    public static final String MESSAGE_TERMINATOR = "\n\n";
    public static final String LINE_TERMINATOR = "\n";

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Lock syncLock = new ReentrantLock();
    private final Queue<SyncCallback> syncCallbacks = new ConcurrentLinkedQueue<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, EslMessage msg) {
        String contentType = msg.getContentType();
        if (contentType.equals(EslHeaders.Value.TEXT_EVENT_PLAIN) ||
                contentType.equals(EslHeaders.Value.TEXT_EVENT_XML)) {
            //  transform into an event
            EslEvent eslEvent = new EslEvent(msg);
            handleEslEvent(ctx, eslEvent);
        } else {
            handleEslMessage(ctx, msg);
        }
    }

    /**
     * Synthesise a synchronous command/response by creating a callback object which is placed in
     * queue and blocks waiting for another IO thread to process an incoming {@link EslMessage} and
     * attach it to the callback.
     *
     * @param channel
     * @param command single string to send
     * @return the {@link EslMessage} attached to this command's callback
     */
    public EslMessage sendSyncSingleLineCommand(Channel channel, final String command) {
        SyncCallback callback = new SyncCallback();
        syncLock.lock();
        try {
            syncCallbacks.add(callback);
            channel.writeAndFlush(command + MESSAGE_TERMINATOR);
        } finally {
            syncLock.unlock();
        }

        //  Block until the response is available
        return callback.get();
    }

    /**
     * Synthesise a synchronous command/response by creating a callback object which is placed in
     * queue and blocks waiting for another IO thread to process an incoming {@link EslMessage} and
     * attach it to the callback.
     *
     * @param channel
     * @param commandLines List of command lines to send
     * @return the {@link EslMessage} attached to this command's callback
     */
    public EslMessage sendSyncMultiLineCommand(Channel channel, final List<String> commandLines) {
        SyncCallback callback = new SyncCallback();
        //  Build command with double line terminator at the end
        StringBuilder sb = new StringBuilder();
        for (String line : commandLines) {
            sb.append(line);
            sb.append(LINE_TERMINATOR);
        }
        sb.append(LINE_TERMINATOR);

        syncLock.lock();
        try {
            syncCallbacks.add(callback);
            channel.writeAndFlush(sb.toString());
        } finally {
            syncLock.unlock();
        }

        //  Block until the response is available
        return callback.get();
    }

    /**
     * Returns the Job UUID of that the response event will have.
     *
     * @param channel
     * @param command
     * @return Job-UUID as a string
     */
    public String sendAsyncCommand(Channel channel, final String command) {
        /*
         * Send synchronously to get the Job-UUID to return, the results of the actual
         * job request will be returned by the server as an async event.
         */
        EslMessage response = sendSyncSingleLineCommand(channel, command);
        if (response.hasHeader(EslHeaders.Name.JOB_UUID)) {
            return response.getHeaderValue(EslHeaders.Name.JOB_UUID);
        } else {
            throw new IllegalStateException("Missing Job-UUID header in bgapi response");
        }
    }

    protected void handleEslMessage(ChannelHandlerContext ctx, EslMessage message) {
        log.info("Received message: [{}]", message);
        String contentType = message.getContentType();

        if (contentType.equals(EslHeaders.Value.API_RESPONSE)) {
            log.debug("Api response received [{}]", message);
            syncCallbacks.poll().handle(message);
        } else if (contentType.equals(EslHeaders.Value.COMMAND_REPLY)) {
            log.debug("Command reply received [{}]", message);
            syncCallbacks.poll().handle(message);
        } else if (contentType.equals(EslHeaders.Value.AUTH_REQUEST)) {
            log.debug("Auth request received [{}]", message);
            handleAuthRequest(ctx);
        } else if (contentType.equals(EslHeaders.Value.TEXT_DISCONNECT_NOTICE)) {
            log.debug("Disconnect notice received [{}]", message);
            handleDisconnectionNotice();
        } else {
            log.warn("Unexpected message content type [{}]", contentType);
        }
    }

    /**
     * 处理ESL事件
     *
     * @param ctx
     * @param event
     */
    protected abstract void handleEslEvent(ChannelHandlerContext ctx, EslEvent event);

    /**
     * 处理认证请求
     *
     * @param ctx
     */
    protected abstract void handleAuthRequest(ChannelHandlerContext ctx);

    /**
     * 服务端断开处理逻辑
     */
    protected abstract void handleDisconnectionNotice();

    private static class SyncCallback {
        private static final Logger log = LoggerFactory.getLogger(SyncCallback.class);
        private final CountDownLatch latch = new CountDownLatch(1);
        private EslMessage response;

        /**
         * Block waiting for the countdown latch to be released, then return the
         * associated response object.
         *
         * @return
         */
        EslMessage get() {
            try {
                log.trace("awaiting latch ... ");
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            log.trace("returning response [{}]", response);
            return response;
        }

        /**
         * Attach this response to the callback and release the countdown latch.
         *
         * @param response
         */
        void handle(EslMessage response) {
            this.response = response;
            log.trace("releasing latch for response [{}]", response);
            latch.countDown();
        }
    }
}
