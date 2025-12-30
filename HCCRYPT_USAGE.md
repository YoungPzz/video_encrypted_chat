# HCCrypto 加密器使用说明

## 架构流程

```
Java 层 (应用代码)
    ↓
Producer.setFrameEncryptor(HCCrypto)
    ↓
JNI 层 (producer_jni.cpp)
    ↓
C++ Producer::SetFrameEncryptor()
    ↓
RtpSender->SetFrameEncryptor()
    ↓
HCCrypto::Encrypt() (实际加密逻辑)
```

## 文件修改清单

### 1. Java 层
- `mediasoup-client/src/main/java/org/webrtc/HCCrypto.java` - 加密器 Java 包装类

### 2. JNI 层
- `mediasoup-client/src/main/jni/hc_crypto_jni.cpp` - HCCrypto 的 JNI 绑定
- `mediasoup-client/src/main/jni/producer_jni.cpp` - Producer 的 JNI 绑定（已存在，已更新）

### 3. C++ 层
- `mediasoup-client/deps/webrtc/src/api/crypto/hc_crypto.h` - 加密器头文件
- `mediasoup-client/deps/webrtc/src/api/crypto/hc_crypto.cc` - 加密器实现
- `mediasoup-client/deps/libmediasoupclient/include/Producer.hpp` - Producer 头文件（已更新）
- `mediasoup-client/deps/libmediasoupclient/src/Producer.cpp` - Producer 实现（已更新）

### 4. 构建配置
- `mediasoup-client/CMakeLists.txt` - 添加 hc_crypto_jni.cpp 到源文件列表

## 使用方法

### 在 VideoChatActivity.java 中使用

```java
import org.webrtc.HCCrypto;
import org.mediasoup.droid.Producer;

// 在 RoomClient 中添加设置加密器的方法
public void setProducerEncryptor(Producer producer) {
    try {
        // 创建加密器实例
        HCCrypto encryptor = new HCCrypto();
        producer.setFrameEncryptor(encryptor);
        Log.d(TAG, "加密器设置成功");
    } catch (MediasoupException e) {
        Log.e(TAG, "设置加密器失败: " + e.getMessage());
    }
}

// 或者在 RoomClient.java 中修改 addTrack 方法

public void addTrack(VideoTrack videoTrack) {
    if (sendTransport != null && videoTrack != null) {
        try {
            videoProducer = sendTransport.produce(
                producer -> Log.d(TAG, "Video producer transport closed"),
                videoTrack,
                null,
                null,
                null
            );

            // 添加加密器设置
            if (videoProducer != null) {
                HCCrypto encryptor = new HCCrypto();
                videoProducer.setFrameEncryptor(encryptor);
                Log.d(TAG, "视频加密器已设置");
            }

            Log.d(TAG, "produce video track success");
        } catch (MediasoupException e) {
            Log.e(TAG, "Error adding video track: " + e.getMessage());
        }
    }
}
```

## 编译步骤

### 1. 重新编译 mediasoup-client 库

```bash
cd mediasoup-client
./gradlew clean assembleRelease
```

### 2. 编译应用

```bash
cd ..
./gradlew clean assembleDebug
```

## 验证加密是否生效

### 1. 查看日志输出

```bash
adb logcat | grep -E "HCCrypto|RtpSender"
```

应该能看到类似以下日志：
```
D/HCCrypto: 加密成功！媒体类型：视频，SSRC：123456，帧长度：10240
```

### 2. 验证视频流

如果加密成功，视频应该能够正常播放（因为你的加密使用的是简单的异或加密，接收端需要使用相同的异或密钥解密）。

## 注意事项

1. **对称加密密钥**：当前实现使用简单的异或加密（密钥 0xAB），生产环境应该使用更安全的加密算法（如 AES-GCM）。

2. **接收端解密**：需要在 Consumer 端实现对应的解密器 `HCDecrypto`。

3. **内存管理**：确保在不需要时调用 `HCCrypto.dispose()` 释放 Native 资源。

4. **线程安全**：加密过程可能在不同线程中调用，确保实现是线程安全的。

## 下一步扩展

### 1. 实现 HCDecrypto（解密器）

创建对应的解密器用于接收端：

```cpp
// hc_decryptor.h
class HCDecrypto : public FrameDecryptorInterface {
    // 解密实现
};
```

### 2. 支持动态密钥

```java
public void setKey(byte[] key) {
    // 通过 JNI 将密钥传递给 Native 层
}
```

### 3. 添加加密统计信息

```java
public String getStats() {
    // 返回加密统计信息
}
```
