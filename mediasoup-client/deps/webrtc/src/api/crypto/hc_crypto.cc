#include "api/crypto/hc_crypto.h"

// 命名空间：与头文件一致
namespace webrtc {

// 实现 GetMaxCiphertextByteSize 方法
    size_t HCCrypto::GetMaxCiphertextByteSize(cricket::MediaType media_type,
                                              size_t frame_size) {
        // 示例：加密后最大长度 = 明文长度 + 16 字节（IV/TAG 开销）
        return frame_size + 16;
    }

// 实现 Encrypt 方法（简单 XOR 加密，演示用）
    int HCCrypto::Encrypt(cricket::MediaType media_type,
                          uint32_t ssrc,
                          rtc::ArrayView<const uint8_t> additional_data,  // 同步修改参数类型
                          rtc::ArrayView<const uint8_t> frame,
                          rtc::ArrayView<uint8_t> encrypted_frame,
                          size_t* bytes_written) {
        // 1. 校验缓冲区是否足够
        if (encrypted_frame.size() < GetMaxCiphertextByteSize(media_type, frame.size())) {
            RTC_LOG(LS_ERROR) << "[HCCrypto] 加密缓冲区不足！明文长度：" << frame.size()
                              << "，缓冲区长度：" << encrypted_frame.size();
            return -1; // 加密失败
        }

        // 2. 简单 XOR 加密（演示用，生产环境替换为 AES-GCM）
        const uint8_t key = 0xAB; // 加密密钥（需与接收端一致）
        for (size_t i = 0; i < frame.size(); ++i) {
            encrypted_frame[i] = frame[i] ^ key; // 逐字节 XOR 加密
        }

        // 3. 设置实际加密后的数据长度
        *bytes_written = frame.size();

        RTC_LOG(LS_INFO) << "[HCCrypto] 加密成功！媒体类型："
                         << (media_type == cricket::MEDIA_TYPE_AUDIO ? "音频" : "视频")
                         << "，SSRC：" << ssrc << "，帧长度：" << frame.size();
        return 0; // 加密成功返回 0
    }

}  // namespace webrtc