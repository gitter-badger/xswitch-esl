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

import java.util.concurrent.TimeUnit;

/**
 * @author : <a href="mailto:ant.zhou@aliyun.com">zhouhailin</a>
 */
public class EslClientConfig {

    private final String host;
    private final int port;
    private final String password;
    private int connectTimeoutMillis = 5 * 1000;
    /**
     * Threads
     */
    private int eventListenerThreads = 4;
    private int backgroudJobListenerThreads = 4;
    private int workerThreads = 4;

    private int sndBufSize = 65535;
    private int rcvBufSize = 65535;
    private long scheduledExecutorInitalDelay = 1;
    private long scheduledExecutorPeriod = 5;
    private TimeUnit scheduledExecutorTimeUnit = TimeUnit.SECONDS;


    // Esl Config
    //

    public EslClientConfig(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public EslClientConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        return this;
    }

    public int getEventListenerThreads() {
        return eventListenerThreads;
    }

    public EslClientConfig setEventListenerThreads(int eventListenerThreads) {
        this.eventListenerThreads = eventListenerThreads;
        return this;
    }

    public int getBackgroudJobListenerThreads() {
        return backgroudJobListenerThreads;
    }

    public EslClientConfig setBackgroudJobListenerThreads(int backgroudJobListenerThreads) {
        this.backgroudJobListenerThreads = backgroudJobListenerThreads;
        return this;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public EslClientConfig setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
        return this;
    }

    public int getSndBufSize() {
        return sndBufSize;
    }

    public EslClientConfig setSndBufSize(int sndBufSize) {
        this.sndBufSize = sndBufSize;
        return this;
    }

    public int getRcvBufSize() {
        return rcvBufSize;
    }

    public EslClientConfig setRcvBufSize(int rcvBufSize) {
        this.rcvBufSize = rcvBufSize;
        return this;
    }

    public long getScheduledExecutorInitalDelay() {
        return scheduledExecutorInitalDelay;
    }

    public EslClientConfig setScheduledExecutorInitalDelay(long scheduledExecutorInitalDelay) {
        this.scheduledExecutorInitalDelay = scheduledExecutorInitalDelay;
        return this;
    }

    public long getScheduledExecutorPeriod() {
        return scheduledExecutorPeriod;
    }

    public EslClientConfig setScheduledExecutorPeriod(long scheduledExecutorPeriod) {
        this.scheduledExecutorPeriod = scheduledExecutorPeriod;
        return this;
    }

    public TimeUnit getScheduledExecutorTimeUnit() {
        return scheduledExecutorTimeUnit;
    }

    public EslClientConfig setScheduledExecutorTimeUnit(TimeUnit scheduledExecutorTimeUnit) {
        this.scheduledExecutorTimeUnit = scheduledExecutorTimeUnit;
        return this;
    }

    @Override
    public String toString() {
        return "EslClientConfig{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", password='" + password + '\'' +
                ", connectTimeoutMillis=" + connectTimeoutMillis +
                ", eventListenerThreads=" + eventListenerThreads +
                ", backgroudJobListenerThreads=" + backgroudJobListenerThreads +
                ", workerThreads=" + workerThreads +
                ", sndBufSize=" + sndBufSize +
                ", rcvBufSize=" + rcvBufSize +
                ", scheduledExecutorInitalDelay=" + scheduledExecutorInitalDelay +
                ", scheduledExecutorPeriod=" + scheduledExecutorPeriod +
                ", scheduledExecutorTimeUnit=" + scheduledExecutorTimeUnit +
                '}';
    }
}
