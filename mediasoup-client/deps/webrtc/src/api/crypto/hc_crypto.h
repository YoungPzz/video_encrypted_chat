/*
 *  Copyright 2018 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#ifndef API_TEST_FAKE_FRAME_ENCRYPTOR_H_
#define API_TEST_FAKE_FRAME_ENCRYPTOR_H_

#include <stddef.h>
#include <stdint.h>

#include "api/array_view.h"
#include "api/crypto/frame_encryptor_interface.h"
#include "api/media_types.h"
#include "rtc_base/ref_counted_object.h"
#include "rtc_base/logging.h" // 日志工具
#include "rtc_base/time_utils.h" // 提供 rtc::TimeMicros() 等高精度计时函数
#include "api/crypto/GmSSL-master/include/gmssl/sm4.h"

namespace webrtc {

// The FakeFrameEncryptor is a TEST ONLY fake implementation of the
// FrameEncryptorInterface. It is constructed with a simple single digit key and
// a fixed postfix byte. This is just to validate that the core code works
// as expected.
    class HCCrypto
            : public rtc::RefCountedObject<FrameEncryptorInterface> {
    public:
        // Provide a key (0,255) and some postfix byte (0,255).
        explicit HCCrypto(uint8_t fake_key = 0xAA,
                                    uint8_t postfix_byte = 255);
        // Simply xors each payload with the provided fake key and adds the postfix
        // bit to the end. This will always fail if fail_encryption_ is set to true.
        int Encrypt(cricket::MediaType media_type,
                    uint32_t ssrc,
                    rtc::ArrayView<const uint8_t> additional_data,
                    rtc::ArrayView<const uint8_t> frame,
                    rtc::ArrayView<uint8_t> encrypted_frame,
                    size_t* bytes_written) override;
        // Always returns 1 more than the size of the frame.
        size_t GetMaxCiphertextByteSize(cricket::MediaType media_type,
                                        size_t frame_size) override;
        // Sets the fake key to use during encryption.
        void SetFakeKey(uint8_t fake_key);
        // Returns the fake key used during encryption.
        uint8_t GetFakeKey() const;
        // Set the postfix byte to use.
        void SetPostfixByte(uint8_t expected_postfix_byte);
        // Return a postfix byte added to each outgoing payload.
        uint8_t GetPostfixByte() const;
        // Force all encryptions to fail.
        void SetFailEncryption(bool fail_encryption);

        enum class FakeEncryptionStatus : int {
            OK = 0,
            FORCED_FAILURE = 1,
        };

    public:
        // SM4 相关方法
        void SetSM4Key(const uint8_t key[16]);
        void SetSM4CTR(const uint8_t ctr[16]);
        void EnableSM4Encryption(bool enable);

    private:
        uint8_t fake_key_ = 0;
        uint8_t postfix_byte_ = 0;
        bool fail_encryption_ = false;

        // 新增：SM4-CTR 加密相关成员
        SM4_KEY sm4_key_;              // SM4 密钥上下文
        uint8_t sm4_key_bytes_[16];    // 16字节 SM4 密钥
        uint8_t sm4_ctr_[16];          // 16字节 CTR 计数器
        bool use_sm4_encryption_;      // 是否使用 SM4 加密

        // 预定义的固定 SM4 密钥和 CTR（用于初始化）
        static const uint8_t DEFAULT_SM4_KEY[16];
        static const uint8_t DEFAULT_SM4_CTR[16];

        // 统计相关变量
        int64_t v_total_us_ = 0;     // 视频总耗时（微秒）
        size_t v_frame_count_ = 0;   // 视频帧数计数
        int64_t v_max_us_ = 0;       // 视频单帧最大耗时
        size_t v_total_bytes_ = 0;   // 视频总处理字节数
    };

}  // namespace webrtc

#endif  // API_TEST_FAKE_FRAME_ENCRYPTOR_H_
