package org.webrtc;

/**
 * 自定义解密器 Java 包装类
 */
public class HCCryptoDecryptor {
    // 保存 Native 层实现的 HCCryptoDecryptor 指针
    private long nativeHCCryptoDecryptorPtr;

    /**
     * 构造函数，初始化 Native 层的解密逻辑
     */
    public HCCryptoDecryptor() {
        // 调用 Native 方法，创建 Native 层的 HCCryptoDecryptor 实现
        this.nativeHCCryptoDecryptorPtr = nativeCreateHCCryptoDecryptor();
    }

    /**
     * 获取 Native 层解密器的指针（供 JNI 调用）
     */
    public long getNativeHCCryptoDecryptor() {
        return this.nativeHCCryptoDecryptorPtr;
    }

    /**
     * 声明 Native 方法：创建 Native 层的 HCCryptoDecryptor
     */
    private native long nativeCreateHCCryptoDecryptor();

    /**
     * 销毁 Native 层资源
     */
    private native void nativeDestroyHCCryptoDecryptor(long ptr);

    /**
     * 释放资源
     */
    public void dispose() {
        if (nativeHCCryptoDecryptorPtr != 0) {
            nativeDestroyHCCryptoDecryptor(nativeHCCryptoDecryptorPtr);
            nativeHCCryptoDecryptorPtr = 0;
        }
    }
}
