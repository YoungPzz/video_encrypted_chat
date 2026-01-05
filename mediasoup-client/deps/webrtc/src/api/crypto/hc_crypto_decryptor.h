#ifndef API_CRYPTO_HC_CRYPTO_DECRYPTOR_H_
#define API_CRYPTO_HC_CRYPTO_DECRYPTOR_H_

#include <stddef.h>
#include <stdint.h>

#include <vector>

#include "api/array_view.h"
#include "api/crypto/frame_decryptor_interface.h"
#include "api/media_types.h"
#include "rtc_base/ref_counted_object.h"
#include "rtc_base/logging.h" // 日志工具

namespace webrtc {

// 自定义解密器，继承 WebRTC 原生的 FrameDecryptorInterface
    class HCCryptoDecryptor
            : public rtc::RefCountedObject<FrameDecryptorInterface> {
    public:
        // Provide a key (0,255) and some postfix byte (0,255) this should match the
        // byte you expect from the FakeFrameEncryptor.
        explicit HCCryptoDecryptor(uint8_t fake_key = 0xAA,
                                    uint8_t expected_postfix_byte = 255);
        // Fake decryption that just xors the payload with the 1 byte key and checks
        // the postfix byte. This will always fail if fail_decryption_ is set to true.
        Result Decrypt(cricket::MediaType media_type,
                       const std::vector<uint32_t>& csrcs,
                       rtc::ArrayView<const uint8_t> additional_data,
                       rtc::ArrayView<const uint8_t> encrypted_frame,
                       rtc::ArrayView<uint8_t> frame) override;
        // Always returns 1 less than the size of the encrypted frame.
        size_t GetMaxPlaintextByteSize(cricket::MediaType media_type,
                                       size_t encrypted_frame_size) override;
        // Sets the fake key to use for encryption.
        void SetFakeKey(uint8_t fake_key);
        // Returns the fake key used for encryption.
        uint8_t GetFakeKey() const;
        // Set the Postfix byte that is expected in the encrypted payload.
        void SetExpectedPostfixByte(uint8_t expected_postfix_byte);
        // Returns the postfix byte that will be checked for in the encrypted payload.
        uint8_t GetExpectedPostfixByte() const;
        // If set to true will force all encryption to fail.
        void SetFailDecryption(bool fail_decryption);
        // Simple error codes for tests to validate against.
        enum class FakeDecryptStatus : int {
            OK = 0,
            FORCED_FAILURE = 1,
            INVALID_POSTFIX = 2
        };

    private:
        uint8_t fake_key_ = 0;
        uint8_t expected_postfix_byte_ = 0;
        bool fail_decryption_ = false;
    };

}  // namespace webrtc

#endif  // API_CRYPTO_HC_CRYPTO_DECRYPTOR_H_
