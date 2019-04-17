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

package link.thingscloud.xswitch.esl;

import link.thingscloud.xswitch.esl.inbound.EslInboundClient;
import link.thingscloud.xswitch.esl.internal.EslClientConfig;
import link.thingscloud.xswitch.esl.transport.event.EslEvent;

/**
 * @author : <a href="mailto:ant.zhou@aliyun.com">zhouhailin</a>
 */
public class IEslClientTest {

    IEslClient client = null;


    @org.junit.Before
    public void setUp() throws Exception {
        client = new EslInboundClient(new EslClientConfig("127.0.0.1", 8201, "ClueCon"));
    }

    @org.junit.After
    public void tearDown() throws Exception {
        client.shutdown();
    }

    public void test() {
        client.addEventListener(new IEslEventListener() {
            @Override
            public void eventReceived(EslEvent event) {

            }

            @Override
            public void backgroundJobResultReceived(EslEvent event) {

            }
        }).start();

    }
}