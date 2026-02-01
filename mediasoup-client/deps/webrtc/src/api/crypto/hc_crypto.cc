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
        if (fail_encryption_) return static_cast<int>(FakeEncryptionStatus::FORCED_FAILURE);

        int64_t start_time = rtc::TimeMicros();

        // 1. 识别 VP8 关键帧
        bool is_key_frame = false;
        if (media_type == cricket::MEDIA_TYPE_VIDEO && frame.size() > 0) {
            is_key_frame = ((frame[0] & 0x01) == 0);
        }

        // 2. 根据 use_sm4_encryption_ 定义判定逻辑
        // true -> 全量加密; false -> 仅关键帧加密
        bool perform_crypto = use_sm4_encryption_ ? true : is_key_frame;

        RTC_CHECK_EQ(frame.size() + 1, encrypted_frame.size());

        if (perform_crypto) {
            // 执行 SM4 加密运算
            uint8_t local_ctr[16];
            memcpy(local_ctr, sm4_ctr_, 16);
            sm4_ctr_encrypt(&sm4_key_, local_ctr, frame.data(), frame.size(), encrypted_frame.data());
            encrypted_frame[frame.size()] = postfix_byte_; // 写入加密后缀标记
        } else {
            // 不满足加密条件（即：Mode为关键帧模式且当前是P帧），执行透传
            memcpy(encrypted_frame.data(), frame.data(), frame.size());
            encrypted_frame[frame.size()] = 0x00; // 写入非加密后缀标记
        }

        *bytes_written = frame.size() + 1;
        int64_t elapsed_us = rtc::TimeMicros() - start_time;

        // 3. 数据统计与对比处理
        if (media_type == cricket::MEDIA_TYPE_VIDEO) {
            v_total_us_ += elapsed_us;
            v_frame_count_++;
            if (is_key_frame) {
                v_key_frame_count_++;
                v_key_frame_total_us_ += elapsed_us;
                if (elapsed_us > v_max_us_) v_max_us_ = elapsed_us;
            }

            if (v_frame_count_ >= 300) {
                double total_avg = static_cast<double>(v_total_us_) / v_frame_count_;
                double key_avg = (v_key_frame_count_ > 0) ? (double)v_key_frame_total_us_ / v_key_frame_count_ : 0;

                RTC_LOG(LS_WARNING) << ">>> [Encrypt Contrast Report] <<<"
                                    << "\n| Current Mode: " << (use_sm4_encryption_ ? "FULL-ENCRYPT" : "KEY-FRAME-ONLY")
                                    << "\n| Total System Avg: " << total_avg << " us"
                                    << "\n| I-Frame Crypto Avg: " << key_avg << " us"
                                    << "\n| Max Jitter (I-Frame): " << v_max_us_ << " us"
                                    << "\n| Sample Frames: " << v_frame_count_ << " (Keys: " << v_key_frame_count_ << ")";

                // 重置统计量
                v_total_us_ = 0; v_frame_count_ = 0; v_key_frame_count_ = 0; v_key_frame_total_us_ = 0; v_max_us_ = 0;
            }
        }
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