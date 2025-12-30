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

} // extern "C"
