# WebRTC Frame Encryption Usage Guide

This guide explains how to implement and use WebRTC's frame encryption functionality, specifically focusing on the `nativeSetFrameEncryptor` method in the `RTPSender` class.

## Overview

WebRTC provides a frame encryption API that allows you to encrypt media frames before they are sent over the network. This is achieved through:

1. **`FrameEncryptor` interface**: For encrypting outgoing frames
2. **`RTPSender.nativeSetFrameEncryptor()`**: For attaching an encryptor to an RTP sender
3. **`FrameDecryptor` interface**: For decrypting incoming frames (on the receiving side)

## Implementation Steps

### 1. Implement the `FrameEncryptor` Interface

```java
import org.webrtc.FrameEncryptor;

public class CustomFrameEncryptor implements FrameEncryptor {
    private final byte[] encryptionKey;
    
    public CustomFrameEncryptor(byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
    
    @Override
    public int encryptFrame(byte[] frame, byte[] additionalData, byte[] encryptedFrame,
                            EncryptionInfo encryptionInfo) {
        try {
            // Implement your encryption logic here
            // Example: XOR encryption (not secure for production!)
            for (int i = 0; i < frame.length; i++) {
                encryptedFrame[i] = (byte) (frame[i] ^ encryptionKey[i % encryptionKey.length]);
            }
            
            return frame.length; // Return encrypted frame size
        } catch (Exception e) {
            e.printStackTrace();
            return -1; // Return -1 on failure
        }
    }
    
    @Override
    public int getMaxCiphertextByteSize(int frameSize) {
        // Return the maximum possible size of encrypted frames
        // For many algorithms, this is the same as the original size
        return frameSize;
    }
}
```

### 2. Implement the `FrameDecryptor` Interface (Receiving Side)

```java
import org.webrtc.FrameDecryptor;

public class CustomFrameDecryptor implements FrameDecryptor {
    private final byte[] decryptionKey;
    
    public CustomFrameDecryptor(byte[] decryptionKey) {
        this.decryptionKey = decryptionKey;
    }
    
    @Override
    public int decryptFrame(byte[] encryptedFrame, byte[] additionalData, byte[] frame,
                            DecryptionInfo decryptionInfo) {
        try {
            // Implement your decryption logic here
            // Example: XOR decryption (matches the encryption example)
            for (int i = 0; i < encryptedFrame.length; i++) {
                frame[i] = (byte) (encryptedFrame[i] ^ decryptionKey[i % decryptionKey.length]);
            }
            
            return encryptedFrame.length; // Return decrypted frame size
        } catch (Exception e) {
            e.printStackTrace();
            return -1; // Return -1 on failure
        }
    }
}
```

### 3. Attach FrameEncryptor to RTPSender

Since `nativeSetFrameEncryptor` is a private method, you'll need to use reflection to access it:

```java
import org.webrtc.RTPSender;

public void attachFrameEncryptor(RTPSender rtpSender, FrameEncryptor frameEncryptor) {
    try {
        // Get the nativeSetFrameEncryptor method
        java.lang.reflect.Method method = RTPSender.class.getDeclaredMethod(
                "nativeSetFrameEncryptor", long.class, long.class);
        method.setAccessible(true);
        
        // Get the native pointer for RTPSender
        java.lang.reflect.Field senderNativeField = RTPSender.class.getDeclaredField("nativeRtpSender");
        senderNativeField.setAccessible(true);
        long nativeRtpSender = senderNativeField.getLong(rtpSender);
        
        // Get the native pointer for FrameEncryptor
        java.lang.reflect.Field encryptorNativeField = FrameEncryptor.class.getDeclaredField("nativeFrameEncryptor");
        encryptorNativeField.setAccessible(true);
        long nativeFrameEncryptor = encryptorNativeField.getLong(frameEncryptor);
        
        // Call the native method
        method.invoke(null, nativeRtpSender, nativeFrameEncryptor);
        
        System.out.println("FrameEncryptor attached successfully");
    } catch (Exception e) {
        e.printStackTrace();
        System.err.println("Failed to attach FrameEncryptor: " + e.getMessage());
    }
}
```

### 4. Attach FrameDecryptor to RTPReceiver (Receiving Side)

```java
import org.webrtc.RTPReceiver;
import org.webrtc.FrameDecryptor;

public void attachFrameDecryptor(RTPReceiver rtpReceiver, FrameDecryptor frameDecryptor) {
    try {
        // Get the nativeSetFrameDecryptor method
        java.lang.reflect.Method method = RTPReceiver.class.getDeclaredMethod(
                "nativeSetFrameDecryptor", long.class, long.class);
        method.setAccessible(true);
        
        // Get the native pointer for RTPReceiver
        java.lang.reflect.Field receiverNativeField = RTPReceiver.class.getDeclaredField("nativeRtpReceiver");
        receiverNativeField.setAccessible(true);
        long nativeRtpReceiver = receiverNativeField.getLong(rtpReceiver);
        
        // Get the native pointer for FrameDecryptor
        java.lang.reflect.Field decryptorNativeField = FrameDecryptor.class.getDeclaredField("nativeFrameDecryptor");
        decryptorNativeField.setAccessible(true);
        long nativeFrameDecryptor = decryptorNativeField.getLong(frameDecryptor);
        
        // Call the native method
        method.invoke(null, nativeRtpReceiver, nativeFrameDecryptor);
        
        System.out.println("FrameDecryptor attached successfully");
    } catch (Exception e) {
        e.printStackTrace();
        System.err.println("Failed to attach FrameDecryptor: " + e.getMessage());
    }
}
```

## Complete Usage Example

```java
import org.webrtc.PeerConnection;
import org.webrtc.VideoTrack;
import org.webrtc.RTPSender;
import org.webrtc.FrameEncryptor;

public class EncryptionSetup {
    
    private byte[] encryptionKey = "secure-encryption-key-1234".getBytes();
    
    public void setupEncryption(PeerConnection peerConnection, VideoTrack videoTrack) {
        // 1. Find RTPSender for the video track
        RTPSender rtpSender = findRTPSender(peerConnection, videoTrack);
        
        if (rtpSender != null) {
            // 2. Create custom FrameEncryptor
            FrameEncryptor frameEncryptor = new CustomFrameEncryptor(encryptionKey);
            
            // 3. Attach FrameEncryptor to RTPSender
            attachFrameEncryptor(rtpSender, frameEncryptor);
        }
    }
    
    private RTPSender findRTPSender(PeerConnection peerConnection, VideoTrack videoTrack) {
        for (RTPSender sender : peerConnection.getSenders()) {
            if (sender.track() == videoTrack) {
                return sender;
            }
        }
        return null;
    }
    
    // Other methods (attachFrameEncryptor, CustomFrameEncryptor, etc.)
}
```

## Important Considerations

### Security
- **Use Strong Encryption**: The XOR example provided is for demonstration only. Use strong encryption algorithms like AES-256 in production.
- **Key Management**: Implement a secure key exchange mechanism (e.g., using signaling server).
- **Key Rotation**: Consider rotating keys periodically for enhanced security.

### Performance
- **Encryption Overhead**: Encryption/decryption adds computational overhead. Test performance with your chosen algorithm.
- **Frame Size**: Ensure your implementation handles large frames properly (WebRTC may split frames).

### Error Handling
- **Return Values**: Always check return values from encryption/decryption methods.
- **Failure Recovery**: Implement strategies for handling decryption failures (e.g., drop frames or notify user).

### API Limitations
- **Private Methods**: Using reflection to access private methods may break in future WebRTC versions.
- **Native Integration**: The frame encryption API has tight integration with WebRTC's native code.

## Files Included

1. **`FrameEncryptorExample.java`**: Complete implementation of frame encryption for senders
2. **`FrameDecryptorExample.java`**: Complete implementation of frame decryption for receivers
3. **`FRAME_ENCRYPTION_USAGE.md`**: This guide

## Troubleshooting

### Common Issues

1. **No Encryption Happening**
   - Ensure `nativeSetFrameEncryptor` is called successfully
   - Check that the correct RTPSender is used
   - Verify encryption algorithm implementation

2. **Decryption Failures**
   - Ensure encryption and decryption keys match
   - Check that the same algorithm is used for both
   - Verify frame integrity

3. **Performance Issues**
   - Optimize encryption algorithm
   - Consider using hardware acceleration
   - Reduce encryption key size if using symmetric encryption

### Debugging Tips
- Enable WebRTC logging to see more details about the encryption process
- Use network analysis tools (Wireshark) to verify frames are encrypted
- Add detailed logging to your encryption/decryption implementations

## Conclusion

WebRTC's frame encryption API provides a powerful way to secure media streams. By implementing the `FrameEncryptor` interface and attaching it to an `RTPSender` using `nativeSetFrameEncryptor`, you can ensure your media frames are encrypted before being sent over the network.

Remember to implement proper security practices, test thoroughly, and handle errors gracefully to ensure a reliable and secure implementation.
