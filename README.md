# xswitch-esl

[![Gitter](https://badges.gitter.im/zhouhailin/xswitch-esl.svg)](https://gitter.im/zhouhailin/xswitch-esl?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)  
[![Jdk Version](https://img.shields.io/badge/JDK-1.8-green.svg)](https://img.shields.io/badge/JDK-1.8-green.svg)
[![Build Status](https://travis-ci.org/zhouhailin/xswitch-esl.svg?branch=master)](https://travis-ci.org/zhouhailin/xswitch-esl)
[![Coverage Status](https://img.shields.io/codecov/c/github/zhouhailin/xswitch-esl/master.svg)](https://codecov.io/github/zhouhailin/xswitch-esl?branch=master&view=all#sort=coverage&dir=asc)  
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/link.thingscloud/xswitch-esl/badge.svg)](https://maven-badges.herokuapp.com/maven-central/link.thingscloud/xswitch-esl/)
[![GitHub release](https://img.shields.io/github/release/zhouhailin/xswitch-esl.svg)](https://github.com/zhouhailin/xswitch-esl/releases)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

## 版本迭代

- [x] 呼入客户端实现 - 0.1.x
- [ ] 外呼服务端实现 - 0.2.x

## 快速使用

- java version

```
java version "1.8.0_181"
Java(TM) SE Runtime Environment (build 1.8.0_181-b13)
Java HotSpot(TM) 64-Bit Server VM (build 25.181-b13, mixed mode)
```

- ### maven - 暂未发布

```
<dependency>
  <groupId>link.thingscloud</groupId>
  <artifactId>xswitch-esl</artifactId>
  <version>${xswitch-esl.version}</version>
</dependency>
```

- example

```
    IEslClient client = null;
    
    @Before
    public void setUp() throws Exception {
        client = new EslInboundClient(new EslClientConfig("127.0.0.1", 8201, "ClueCon"));
    }

    @After
    public void tearDown() throws Exception {
        client.shutdown();
    }

    @Test
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
```

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) Copyright (C) Apache Software Foundation
