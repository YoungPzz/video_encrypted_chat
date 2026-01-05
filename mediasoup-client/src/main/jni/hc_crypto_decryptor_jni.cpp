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

} // extern "C"
