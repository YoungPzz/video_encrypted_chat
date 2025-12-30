package com.example.android_bysj_demo;

import org.webrtc.FrameEncryptor;

public class MyFrameEncryptor implements FrameEncryptor {
    // 保存Native层实现的FrameEncryptorInterface指针
    private long nativeEncryptorPtr;

    // 构造时初始化Native层的加密逻辑
    public MyFrameEncryptor() {
        // 调用Native方法，创建Native层的FrameEncryptor实现，并返回其指针
        this.nativeEncryptorPtr = nativeCreateMyFrameEncryptor();
    }

    @Override
    public long getNativeFrameEncryptor() {
        // 返回Native层加密器的指针
        return this.nativeEncryptorPtr;
    }

    // 声明Native方法：创建Native层的加密器
    private native long nativeCreateMyFrameEncryptor();

    // （可选）销毁Native资源的方法
    public native void nativeDestroyMyFrameEncryptor();
}
