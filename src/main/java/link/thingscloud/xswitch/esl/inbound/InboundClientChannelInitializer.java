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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import link.thingscloud.xswitch.esl.internal.IEslProtocolListener;
import link.thingscloud.xswitch.esl.transport.message.EslFrameDecoder;

/**
 * @author : <a href="mailto:ant.zhou@aliyun.com">zhouhailin</a>
 */
public class InboundClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final String password;
    private final IEslProtocolListener listener;

    public InboundClientChannelInitializer(String password, IEslProtocolListener listener) {
        this.password = password;
        this.listener = listener;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("encoder", new StringEncoder());
        pipeline.addLast("decoder", new EslFrameDecoder(8192));

        // now the inbound client logic
        pipeline.addLast("clientHandler", new InboundClientHandler(password, listener));
    }

}
