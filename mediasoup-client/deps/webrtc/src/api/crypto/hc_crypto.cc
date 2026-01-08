#include "api/crypto/hc_crypto.h"
#include <cstring>

// 定义静态常量：预定义的固定 SM4 密钥和 CTR
namespace webrtc {
    const uint8_t HCCrypto::DEFAULT_SM4_KEY[16] = {
        0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF,
        0xFE, 0xDC, 0xBA, 0x98, 0x76, 0x54, 0x32, 0x10
    };
    
    const uint8_t HCCrypto::DEFAULT_SM4_CTR[16] = {
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
    };

    HCCrypto::HCCrypto(uint8_t fake_key, uint8_t postfix_byte)
            : fake_key_(fake_key), 
              postfix_byte_(postfix_byte),
              use_sm4_encryption_(true) {
        
        // 初始化 SM4 密钥上下文（使用预定义的固定密钥）
        memcpy(sm4_key_bytes_, DEFAULT_SM4_KEY, 16);
        sm4_set_encrypt_key(&sm4_key_, sm4_key_bytes_);
        
        // 初始化 CTR（使用预定义的固定 CTR）
        memcpy(sm4_ctr_, DEFAULT_SM4_CTR, 16);
    }
// 实现 GetMaxCiphertextByteSize 方法
    size_t HCCrypto::GetMaxCiphertextByteSize(
            cricket::MediaType media_type,
            size_t frame_size) {
        return frame_size + 1;
    }

    int HCCrypto::Encrypt(cricket::MediaType media_type,
                          uint32_t ssrc,
                          rtc::ArrayView<const uint8_t> additional_data,
                          rtc::ArrayView<const uint8_t> frame,
                          rtc::ArrayView<uint8_t> encrypted_frame,
                          size_t* bytes_written) {

        if (fail_encryption_) {
            return static_cast<int>(FakeEncryptionStatus::FORCED_FAILURE);
        }

        RTC_CHECK_EQ(frame.size() + 1, encrypted_frame.size());

        if (use_sm4_encryption_) {
            // 使用 SM4-CTR 加密
            uint8_t local_ctr[16];
            memcpy(local_ctr, sm4_ctr_, 16);  // 使用本地副本避免修改原始 CTR
            
            // 调用 GmSSL 的 SM4-CTR 加密函数
            sm4_ctr_encrypt(&sm4_key_, local_ctr,
                           frame.data(), frame.size(),
                           encrypted_frame.data());
            
            *bytes_written = frame.size();
            RTC_LOG(LS_INFO) << "[HCCrypto] SM4-CTR encrypted " << frame.size() << " bytes";
        } else {
            // 使用原有的 XOR 加密（保持向后兼容）
            for (size_t i = 0; i < frame.size(); i++) {
                encrypted_frame[i] = frame[i] ^ fake_key_;
            }
            *bytes_written = frame.size();
            RTC_LOG(LS_INFO) << "[HCCrypto] XOR encrypted " << frame.size() << " bytes";
        }

        encrypted_frame[frame.size()] = postfix_byte_;
        *bytes_written += 1;
        
        RTC_LOG(LS_INFO) << "[HCCrypto] encrypt success, status=" << FakeEncryptionStatus::OK;
        return static_cast<int>(FakeEncryptionStatus::OK);
    }
    void HCCrypto::SetFakeKey(uint8_t fake_key) {
        fake_key_ = fake_key;
        // 同时更新 SM4 密钥（使用 fake_key 填充 16 字节）
        memset(sm4_key_bytes_, fake_key, 16);
        sm4_set_encrypt_key(&sm4_key_, sm4_key_bytes_);
    }

    uint8_t HCCrypto::GetFakeKey() const {
        return fake_key_;
    }

    void HCCrypto::SetPostfixByte(uint8_t postfix_byte) {
        postfix_byte_ = postfix_byte;
    }

    uint8_t HCCrypto::GetPostfixByte() const {
        return postfix_byte_;
    }

    void HCCrypto::SetFailEncryption(bool fail_encryption) {
        fail_encryption_ = fail_encryption;
    }

    // SM4 相关方法实现
    void HCCrypto::SetSM4Key(const uint8_t key[16]) {
        memcpy(sm4_key_bytes_, key, 16);
        sm4_set_encrypt_key(&sm4_key_, sm4_key_bytes_);
        RTC_LOG(LS_INFO) << "[HCCrypto] SM4 key updated, encryption enabled";
    }

    void HCCrypto::SetSM4CTR(const uint8_t ctr[16]) {
        memcpy(sm4_ctr_, ctr, 16);
        RTC_LOG(LS_INFO) << "[HCCrypto] SM4 CTR updated";
    }

    void HCCrypto::EnableSM4Encryption(bool enable) {
        use_sm4_encryption_ = enable;
        RTC_LOG(LS_INFO) << "[HCCrypto] SM4 encryption " << (enable ? "enabled" : "disabled");
    }
}  // namespace webrtc