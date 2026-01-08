package org.webrtc;

/**
 * SM4-CTR模式解密器 Java 包装类
 */
public class SM4CTRDecryptor {
    // 保存 Native 层实现的 SM4CTRDecryptor 指针
    private long nativeSM4CTRDecryptorPtr;

    /**
     * 构造函数，初始化 Native 层的 SM4-CTR 解密逻辑
     * 
     * @param key SM4密钥，16字节
     * @param iv 初始向量，16字节
     */
    public SM4CTRDecryptor(byte[] key, byte[] iv) {
        if (key == null || key.length != 16) {
            throw new IllegalArgumentException("Key must be 16 bytes");
        }
        if (iv == null || iv.length != 16) {
            throw new IllegalArgumentException("IV must be 16 bytes");
        }
        
        // 调用 Native 方法，创建 Native 层的 SM4CTRDecryptor 实现
        this.nativeSM4CTRDecryptorPtr = nativeCreateSM4CTRDecryptor(key, iv);
    }

    /**
     * 获取 Native 层解密器的指针（供 JNI 调用）
     */
    public long getNativeSM4CTRDecryptor() {
        return this.nativeSM4CTRDecryptorPtr;
    }

    /**
     * 声明 Native 方法：创建 Native 层的 SM4CTRDecryptor
     */
    private native long nativeCreateSM4CTRDecryptor(byte[] key, byte[] iv);

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
     * 设置解密结果状态
     * 
     * @param status 状态码 (0=成功, 其他值表示失败)
     */
    public native void setStatus(int status);

    /**
     * 销毁 Native 层资源
     */
    private native void nativeDestroySM4CTRDecryptor(long ptr);

    /**
     * 释放资源
     */
    public void dispose() {
        if (nativeSM4CTRDecryptorPtr != 0) {
            nativeDestroySM4CTRDecryptor(nativeSM4CTRDecryptorPtr);
            nativeSM4CTRDecryptorPtr = 0;
        }
    }
}
