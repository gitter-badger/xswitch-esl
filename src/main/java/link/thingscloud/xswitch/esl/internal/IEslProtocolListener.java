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

import link.thingscloud.xswitch.esl.transport.CommandResponse;
import link.thingscloud.xswitch.esl.transport.event.EslEvent;

/**
 * End users of the {@link EslClient} should not need to use this class.
 * <p>
 * Allow client implementations to observe events arriving from the server.
 *
 * @author : <a href="mailto:ant.zhou@aliyun.com">zhouhailin</a>
 */
public interface IEslProtocolListener {
    void authResponseReceived(CommandResponse response);

    void eventReceived(EslEvent event);

    void disconnected();
}
