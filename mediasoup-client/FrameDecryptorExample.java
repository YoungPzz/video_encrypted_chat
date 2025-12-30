package org.mediasoup.droid;

import org.webrtc.FrameDecryptor;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.RTPReceiver;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;

/**
 * FrameDecryptor使用示例
 * 展示如何实现WebRTC的FrameDecryptor接口并与RTPReceiver一起使用
 */
public class FrameDecryptorExample {

    // 自定义FrameDecryptor实现
    private static class CustomFrameDecryptor implements FrameDecryptor {
        
        // 解密密钥（必须与加密密钥相同）
        private final byte[] decryptionKey;
        
        public CustomFrameDecryptor(byte[] decryptionKey) {
            this.decryptionKey = decryptionKey;
        }
        
        @Override
        public int decryptFrame(byte[] encryptedFrame, byte[] additionalData, byte[] frame,
                                DecryptionInfo decryptionInfo) {
            try {
                // 这里实现具体的解密逻辑
                // 示例：简单的XOR解密（与加密对应）
                for (int i = 0; i < encryptedFrame.length; i++) {
                    frame[i] = (byte) (encryptedFrame[i] ^ decryptionKey[i % decryptionKey.length]);
                }
                
                // 返回解密后的帧长度
                return encryptedFrame.length;
            } catch (Exception e) {
                e.printStackTrace();
                return -1; // 解密失败
            }
        }
    }
    
    /**
     * 为RTPReceiver设置FrameDecryptor
     * @param peerConnection PeerConnection实例
     * @param videoTrack VideoTrack实例
     * @return RTPReceiver实例
     */
    public RTPReceiver setupFrameDecryptor(PeerConnection peerConnection, VideoTrack videoTrack) {
        // 1. 创建解密密钥（必须与发送端的加密密钥相同）
        byte[] decryptionKey = "my-encryption-key-123".getBytes();
        
        // 2. 创建自定义FrameDecryptor
        CustomFrameDecryptor frameDecryptor = new CustomFrameDecryptor(decryptionKey);
        
        // 3. 获取RTPReceiver
        RTPReceiver rtpReceiver = findRTPReceiverForTrack(peerConnection, videoTrack);
        
        if (rtpReceiver != null) {
            // 4. 设置FrameDecryptor
            // 注意：这里需要使用反射来调用private的native方法
            try {
                // 获取nativeSetFrameDecryptor方法
                java.lang.reflect.Method method = RTPReceiver.class.getDeclaredMethod(
                        "nativeSetFrameDecryptor", long.class, long.class);
                method.setAccessible(true);
                
                // 获取RTPReceiver的native指针
                java.lang.reflect.Field nativeField = RTPReceiver.class.getDeclaredField("nativeRtpReceiver");
                nativeField.setAccessible(true);
                long nativeRtpReceiver = nativeField.getLong(rtpReceiver);
                
                // 获取FrameDecryptor的native指针
                java.lang.reflect.Field decryptorNativeField = FrameDecryptor.class.getDeclaredField("nativeFrameDecryptor");
                decryptorNativeField.setAccessible(true);
                long nativeFrameDecryptor = decryptorNativeField.getLong(frameDecryptor);
                
                // 调用native方法设置FrameDecryptor
                method.invoke(null, nativeRtpReceiver, nativeFrameDecryptor);
                
                System.out.println("FrameDecryptor设置成功");
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("设置FrameDecryptor失败: " + e.getMessage());
            }
        }
        
        return rtpReceiver;
    }
    
    /**
     * 查找与指定轨道关联的RTPReceiver
     * @param peerConnection PeerConnection实例
     * @param track MediaStreamTrack实例
     * @return 对应的RTPReceiver实例，找不到返回null
     */
    private RTPReceiver findRTPReceiverForTrack(PeerConnection peerConnection, MediaStreamTrack track) {
        for (RTPReceiver receiver : peerConnection.getReceivers()) {
            if (receiver.track() == track) {
                return receiver;
            }
        }
        return null;
    }
}

/**
 * 完整的加密解密使用示例
 */
class FullEncryptionExample {
    
    private byte[] encryptionKey = "my-encryption-key-123".getBytes();
    
    /**
     * 发送端设置
     */
    public void setupSender(PeerConnection peerConnection, VideoTrack videoTrack) {
        FrameEncryptorExample encryptorExample = new FrameEncryptorExample();
        encryptorExample.setupFrameEncryptor(peerConnection, videoTrack);
    }
    
    /**
     * 接收端设置
     */
    public void setupReceiver(PeerConnection peerConnection, VideoTrack videoTrack) {
        FrameDecryptorExample decryptorExample = new FrameDecryptorExample();
        decryptorExample.setupFrameDecryptor(peerConnection, videoTrack);
    }
}
