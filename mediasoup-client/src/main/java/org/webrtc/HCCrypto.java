package org.webrtc;

/**
 * 自定义加密器 Java 包装类
 */
public class HCCrypto {
    // 保存 Native 层实现的 HCCrypto 指针
    private long nativeHCCryptoPtr;

    /**
     * 构造函数，初始化 Native 层的加密逻辑
     */
    public HCCrypto() {
        // 调用 Native 方法，创建 Native 层的 HCCrypto 实现
        this.nativeHCCryptoPtr = nativeCreateHCCrypto();
    }

    /**
     * 获取 Native 层加密器的指针（供 JNI 调用）
     */
    public long getNativeHCCrypto() {
        return this.nativeHCCryptoPtr;
    }

    /**
     * 声明 Native 方法：创建 Native 层的 HCCrypto
     */
    private native long nativeCreateHCCrypto();

    /**
     * 销毁 Native 层资源
     */
    private native void nativeDestroyHCCrypto(long ptr);

    /**
     * 释放资源
     */
    public void dispose() {
        if (nativeHCCryptoPtr != 0) {
            nativeDestroyHCCrypto(nativeHCCryptoPtr);
            nativeHCCryptoPtr = 0;
        }
    }
}
