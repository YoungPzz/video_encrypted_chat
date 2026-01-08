package org.webrtc;

/**
 * SM4-CTR模式加密器 Java 包装类
 */
public class SM4CTREncryptor {
    // 保存 Native 层实现的 SM4CTREncryptor 指针
    private long nativeSM4CTREncryptorPtr;

    /**
     * 构造函数，初始化 Native 层的 SM4-CTR 加密逻辑
     * 
     * @param key SM4密钥，16字节
     * @param iv 初始向量，16字节
     */
    public SM4CTREncryptor(byte[] key, byte[] iv) {
        if (key == null || key.length != 16) {
            throw new IllegalArgumentException("Key must be 16 bytes");
        }
        if (iv == null || iv.length != 16) {
            throw new IllegalArgumentException("IV must be 16 bytes");
        }
        
        // 调用 Native 方法，创建 Native 层的 SM4CTREncryptor 实现
        this.nativeSM4CTREncryptorPtr = nativeCreateSM4CTREncryptor(key, iv);
    }

    /**
     * 获取 Native 层加密器的指针（供 JNI 调用）
     */
    public long getNativeSM4CTREncryptor() {
        return this.nativeSM4CTREncryptorPtr;
    }

    /**
     * 声明 Native 方法：创建 Native 层的 SM4CTREncryptor
     */
    private native long nativeCreateSM4CTREncryptor(byte[] key, byte[] iv);

    /**
     * 设置新的密钥
     * 
     * @param key 新的SM4密钥，16字节
     */
    public native void setKey(byte[] key);

    /**
     * 设置新的IV
     * 
     * @param iv 新的初始向量，16字节
     */
    public native void setIV(byte[] iv);

    /**
     * 销毁 Native 层资源
     */
    private native void nativeDestroySM4CTREncryptor(long ptr);

    /**
     * 释放资源
     */
    public void dispose() {
        if (nativeSM4CTREncryptorPtr != 0) {
            nativeDestroySM4CTREncryptor(nativeSM4CTREncryptorPtr);
            nativeSM4CTREncryptorPtr = 0;
        }
    }
}
