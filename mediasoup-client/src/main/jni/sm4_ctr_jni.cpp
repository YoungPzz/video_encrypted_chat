#define MSC_CLASS "sm4_ctr_jni"

#include "Logger.hpp"
#include "api/crypto/sm4_ctr.h"
#include <jni.h>
#include <rtc_base/ref_count.h>
#include <rtc_base/ref_counted_object.h>

extern "C" {

/**
 * JNI: 创建 Native 层的 SM4CTREncryptor 实例
 */
jlong Java_org_webrtc_SM4CTREncryptor_nativeCreateSM4CTREncryptor(
    JNIEnv* env, jobject thiz, jbyteArray key, jbyteArray iv) {
    MSC_TRACE();

    try {
        // 获取密钥数组
        jbyte* key_ptr = env->GetByteArrayElements(key, nullptr);
        if (key_ptr == nullptr) {
            MSC_ERROR("Failed to get key array");
            return 0;
        }
        jsize key_len = env->GetArrayLength(key);
        if (key_len != 16) {
            MSC_ERROR("Key length must be 16 bytes, got %d", key_len);
            env->ReleaseByteArrayElements(key, key_ptr, JNI_ABORT);
            return 0;
        }

        // 获取IV数组
        jbyte* iv_ptr = env->GetByteArrayElements(iv, nullptr);
        if (iv_ptr == nullptr) {
            MSC_ERROR("Failed to get IV array");
            env->ReleaseByteArrayElements(key, key_ptr, JNI_ABORT);
            return 0;
        }
        jsize iv_len = env->GetArrayLength(iv);
        if (iv_len != 16) {
            MSC_ERROR("IV length must be 16 bytes, got %d", iv_len);
            env->ReleaseByteArrayElements(key, key_ptr, JNI_ABORT);
            env->ReleaseByteArrayElements(iv, iv_ptr, JNI_ABORT);
            return 0;
        }

        // 创建加密器实例
        auto encryptor = rtc::make_ref_counted<webrtc::SM4CTREncryptor>(
            reinterpret_cast<const uint8_t*>(key_ptr),
            reinterpret_cast<const uint8_t*>(iv_ptr)
        );
        encryptor->AddRef();

        // 释放数组资源
        env->ReleaseByteArrayElements(key, key_ptr, JNI_ABORT);
        env->ReleaseByteArrayElements(iv, iv_ptr, JNI_ABORT);

        MSC_DEBUG("SM4CTREncryptor native instance created: %p", encryptor.get());

        return reinterpret_cast<jlong>(encryptor.release());
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to create SM4CTREncryptor: %s", e.what());
        return 0;
    }
}

/**
 * JNI: 设置新的密钥
 */
void Java_org_webrtc_SM4CTREncryptor_setKey(JNIEnv* env, jobject thiz, jbyteArray key) {
    MSC_TRACE();

    // 获取native指针
    jclass clazz = env->GetObjectClass(thiz);
    jfieldID field = env->GetFieldID(clazz, "nativeSM4CTREncryptorPtr", "J");
    jlong native_ptr = env->GetLongField(thiz, field);

    if (native_ptr == 0) {
        MSC_WARN("SM4CTREncryptor native pointer is null");
        return;
    }

    try {
        // 获取密钥数组
        jbyte* key_ptr = env->GetByteArrayElements(key, nullptr);
        if (key_ptr == nullptr) {
            MSC_ERROR("Failed to get key array");
            return;
        }
        jsize key_len = env->GetArrayLength(key);
        if (key_len != 16) {
            MSC_ERROR("Key length must be 16 bytes, got %d", key_len);
            env->ReleaseByteArrayElements(key, key_ptr, JNI_ABORT);
            return;
        }

        // 设置密钥
        webrtc::SM4CTREncryptor* encryptor =
            reinterpret_cast<webrtc::SM4CTREncryptor*>(native_ptr);
        encryptor->SetKey(reinterpret_cast<const uint8_t*>(key_ptr));

        // 释放数组资源
        env->ReleaseByteArrayElements(key, key_ptr, JNI_ABORT);

        MSC_DEBUG("SM4CTREncryptor key updated");
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to set key: %s", e.what());
    }
}

/**
 * JNI: 设置新的IV
 */
void Java_org_webrtc_SM4CTREncryptor_setIV(JNIEnv* env, jobject thiz, jbyteArray iv) {
    MSC_TRACE();

    // 获取native指针
    jclass clazz = env->GetObjectClass(thiz);
    jfieldID field = env->GetFieldID(clazz, "nativeSM4CTREncryptorPtr", "J");
    jlong native_ptr = env->GetLongField(thiz, field);

    if (native_ptr == 0) {
        MSC_WARN("SM4CTREncryptor native pointer is null");
        return;
    }

    try {
        // 获取IV数组
        jbyte* iv_ptr = env->GetByteArrayElements(iv, nullptr);
        if (iv_ptr == nullptr) {
            MSC_ERROR("Failed to get IV array");
            return;
        }
        jsize iv_len = env->GetArrayLength(iv);
        if (iv_len != 16) {
            MSC_ERROR("IV length must be 16 bytes, got %d", iv_len);
            env->ReleaseByteArrayElements(iv, iv_ptr, JNI_ABORT);
            return;
        }

        // 设置IV
        webrtc::SM4CTREncryptor* encryptor =
            reinterpret_cast<webrtc::SM4CTREncryptor*>(native_ptr);
        encryptor->SetIV(reinterpret_cast<const uint8_t*>(iv_ptr));

        // 释放数组资源
        env->ReleaseByteArrayElements(iv, iv_ptr, JNI_ABORT);

        MSC_DEBUG("SM4CTREncryptor IV updated");
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to set IV: %s", e.what());
    }
}

/**
 * JNI: 销毁 Native 层的 SM4CTREncryptor 实例
 */
void Java_org_webrtc_SM4CTREncryptor_nativeDestroySM4CTREncryptor(
    JNIEnv* env, jobject thiz, jlong native_ptr) {
    MSC_TRACE();

    if (native_ptr == 0) {
        MSC_WARN("SM4CTREncryptor native pointer is null");
        return;
    }

    try {
        webrtc::FrameEncryptorInterface* encryptor =
            reinterpret_cast<webrtc::FrameEncryptorInterface*>(native_ptr);

        // 释放引用计数
        encryptor->Release();

        MSC_DEBUG("SM4CTREncryptor native instance released: %p", encryptor);
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to release SM4CTREncryptor: %s", e.what());
    }
}

/**
 * JNI: 创建 Native 层的 SM4CTRDecryptor 实例
 */
jlong Java_org_webrtc_SM4CTRDecryptor_nativeCreateSM4CTRDecryptor(
    JNIEnv* env, jobject thiz, jbyteArray key, jbyteArray iv) {
    MSC_TRACE();

    try {
        // 获取密钥数组
        jbyte* key_ptr = env->GetByteArrayElements(key, nullptr);
        if (key_ptr == nullptr) {
            MSC_ERROR("Failed to get key array");
            return 0;
        }
        jsize key_len = env->GetArrayLength(key);
        if (key_len != 16) {
            MSC_ERROR("Key length must be 16 bytes, got %d", key_len);
            env->ReleaseByteArrayElements(key, key_ptr, JNI_ABORT);
            return 0;
        }

        // 获取IV数组
        jbyte* iv_ptr = env->GetByteArrayElements(iv, nullptr);
        if (iv_ptr == nullptr) {
            MSC_ERROR("Failed to get IV array");
            env->ReleaseByteArrayElements(key, key_ptr, JNI_ABORT);
            return 0;
        }
        jsize iv_len = env->GetArrayLength(iv);
        if (iv_len != 16) {
            MSC_ERROR("IV length must be 16 bytes, got %d", iv_len);
            env->ReleaseByteArrayElements(key, key_ptr, JNI_ABORT);
            env->ReleaseByteArrayElements(iv, iv_ptr, JNI_ABORT);
            return 0;
        }

        // 创建解密器实例
        auto decryptor = rtc::make_ref_counted<webrtc::SM4CTRDecryptor>(
            reinterpret_cast<const uint8_t*>(key_ptr),
            reinterpret_cast<const uint8_t*>(iv_ptr)
        );
        decryptor->AddRef();

        // 释放数组资源
        env->ReleaseByteArrayElements(key, key_ptr, JNI_ABORT);
        env->ReleaseByteArrayElements(iv, iv_ptr, JNI_ABORT);

        MSC_DEBUG("SM4CTRDecryptor native instance created: %p", decryptor.get());

        return reinterpret_cast<jlong>(decryptor.release());
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to create SM4CTRDecryptor: %s", e.what());
        return 0;
    }
}

/**
 * JNI: 设置新的密钥
 */
void Java_org_webrtc_SM4CTRDecryptor_setKey(JNIEnv* env, jobject thiz, jbyteArray key) {
    MSC_TRACE();

    // 获取native指针
    jclass clazz = env->GetObjectClass(thiz);
    jfieldID field = env->GetFieldID(clazz, "nativeSM4CTRDecryptorPtr", "J");
    jlong native_ptr = env->GetLongField(thiz, field);

    if (native_ptr == 0) {
        MSC_WARN("SM4CTRDecryptor native pointer is null");
        return;
    }

    try {
        // 获取密钥数组
        jbyte* key_ptr = env->GetByteArrayElements(key, nullptr);
        if (key_ptr == nullptr) {
            MSC_ERROR("Failed to get key array");
            return;
        }
        jsize key_len = env->GetArrayLength(key);
        if (key_len != 16) {
            MSC_ERROR("Key length must be 16 bytes, got %d", key_len);
            env->ReleaseByteArrayElements(key, key_ptr, JNI_ABORT);
            return;
        }

        // 设置密钥
        webrtc::SM4CTRDecryptor* decryptor =
            reinterpret_cast<webrtc::SM4CTRDecryptor*>(native_ptr);
        decryptor->SetKey(reinterpret_cast<const uint8_t*>(key_ptr));

        // 释放数组资源
        env->ReleaseByteArrayElements(key, key_ptr, JNI_ABORT);

        MSC_DEBUG("SM4CTRDecryptor key updated");
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to set key: %s", e.what());
    }
}

/**
 * JNI: 设置新的IV
 */
void Java_org_webrtc_SM4CTRDecryptor_setIV(JNIEnv* env, jobject thiz, jbyteArray iv) {
    MSC_TRACE();

    // 获取native指针
    jclass clazz = env->GetObjectClass(thiz);
    jfieldID field = env->GetFieldID(clazz, "nativeSM4CTRDecryptorPtr", "J");
    jlong native_ptr = env->GetLongField(thiz, field);

    if (native_ptr == 0) {
        MSC_WARN("SM4CTRDecryptor native pointer is null");
        return;
    }

    try {
        // 获取IV数组
        jbyte* iv_ptr = env->GetByteArrayElements(iv, nullptr);
        if (iv_ptr == nullptr) {
            MSC_ERROR("Failed to get IV array");
            return;
        }
        jsize iv_len = env->GetArrayLength(iv);
        if (iv_len != 16) {
            MSC_ERROR("IV length must be 16 bytes, got %d", iv_len);
            env->ReleaseByteArrayElements(iv, iv_ptr, JNI_ABORT);
            return;
        }

        // 设置IV
        webrtc::SM4CTRDecryptor* decryptor =
            reinterpret_cast<webrtc::SM4CTRDecryptor*>(native_ptr);
        decryptor->SetIV(reinterpret_cast<const uint8_t*>(iv_ptr));

        // 释放数组资源
        env->ReleaseByteArrayElements(iv, iv_ptr, JNI_ABORT);

        MSC_DEBUG("SM4CTRDecryptor IV updated");
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to set IV: %s", e.what());
    }
}

/**
 * JNI: 设置解密结果状态
 */
void Java_org_webrtc_SM4CTRDecryptor_setStatus(JNIEnv* env, jobject thiz, jint status) {
    MSC_TRACE();

    // 获取native指针
    jclass clazz = env->GetObjectClass(thiz);
    jfieldID field = env->GetFieldID(clazz, "nativeSM4CTRDecryptorPtr", "J");
    jlong native_ptr = env->GetLongField(thiz, field);

    if (native_ptr == 0) {
        MSC_WARN("SM4CTRDecryptor native pointer is null");
        return;
    }

    try {
        webrtc::SM4CTRDecryptor* decryptor =
            reinterpret_cast<webrtc::SM4CTRDecryptor*>(native_ptr);
        decryptor->SetStatus(static_cast<webrtc::FrameDecryptorInterface::Status>(status));

        MSC_DEBUG("SM4CTRDecryptor status set to %d", status);
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to set status: %s", e.what());
    }
}

/**
 * JNI: 销毁 Native 层的 SM4CTRDecryptor 实例
 */
void Java_org_webrtc_SM4CTRDecryptor_nativeDestroySM4CTRDecryptor(
    JNIEnv* env, jobject thiz, jlong native_ptr) {
    MSC_TRACE();

    if (native_ptr == 0) {
        MSC_WARN("SM4CTRDecryptor native pointer is null");
        return;
    }

    try {
        webrtc::FrameDecryptorInterface* decryptor =
            reinterpret_cast<webrtc::FrameDecryptorInterface*>(native_ptr);

        // 释放引用计数
        decryptor->Release();

        MSC_DEBUG("SM4CTRDecryptor native instance released: %p", decryptor);
    }
    catch (const std::exception& e) {
        MSC_ERROR("Failed to release SM4CTRDecryptor: %s", e.what());
    }
}

} // extern "C"
