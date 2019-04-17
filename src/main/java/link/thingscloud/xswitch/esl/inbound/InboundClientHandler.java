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

import io.netty.channel.ChannelHandlerContext;
import link.thingscloud.xswitch.esl.internal.AbstractEslClientHandler;
import link.thingscloud.xswitch.esl.internal.IEslProtocolListener;
import link.thingscloud.xswitch.esl.transport.CommandResponse;
import link.thingscloud.xswitch.esl.transport.event.EslEvent;
import link.thingscloud.xswitch.esl.transport.message.EslHeaders;
import link.thingscloud.xswitch.esl.transport.message.EslMessage;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author : <a href="mailto:ant.zhou@aliyun.com">zhouhailin</a>
 */
public class InboundClientHandler extends AbstractEslClientHandler {
    private final String password;
    private final IEslProtocolListener listener;
    private final Executor executor;

    public InboundClientHandler(String password, IEslProtocolListener listener) {
        this.password = password;
        this.listener = listener;
        this.executor = new ScheduledThreadPoolExecutor(1,
                new BasicThreadFactory.Builder().namingPattern("InboundClientHandlerExecutor-%d").daemon(true).build());
    }

    @Override
    protected void handleEslEvent(ChannelHandlerContext ctx, EslEvent event) {
        log.debug("Received event: [{}]", event);
        this.executor.execute(() -> listener.eventReceived(event));
    }

    @Override
    protected void handleAuthRequest(ChannelHandlerContext ctx) {
        log.debug("Auth requested, sending [auth {}]", "*****");
        this.executor.execute(() -> {
            EslMessage response = sendSyncSingleLineCommand(ctx.channel(), "auth " + password);
            log.debug("Auth response [{}]", response);
            if (response.getContentType().equals(EslHeaders.Value.COMMAND_REPLY)) {
                CommandResponse commandResponse = new CommandResponse("auth " + password, response);
                listener.authResponseReceived(commandResponse);
            } else {
                log.error("Bad auth response message [{}]", response);
                throw new IllegalStateException("Incorrect auth response");
            }
        });
    }

    @Override
    protected void handleDisconnectionNotice() {
        log.debug("Received disconnection notice");
        listener.disconnected();
    }
}
