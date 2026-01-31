#define MSC_CLASS "hc_crypto_jni"

#include "Logger.hpp"
#include "api/crypto/hc_crypto.h"
#include <jni.h>
#include <rtc_base/ref_count.h>
#include <rtc_base/ref_counted_object.h>

extern "C" {

/**
 * JNI: 创建 Native 层的 HCCrypto 实例
 */
jlong Java_org_webrtc_HCCrypto_nativeCreateHCCrypto(JNIEnv* env, jobject thiz) {
    MSC_TRACE();

    try {
        // 使用 rtc::make_ref_counted 创建引用计数对象
        auto encryptor = rtc::make_ref_counted<webrtc::HCCrypto>();
        encryptor->AddRef();

        MSC_DEBUG("HCCrypto native instance created: %p", encryptor.get());

        return reinterpret_cast<jlong>(encryptor.release());
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to create HCCrypto: %s", e.what());
        return 0;
    }
}

/**
 * JNI: 销毁 Native 层的 HCCrypto 实例
 */
void Java_org_webrtc_HCCrypto_nativeDestroyHCCrypto(JNIEnv* env, jobject thiz, jlong native_ptr) {
    MSC_TRACE();

    if (native_ptr == 0) {
        MSC_WARN("HCCrypto native pointer is null");
        return;
    }

    try {
        webrtc::FrameEncryptorInterface* encryptor =
            reinterpret_cast<webrtc::FrameEncryptorInterface*>(native_ptr);

        // 释放引用计数
        encryptor->Release();

        MSC_DEBUG("HCCrypto native instance released: %p", encryptor);
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to release HCCrypto: %s", e.what());
    }
}

/**
 * JNI: 启用或禁用 SM4 加密
 */
void Java_org_webrtc_HCCrypto_nativeEnableSM4Encryption(JNIEnv* env, jobject thiz, jlong native_ptr, jboolean enable) {
    MSC_TRACE();

    if (native_ptr == 0) {
        MSC_WARN("HCCrypto native pointer is null");
        return;
    }

    try {
        webrtc::HCCrypto* encryptor =
            reinterpret_cast<webrtc::HCCrypto*>(native_ptr);

        encryptor->EnableSM4Encryption(enable);

        MSC_DEBUG("HCCrypto SM4 encryption %s", enable ? "enabled" : "disabled");
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to enable/disable SM4 encryption: %s", e.what());
    }
}

/**
 * JNI: 设置 SM4 密钥
 */
void Java_org_webrtc_HCCrypto_nativeSetSM4Key(JNIEnv* env, jobject thiz, jlong native_ptr, jbyteArray key) {
    MSC_TRACE();

    if (native_ptr == 0) {
        MSC_WARN("HCCrypto native pointer is null");
        return;
    }

    if (key == nullptr || env->GetArrayLength(key) != 16) {
        MSC_ERROR("Invalid SM4 key: must be 16 bytes");
        return;
    }

    try {
        webrtc::HCCrypto* encryptor =
            reinterpret_cast<webrtc::HCCrypto*>(native_ptr);

        jbyte* key_bytes = env->GetByteArrayElements(key, nullptr);
        uint8_t sm4_key[16];
        memcpy(sm4_key, key_bytes, 16);
        env->ReleaseByteArrayElements(key, key_bytes, JNI_ABORT);

        encryptor->SetSM4Key(sm4_key);

        MSC_DEBUG("HCCrypto SM4 key updated");
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to set SM4 key: %s", e.what());
    }
}

/**
 * JNI: 设置 SM4 CTR
 */
void Java_org_webrtc_HCCrypto_nativeSetSM4CTR(JNIEnv* env, jobject thiz, jlong native_ptr, jbyteArray ctr) {
    MSC_TRACE();

    if (native_ptr == 0) {
        MSC_WARN("HCCrypto native pointer is null");
        return;
    }

    if (ctr == nullptr || env->GetArrayLength(ctr) != 16) {
        MSC_ERROR("Invalid SM4 CTR: must be 16 bytes");
        return;
    }

    try {
        webrtc::HCCrypto* encryptor =
            reinterpret_cast<webrtc::HCCrypto*>(native_ptr);

        jbyte* ctr_bytes = env->GetByteArrayElements(ctr, nullptr);
        uint8_t sm4_ctr[16];
        memcpy(sm4_ctr, ctr_bytes, 16);
        env->ReleaseByteArrayElements(ctr, ctr_bytes, JNI_ABORT);

        encryptor->SetSM4CTR(sm4_ctr);

        MSC_DEBUG("HCCrypto SM4 CTR updated");
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to set SM4 CTR: %s", e.what());
    }
}

} // extern "C"
