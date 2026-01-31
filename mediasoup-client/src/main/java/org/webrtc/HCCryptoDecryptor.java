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
       */
    private native void nativeDestroyHCCryptoDecryptor(long ptr);

    /**
     * 启用或禁用 SM4 解密
     * @param enable true 启用 SM4 解密，false 使用 XOR 解密
     */
    public native void nativeEnableSM4Decryption(long ptr, boolean enable);

    /**
     * 设置 SM4 密钥（16字节）
     * @param key 16字节的 SM4 密钥
     */
    public native void nativeSetSM4Key(long ptr, byte[] key);

    /**
     * 设置 SM4 CTR 计数器（16字节）
     * @param ctr 16字节的 CTR 计数器
     */
    public native void nativeSetSM4CTR(long ptr, byte[] ctr);

    /**
     * 释放资源
     */
    public void dispose() {
        if (nativeHCCryptoDecryptorPtr != 0) {
            nativeDestroyHCCryptoDecryptor(nativeHCCryptoDecryptorPtr);
            nativeHCCryptoDecryptorPtr = 0;
        }
    }

    /**
     * 启用或禁用 SM4 解密
     * @param enable true 启用 SM4 解密，false 使用 XOR 解密
     */
    public void enableSM4Decryption(boolean enable) {
        if (nativeHCCryptoDecryptorPtr != 0) {
            nativeEnableSM4Decryption(nativeHCCryptoDecryptorPtr, enable);
        }
    }

    /**
     * 设置 SM4 密钥（16字节）
     * @param key 16字节的 SM4 密钥
     */
    public void setSM4Key(byte[] key) {
        if (nativeHCCryptoDecryptorPtr != 0) {
            nativeSetSM4Key(nativeHCCryptoDecryptorPtr, key);
        }
    }

    /**
     * 设置 SM4 CTR 计数器（16字节）
     * @param ctr 16字节的 CTR 计数器
     */
    public void setSM4CTR(byte[] ctr) {
        if (nativeHCCryptoDecryptorPtr != 0) {
            nativeSetSM4CTR(nativeHCCryptoDecryptorPtr, ctr);
        }
    }
}
