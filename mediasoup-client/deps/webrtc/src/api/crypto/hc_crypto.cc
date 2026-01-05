#include "api/crypto/hc_crypto.h"

// 命名空间：与头文件一致
namespace webrtc {
    HCCrypto::HCCrypto(uint8_t fake_key, uint8_t postfix_byte)
            : fake_key_(fake_key), postfix_byte_(postfix_byte) {}
// 实现 GetMaxCiphertextByteSize 方法
    size_t HCCrypto::GetMaxCiphertextByteSize(
            cricket::MediaType media_type,
            size_t frame_size) {
        return frame_size + 1;
    }

// 实现 Encrypt 方法（简单 XOR 加密，演示用）
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
        for (size_t i = 0; i < frame.size(); i++) {
            encrypted_frame[i] = frame[i] ^ fake_key_;
        }

        encrypted_frame[frame.size()] = postfix_byte_;
        *bytes_written = encrypted_frame.size();
        RTC_LOG(LS_INFO) << "[HCCrypto] fake encrypt" << FakeEncryptionStatus::OK;
        return static_cast<int>(FakeEncryptionStatus::OK);
    }
    void HCCrypto::SetFakeKey(uint8_t fake_key) {
        fake_key_ = fake_key;
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
}  // namespace webrtc