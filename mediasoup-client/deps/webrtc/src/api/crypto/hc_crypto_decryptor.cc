/*
 *  Copyright 2018 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
#include "api/crypto/hc_crypto_decryptor.h"

#include <vector>
#include <cstring>

#include "rtc_base/checks.h"

namespace webrtc {
    // 定义静态常量：预定义的固定 SM4 密钥和 CTR
    const uint8_t HCCryptoDecryptor::DEFAULT_SM4_KEY[16] = {
        0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF,
        0xFE, 0xDC, 0xBA, 0x98, 0x76, 0x54, 0x32, 0x10
    };
    
    const uint8_t HCCryptoDecryptor::DEFAULT_SM4_CTR[16] = {
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
    };

    HCCryptoDecryptor::HCCryptoDecryptor(uint8_t fake_key,
                                           uint8_t expected_postfix_byte)
            : fake_key_(fake_key),
              expected_postfix_byte_(expected_postfix_byte),
              use_sm4_decryption_(true) {
        
        // 初始化 SM4 密钥上下文（使用预定义的固定密钥）
        memcpy(sm4_key_bytes_, DEFAULT_SM4_KEY, 16);
        sm4_set_encrypt_key(&sm4_key_, sm4_key_bytes_);
        
        // 初始化 CTR（使用预定义的固定 CTR）
        memcpy(sm4_ctr_, DEFAULT_SM4_CTR, 16);
    }

    HCCryptoDecryptor::Result HCCryptoDecryptor::Decrypt(
            cricket::MediaType media_type,
            const std::vector<uint32_t>& csrcs,
            rtc::ArrayView<const uint8_t> additional_data,
            rtc::ArrayView<const uint8_t> encrypted_frame,
            rtc::ArrayView<uint8_t> frame) {
        if (fail_decryption_) {
            return Result(Status::kFailedToDecrypt, 0);
        }

        RTC_CHECK_EQ(frame.size() + 1, encrypted_frame.size());

        if (use_sm4_decryption_) {
            // 使用 SM4-CTR 解密
            uint8_t local_ctr[16];
            memcpy(local_ctr, sm4_ctr_, 16);  // 使用本地副本避免修改原始 CTR
            
            // 调用 GmSSL 的 SM4-CTR 解密函数（CTR 模式加密和解密使用相同的操作）
            sm4_ctr_encrypt(&sm4_key_, local_ctr,
                           encrypted_frame.data(), frame.size(),
                           frame.data());
            
            RTC_LOG(LS_INFO) << "[HCCrypto] SM4-CTR decrypted " << frame.size() << " bytes";
        } else {
            // 使用原有的 XOR 解密（保持向后兼容）
            for (size_t i = 0; i < frame.size(); i++) {
                frame[i] = encrypted_frame[i] ^ fake_key_;
            }
            RTC_LOG(LS_INFO) << "[HCCrypto] XOR decrypted " << frame.size() << " bytes";
        }

        // 验证后缀字节
        if (encrypted_frame[frame.size()] != expected_postfix_byte_) {
            RTC_LOG(LS_ERROR) << "[HCCrypto] Invalid postfix byte, expected " 
                           << expected_postfix_byte_ << ", got " 
                           << encrypted_frame[frame.size()];
            return Result(Status::kFailedToDecrypt, 0);
        }

        RTC_LOG(LS_INFO) << "[HCCrypto] decrypt success, status=OK";
        return Result(Status::kOk, frame.size());
    }

    size_t HCCryptoDecryptor::GetMaxPlaintextByteSize(
            cricket::MediaType media_type,
            size_t encrypted_frame_size) {
        return encrypted_frame_size - 1;
    }

    void HCCryptoDecryptor::SetFakeKey(uint8_t fake_key) {
        fake_key_ = fake_key;
        // 同时更新 SM4 密钥（使用 fake_key 填充 16 字节）
        memset(sm4_key_bytes_, fake_key, 16);
        sm4_set_encrypt_key(&sm4_key_, sm4_key_bytes_);
    }

    uint8_t HCCryptoDecryptor::GetFakeKey() const {
        return fake_key_;
    }

    void HCCryptoDecryptor::SetExpectedPostfixByte(uint8_t expected_postfix_byte) {
        expected_postfix_byte_ = expected_postfix_byte;
    }

    uint8_t HCCryptoDecryptor::GetExpectedPostfixByte() const {
        return expected_postfix_byte_;
    }

    void HCCryptoDecryptor::SetFailDecryption(bool fail_decryption) {
        fail_decryption_ = fail_decryption;
    }

    // SM4 相关方法实现
    void HCCryptoDecryptor::SetSM4Key(const uint8_t key[16]) {
        memcpy(sm4_key_bytes_, key, 16);
        sm4_set_encrypt_key(&sm4_key_, sm4_key_bytes_);
        RTC_LOG(LS_INFO) << "[HCCryptoDecryptor] SM4 key updated, decryption enabled";
    }

    void HCCryptoDecryptor::SetSM4CTR(const uint8_t ctr[16]) {
        memcpy(sm4_ctr_, ctr, 16);
        RTC_LOG(LS_INFO) << "[HCCryptoDecryptor] SM4 CTR updated";
    }

    void HCCryptoDecryptor::EnableSM4Decryption(bool enable) {
        use_sm4_decryption_ = enable;
        RTC_LOG(LS_INFO) << "[HCCryptoDecryptor] SM4 decryption " << (enable ? "enabled" : "disabled");
    }

}  // namespace webrtc
