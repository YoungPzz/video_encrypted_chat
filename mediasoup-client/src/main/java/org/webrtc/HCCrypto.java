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
     * 启用或禁用 SM4 加密
     * @param enable true 启用 SM4 加密，false 使用 XOR 加密
     */
    public native void nativeEnableSM4Encryption(long ptr, boolean enable);

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
        if (nativeHCCryptoPtr != 0) {
            nativeDestroyHCCrypto(nativeHCCryptoPtr);
            nativeHCCryptoPtr = 0;
        }
    }

    /**
     * 启用或禁用 SM4 加密
     * @param enable true 启用 SM4 加密，false 使用 XOR 加密
     */
    public void enableSM4Encryption(boolean enable) {
        if (nativeHCCryptoPtr != 0) {
            nativeEnableSM4Encryption(nativeHCCryptoPtr, enable);
        }
    }

    /**
     * 设置 SM4 密钥（16字节）
     * @param key 16字节的 SM4 密钥
     */
    public void setSM4Key(byte[] key) {
        if (nativeHCCryptoPtr != 0) {
            nativeSetSM4Key(nativeHCCryptoPtr, key);
        }
    }

    /**
     * 设置 SM4 CTR 计数器（16字节）
     * @param ctr 16字节的 CTR 计数器
     */
    public void setSM4CTR(byte[] ctr) {
        if (nativeHCCryptoPtr != 0) {
            nativeSetSM4CTR(nativeHCCryptoPtr, ctr);
        }
    }
}
