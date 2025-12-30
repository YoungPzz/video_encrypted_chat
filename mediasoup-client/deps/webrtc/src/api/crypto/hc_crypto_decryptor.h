#ifndef API_CRYPTO_HC_CRYPTO_DECRYPTOR_H_
#define API_CRYPTO_HC_CRYPTO_DECRYPTOR_H_

// 引用WebRTC解密接口
#include "api/crypto/frame_decryptor_interface.h"
// WebRTC基础依赖
#include "api/media_types.h"
#include "rtc_base/array_view.h"
#include "rtc_base/logging.h"
#include "rtc_base/ref_count.h"

namespace webrtc {

// 自定义XOR解密器：对应XOR加密器
    class HCCryptoDecryptor : public FrameDecryptorInterface {
    public:
        // 构造函数：传入XOR密钥（与加密侧一致）
        explicit HCCryptoDecryptor(uint8_t xor_key);
        ~HCCryptoDecryptor() override = default;

        // 核心解密方法（重写父类虚函数）
        Result Decrypt(cricket::MediaType media_type,
                       const std::vector<uint32_t>& ssrcs,
                       rtc::ArrayView<const uint8_t> additional_data,
                       rtc::ArrayView<const uint8_t> encrypted_frame,
                       rtc::ArrayView<uint8_t> frame) override;

        // 计算解密后明文的最大长度（与加密侧GetMaxCiphertextByteSize对应）
        size_t GetMaxPlaintextByteSize(cricket::MediaType media_type,
                                       size_t encrypted_frame_size) override;

    private:
        uint8_t xor_key_;  // XOR密钥（与加密侧完全一致）
    };

}  // namespace webrtc

#endif  // API_CRYPTO_HC_CRYPTO_DECRYPTOR_H_