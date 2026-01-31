#define MSC_CLASS "hc_crypto_decryptor_jni"

#include "Logger.hpp"
#include "api/crypto/hc_crypto_decryptor.h"
#include <jni.h>
#include <rtc_base/ref_count.h>
#include <rtc_base/ref_counted_object.h>

extern "C" {

/**
 * JNI: 创建 Native 层的 HCCryptoDecryptor 实例
 */
jlong Java_org_webrtc_HCCryptoDecryptor_nativeCreateHCCryptoDecryptor(JNIEnv* env, jobject thiz) {
    MSC_TRACE();

    try {
        // 直接创建 HCCryptoDecryptor 实例并手动管理引用计数
        auto decryptor = rtc::make_ref_counted<webrtc::HCCryptoDecryptor>();
        decryptor->AddRef();

//        MSC_DEBUG("HCCryptoDecryptor native instance created: %p", decryptor.get();

        return reinterpret_cast<jlong>(decryptor.release());
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to create HCCryptoDecryptor: %s", e.what());
        return 0;
    }
}

/**
 * JNI: 销毁 Native 层的 HCCryptoDecryptor 实例
 */
void Java_org_webrtc_HCCryptoDecryptor_nativeDestroyHCCryptoDecryptor(JNIEnv* env, jobject thiz, jlong native_ptr) {
    MSC_TRACE();

    if (native_ptr == 0) {
        MSC_WARN("HCCryptoDecryptor native pointer is null");
        return;
    }

    try {
        webrtc::FrameDecryptorInterface* decryptor =
            reinterpret_cast<webrtc::FrameDecryptorInterface*>(native_ptr);

        // 释放引用计数
        decryptor->Release();

        MSC_DEBUG("HCCryptoDecryptor native instance released: %p", decryptor);
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to release HCCryptoDecryptor: %s", e.what());
    }
}

/**
 * JNI: 启用或禁用 SM4 解密
 */
void Java_org_webrtc_HCCryptoDecryptor_nativeEnableSM4Decryption(JNIEnv* env, jobject thiz, jlong native_ptr, jboolean enable) {
    MSC_TRACE();

    if (native_ptr == 0) {
        MSC_WARN("HCCryptoDecryptor native pointer is null");
        return;
    }

    try {
        webrtc::HCCryptoDecryptor* decryptor =
            reinterpret_cast<webrtc::HCCryptoDecryptor*>(native_ptr);

        decryptor->EnableSM4Decryption(enable);

        MSC_DEBUG("HCCryptoDecryptor SM4 decryption %s", enable ? "enabled" : "disabled");
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to enable/disable SM4 decryption: %s", e.what());
    }
}

/**
 * JNI: 设置 SM4 密钥
 */
void Java_org_webrtc_HCCryptoDecryptor_nativeSetSM4Key(JNIEnv* env, jobject thiz, jlong native_ptr, jbyteArray key) {
    MSC_TRACE();

    if (native_ptr == 0) {
        MSC_WARN("HCCryptoDecryptor native pointer is null");
        return;
    }

    if (key == nullptr || env->GetArrayLength(key) != 16) {
        MSC_ERROR("Invalid SM4 key: must be 16 bytes");
        return;
    }

    try {
        webrtc::HCCryptoDecryptor* decryptor =
            reinterpret_cast<webrtc::HCCryptoDecryptor*>(native_ptr);

        jbyte* key_bytes = env->GetByteArrayElements(key, nullptr);
        uint8_t sm4_key[16];
        memcpy(sm4_key, key_bytes, 16);
        env->ReleaseByteArrayElements(key, key_bytes, JNI_ABORT);

        decryptor->SetSM4Key(sm4_key);

        MSC_DEBUG("HCCryptoDecryptor SM4 key updated");
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to set SM4 key: %s", e.what());
    }
}

/**
 * JNI: 设置 SM4 CTR
 */
void Java_org_webrtc_HCCryptoDecryptor_nativeSetSM4CTR(JNIEnv* env, jobject thiz, jlong native_ptr, jbyteArray ctr) {
    MSC_TRACE();

    if (native_ptr == 0) {
        MSC_WARN("HCCryptoDecryptor native pointer is null");
        return;
    }

    if (ctr == nullptr || env->GetArrayLength(ctr) != 16) {
        MSC_ERROR("Invalid SM4 CTR: must be 16 bytes");
        return;
    }

    try {
        webrtc::HCCryptoDecryptor* decryptor =
            reinterpret_cast<webrtc::HCCryptoDecryptor*>(native_ptr);

        jbyte* ctr_bytes = env->GetByteArrayElements(ctr, nullptr);
        uint8_t sm4_ctr[16];
        memcpy(sm4_ctr, ctr_bytes, 16);
        env->ReleaseByteArrayElements(ctr, ctr_bytes, JNI_ABORT);

        decryptor->SetSM4CTR(sm4_ctr);

        MSC_DEBUG("HCCryptoDecryptor SM4 CTR updated");
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to set SM4 CTR: %s", e.what());
    }
}

} // extern "C"
