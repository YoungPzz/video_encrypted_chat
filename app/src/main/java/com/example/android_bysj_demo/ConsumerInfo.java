package com.example.android_bysj_demo;

import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.RecvTransport;

// 消费者信息类
public class ConsumerInfo {
    RecvTransport consumerTransport;
    String serverConsumerTransportId;
    String producerId;
    Consumer consumer;
    String kind;
    String participantId;  // 用户ID，用于区分不同用户
    String userName;       // 用户名称
}