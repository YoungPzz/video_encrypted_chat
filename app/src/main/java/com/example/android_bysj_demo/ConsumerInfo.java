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
}