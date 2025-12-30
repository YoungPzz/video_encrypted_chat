// 头文件保护宏：按 WebRTC 规范命名（基于文件路径）
#ifndef API_CRYPTO_HC_CRYPTO_H_
#define API_CRYPTO_HC_CRYPTO_H_

// 引用同级目录的 FrameEncryptorInterface（核心路径修改）
#include "api/crypto/frame_encryptor_interface.h"
// WebRTC 基础依赖（按源码根路径引用，无需相对路径）
#include "rtc_base/ref_count.h"
#include "rtc_base/buffer.h"
#include "api/media_types.h"  // 媒体类型（音频/视频）定义
#include "rtc_base/logging.h" // 日志工具
#include "rtc_base/ref_counted_object.h" // 引用计数对象

// 命名空间：建议放到 webrtc 下，符合 WebRTC 代码规范
namespace webrtc {

// 自定义加密器，继承 WebRTC 原生的 FrameEncryptorInterface
class HCCrypto : public FrameEncryptorInterface {
public:
    // 预估加密后的数据最大长度（必须实现的纯虚函数）
    size_t GetMaxCiphertextByteSize(cricket::MediaType media_type,
                                            size_t frame_size) override;

    // 核心加密逻辑（必须实现的纯虚函数）
    int Encrypt(cricket::MediaType media_type,
                uint32_t ssrc,
                rtc::ArrayView<const uint8_t> additional_data,
                rtc::ArrayView<const uint8_t> frame,
                rtc::ArrayView<uint8_t> encrypted_frame,
                size_t* bytes_written) override;
};

}  // namespace webrtc

#endif  // API_CRYPTO_HC_CRYPTO_H_