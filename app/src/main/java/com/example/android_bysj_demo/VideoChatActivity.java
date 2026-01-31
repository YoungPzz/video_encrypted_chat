package com.example.android_bysj_demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import org.json.JSONObject;
import org.mediasoup.droid.MediasoupClient;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.EncodedImage;
import org.webrtc.FrameEncryptor;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCodecStatus;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.*;


import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class VideoChatActivity extends Activity {
    private static final String TAG = "VideoChatActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
    };

    // UI Components
    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private ImageButton toggleAudioButton;
    private ImageButton toggleVideoButton;
    private ImageButton hangupButton;
    private TextView statusTextView;
    private TextView timerTextView;

    // WebRTC Components
    private EglBase eglBase;
    private PeerConnectionFactory peerConnectionFactory;
    private VideoCapturer videoCapturer;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private SurfaceTextureHelper surfaceTextureHelper;

    // Mediasoup Components
    private RoomClient roomClient;

    // State
    private boolean isAudioEnabled = true;
    private boolean isVideoEnabled = true;
    private boolean isOnlyAudio = false;
    private String roomId;
    private String userId;
    private long callStartTime;
    private Handler timerHandler;
    private boolean isCallConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);

        // Get intent extras
        roomId = getIntent().getStringExtra("ROOM_ID");
        userId = getIntent().getStringExtra("USER_ID");

        isOnlyAudio = getIntent().getBooleanExtra("isOnlyAudio", false);
        initViews();
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            initializeCall();
        }
    }

    private void initializeCall() {
//        initViews();
        roomClient = new RoomClient(this, userId, roomId);
        initWebRTC();
        initMediasoup();
        startCall();
    }

    private void initViews() {
        localVideoView = (SurfaceViewRenderer) findViewById(R.id.local_video_view);
        remoteVideoView = (SurfaceViewRenderer) findViewById(R.id.remote_video_view);
        toggleAudioButton = (ImageButton) findViewById(R.id.toggle_audio_button);
        toggleVideoButton = (ImageButton) findViewById(R.id.toggle_video_button);
        hangupButton = (ImageButton) findViewById(R.id.hangup_button);
        statusTextView = (TextView) findViewById(R.id.status_text_view);
        timerTextView = (TextView) findViewById(R.id.timer_text_view);

        setupClickListeners();


//        if (isOnlyAudio) {
//            localVideoView.setVisibility(View.GONE);
//            toggleVideoButton.setVisibility(View.GONE);
//        }
    }

    private void setupClickListeners() {
        toggleAudioButton.setOnClickListener(v -> toggleAudio());
        toggleVideoButton.setOnClickListener(v -> toggleVideo());
        hangupButton.setOnClickListener(v -> hangup());
    }


//    /**
//     * 使用异或操作加密视频帧的编码器包装类
//     */
//    public class XorEncryptingVideoEncoder implements VideoEncoder {
//        private final VideoEncoder originalEncoder;
//        private final byte xorKey; // 简单的单字节异或密钥
//
//        public XorEncryptingVideoEncoder(VideoEncoder originalEncoder, byte xorKey) {
//            this.originalEncoder = originalEncoder;
//            this.xorKey = xorKey;
//        }
//
//        @Override
//        public VideoCodecStatus initEncode(Settings settings, Callback callback) {
//            // 包装回调以便在编码后加密
//            Log.d(TAG, "XorEncryptingVideoEncoder.initEncode called");
//            return originalEncoder.initEncode(settings, new EncryptingCallback(callback, xorKey));
//        }
//
//        @Override
//        public VideoCodecStatus release() {
//            return originalEncoder.release();
//        }
//
//        @Override
//        public VideoCodecStatus encode(VideoFrame videoFrame, EncodeInfo encodeInfo) {
//            // 传递给原始编码器处理
//            Log.d(TAG, "XorEncryptingVideoEncoder.encode called");
//            return originalEncoder.encode(videoFrame, encodeInfo);
//        }
//
//        @Override
//        public VideoCodecStatus setRateAllocation(BitrateAllocation bitrateAllocation, int frameRate) {
//            return originalEncoder.setRateAllocation(bitrateAllocation, frameRate);
//        }
//
//        @Override
//        public ScalingSettings getScalingSettings() {
//            return originalEncoder.getScalingSettings();
//        }
//
//        @Override
//        public String getImplementationName() {
//            return "XorEncrypted-";
//        }
//
//        /**
//         * 包装编码器回调，用于加密编码后的帧
//         */
//        private  class EncryptingCallback implements Callback {
//            private final Callback originalCallback;
//            private final byte xorKey;
//
//            public EncryptingCallback(Callback originalCallback, byte xorKey) {
//                this.originalCallback = originalCallback;
//                this.xorKey = xorKey;
//            }
//
//            @Override
//            public void onEncodedFrame(EncodedImage encodedImage, CodecSpecificInfo codecSpecificInfo) {
//                // 获取编码后的数据
//                ByteBuffer buffer = encodedImage.buffer;
//                int size = buffer.remaining();
//                Log.d(TAG, "Encrypting frame: size=" + size +
//                        " bytes, frameType=" + encodedImage.frameType +
//                        ", timestamp=" + encodedImage.captureTimeNs);
//
//                // 创建新的缓冲区来存储加密数据
//                byte[] encryptedData = new byte[size];
//
//                // 保存原始位置
//                int originalPosition = buffer.position();
//
//                // 复制并加密数据
//                for (int i = 0; i < size; i++) {
//                    encryptedData[i] = (byte) (buffer.get() ^ xorKey); // 简单异或加密
//                }
//
//                // 创建一个简单的释放回调
//                Runnable releaseCallback = new Runnable() {
//                    @Override
//                    public void run() {
//                        // 这里可以添加清理逻辑，如果需要
//                        // 对于ByteBuffer.wrap创建的缓冲区，通常不需要特别的释放操作
//                        // 但保留这个回调以满足API要求
//                    }
//                };
//
//                // 重置缓冲区位置
//                buffer.position(originalPosition);
//                // 将加密后的字节数组转换为ByteBuffer
//                ByteBuffer encryptedBuffer = ByteBuffer.wrap(encryptedData);
//
//                // 创建新的EncodedImage - 使用适当的公共API
//                EncodedImage.Builder builder = EncodedImage.builder()
//                        .setBuffer(encryptedBuffer, releaseCallback)
//                        .setFrameType(encodedImage.frameType)
//                        .setRotation(encodedImage.rotation)
//                        .setCaptureTimeNs(encodedImage.captureTimeNs);
//
//                // 设置其他可用的属性
//                if (encodedImage.qp != null) {
//                    builder.setQp(encodedImage.qp);
//                }
//
//                EncodedImage encryptedImage = builder.createEncodedImage();
//
//                // 调用原始回调
//                originalCallback.onEncodedFrame(encryptedImage, codecSpecificInfo);
//            }
//        }
//    }
//
//    /**
//     * 使用异或操作解密视频帧的解码器包装类
//     */
//    public class XorDecryptingVideoDecoder implements VideoDecoder {
//        private final VideoDecoder originalDecoder;
//        private final byte xorKey;
//
//        public XorDecryptingVideoDecoder(VideoDecoder originalDecoder, byte xorKey) {
//            this.originalDecoder = originalDecoder;
//            this.xorKey = xorKey;
//        }
//
//        @Override
//        public VideoCodecStatus initDecode(Settings settings, Callback callback) {
//            return originalDecoder.initDecode(settings, callback);
//        }
//
//        @Override
//        public VideoCodecStatus release() {
//            return originalDecoder.release();
//        }
//
//        @Override
//        public VideoCodecStatus decode(EncodedImage encodedImage, DecodeInfo decodeInfo) {
//            // 获取加密的数据
//            ByteBuffer buffer = encodedImage.buffer;
//            int size = buffer.remaining();
//            Log.d(TAG, "Decrypting frame: size=" + size +
//                    " bytes, frameType=" + encodedImage.frameType +
//                    ", timestamp=" + encodedImage.captureTimeNs);
//
//            // 创建新的缓冲区来存储解密数据
//            byte[] decryptedData = new byte[size];
//
//            // 保存原始位置
//            int originalPosition = buffer.position();
//
//            // 复制并解密数据
//            for (int i = 0; i < size; i++) {
//                decryptedData[i] = (byte) (buffer.get() ^ xorKey); // 简单异或解密
//            }
//
//
//            // 创建一个简单的释放回调
//            Runnable releaseCallback = new Runnable() {
//                @Override
//                public void run() {
//                    // 这里可以添加清理逻辑，如果需要
//                    // 对于ByteBuffer.wrap创建的缓冲区，通常不需要特别的释放操作
//                    // 但保留这个回调以满足API要求
//                }
//            };
//
//            // 重置缓冲区位置
//            buffer.position(originalPosition);
//            // 将加密后的字节数组转换为ByteBuffer
//            ByteBuffer decryptedBuffer = ByteBuffer.wrap(decryptedData);
//
//            // 创建新的EncodedImage - 使用适当的公共API
//            EncodedImage.Builder builder = EncodedImage.builder()
//                    .setBuffer(decryptedBuffer, releaseCallback)
//                    .setFrameType(encodedImage.frameType)
//                    .setRotation(encodedImage.rotation)
//                    .setCaptureTimeNs(encodedImage.captureTimeNs);
//
//            // 设置其他可用的属性
//            if (encodedImage.qp != null) {
//                builder.setQp(encodedImage.qp);
//            }
//
//            EncodedImage decryptedImage = builder.createEncodedImage();
//
//            // 传递给原始解码器
//            return originalDecoder.decode(decryptedImage, decodeInfo);
//        }
//
//        @Override
//        public String getImplementationName() {
//            return "XorDecrypted-" + originalDecoder.getImplementationName();
//        }
//    }
//
//    /**
//     * 加密视频编码器工厂
//     */
//    public class XorEncryptedVideoEncoderFactory implements VideoEncoderFactory {
//        private final VideoEncoderFactory originalFactory;
//        private final byte xorKey;
//
//        public XorEncryptedVideoEncoderFactory(VideoEncoderFactory originalFactory, byte xorKey) {
//            this.originalFactory = originalFactory;
//            this.xorKey = xorKey;
//        }
//
//        @Override
//        public VideoEncoder createEncoder(VideoCodecInfo codecInfo) {
//            VideoEncoder originalEncoder = originalFactory.createEncoder(codecInfo);
//            if (originalEncoder != null) {
//                return new XorEncryptingVideoEncoder(originalEncoder, xorKey);
//            }
//            return null;
//        }
//
//        @Override
//        public VideoCodecInfo[] getSupportedCodecs() {
//            return originalFactory.getSupportedCodecs();
//        }
//    }
//
//    /**
//     * 解密视频解码器工厂
//     */
//    public class XorEncryptedVideoDecoderFactory implements VideoDecoderFactory {
//        private final VideoDecoderFactory originalFactory;
//        private final byte xorKey;
//
//        public XorEncryptedVideoDecoderFactory(VideoDecoderFactory originalFactory, byte xorKey) {
//            this.originalFactory = originalFactory;
//            this.xorKey = xorKey;
//        }
//
//        @Override
//        public VideoDecoder createDecoder(VideoCodecInfo codecInfo) {
//            VideoDecoder originalDecoder = originalFactory.createDecoder(codecInfo);
//            if (originalDecoder != null) {
//                return new XorDecryptingVideoDecoder(originalDecoder, xorKey);
//            }
//            return null;
//        }
//
//        @Override
//        public VideoCodecInfo[] getSupportedCodecs() {
//            return originalFactory.getSupportedCodecs();
//        }
//    }

    private void initWebRTC() {
        eglBase = EglBase.create();

        Log.d(TAG, "WebRTC version info:");

//        // 方法1：获取 PeerConnectionFactory 类的版本信息
//        String webrtcVersion = "";
//        try {
//            Class<?> rtcVersionClass = Class.forName("org.webrtc.PeerConnectionFactory");
//            java.lang.reflect.Method getVersionMethod = rtcVersionClass.getMethod("nativeGetVersion");
//            webrtcVersion = (String) getVersionMethod.invoke(null);
//            Log.d(TAG, "WebRTC native version: " + webrtcVersion);
//        } catch (Exception e) {
//            Log.e(TAG, "Failed to get WebRTC version via reflection: " + e.getMessage());
//        }

//        // 2. 初始化 PeerConnectionFactory
//        PeerConnectionFactory.InitializationOptions initOptions =
//                PeerConnectionFactory.InitializationOptions.builder(getApplicationContext())
//                        .createInitializationOptions();
//        PeerConnectionFactory.initialize(initOptions);
//
//        // 3. 创建默认的编解码器工厂
//        DefaultVideoEncoderFactory defaultEncoderFactory =
//                new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);
//        DefaultVideoDecoderFactory defaultDecoderFactory =
//                new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
//
//        // 4. 用加密包装器包装默认工厂
//        byte xorKey = 0x55; // 简单的异或密钥
//        VideoEncoderFactory encryptedEncoderFactory =
//                new XorEncryptedVideoEncoderFactory(defaultEncoderFactory, xorKey);
//        VideoDecoderFactory encryptedDecoderFactory =
//                new XorEncryptedVideoDecoderFactory(defaultDecoderFactory, xorKey);
//        VideoCodecInfo encryptedSupportedCodecs = encryptedEncoderFactory.getSupportedCodecs()[0];
////        Log.e(TAG, "Encrypted factory supports " + encryptedSupportedCodecs.length + " codecs:");
//        VideoEncoder encoder = encryptedEncoderFactory.createEncoder(encryptedSupportedCodecs);
//        if (encoder != null) {
//            Log.d(TAG, "Successfully created encoder: " + encoder.getImplementationName());
//            // 尝试初始化编码器以触发回调
//            encoder.initEncode(new VideoEncoder.Settings(1, 1280, 720,2000,  30, 1, true),
//                    new VideoEncoder.Callback() {
//                        @Override
//                        public void onEncodedFrame(EncodedImage encodedImage, VideoEncoder.CodecSpecificInfo info) {
//                            Log.d(TAG, "Manual test - onEncodedFrame called");
//                        }
//                    });
//        } else {
//            Log.e(TAG, "Failed to create encoder for codec: " + encryptedSupportedCodecs.name);
//        }
//        // 5. 使用加密工厂创建 PeerConnectionFactory
//        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
//        peerConnectionFactory = PeerConnectionFactory.builder()
//                .setOptions(options)
//                .setVideoEncoderFactory(encryptedEncoderFactory)
//                .setVideoDecoderFactory(defaultDecoderFactory)
//                .createPeerConnectionFactory();

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(null)
                .createPeerConnectionFactory();

        // Initialize video views
        localVideoView.init(eglBase.getEglBaseContext(), null);
        localVideoView.setMirror(true);
        remoteVideoView.init(eglBase.getEglBaseContext(), null);
        remoteVideoView.setMirror(false);

        // Create video capturer
        if (!isOnlyAudio) {
            createVideoCapturer();
        }

        // Create audio track
        createAudioTrack();

        if (!isOnlyAudio) {
            createVideoTrack();
        }
    }

    private void createVideoCapturer() {
        videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
    }


    private void createVideoTrack() {
        if (videoCapturer != null) {
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
            VideoSource videoSource = peerConnectionFactory.createVideoSource(false);
            videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
            videoCapturer.startCapture(1280, 720, 30);  // 修改为1280x720分辨率，30fps

            localVideoTrack = peerConnectionFactory.createVideoTrack("video_track", videoSource);
            localVideoTrack.addSink(localVideoView);
        }
    }

    private void createAudioTrack() {
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));

        localAudioTrack = peerConnectionFactory.createAudioTrack(
                "audio_track",
                peerConnectionFactory.createAudioSource(audioConstraints));
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private void initMediasoup() {

        roomClient.setMediasoupClientListener(new RoomClient.MediasoupClientListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    updateStatus("已连接到服务器");
                    startLocalMedia();
                });
            }

            @Override
            public void onConnectionFailed(String error) {
                runOnUiThread(() -> {
                    updateStatus("连接失败: " + error);
                    showToast("连接失败");
                });
            }

            @Override
            public void onRemoteTrackAdded(VideoTrack remoteVideoTrack, AudioTrack audioTrack) {
                runOnUiThread(() -> {
                    Log.d(TAG,"onRemoteTrackAdded");
//                    if (videoTrack != null) {
//                        Log.d("RoomClient", "更新");
//                        remoteVideoView.setVisibility(View.VISIBLE);
//                        videoTrack.addSink(remoteVideoView);
//                    }
                    if (remoteVideoTrack != null) {
                        remoteVideoTrack.setEnabled(true); // 确保轨道启用
//                        remoteVideoTrack.addSink(new VideoSink() {
//                            @Override
//                            public void onFrame(VideoFrame frame) {
//                                Log.d("RoomClient", "Remote video frame received: " + frame.getRotatedWidth() + "x" + frame.getRotatedHeight());
//                            }
//                        });
                        remoteVideoTrack.addSink(remoteVideoView);
                    } else {
                        Log.e("RoomClient", "remoteVideoTrack is null.");
                    }
                    if(audioTrack != null){
                        audioTrack.setEnabled(true);
                    }
                    if (!isCallConnected) {
                        isCallConnected = true;
                        startCallTimer();
                    }
                });

            }

            @Override
            public void onParticipantLeft(String participantId) {
                runOnUiThread(() -> {
                    if (participantId.equals(userId)) {
                        showToast("对方已离开");
                        finish();
                    }
                });
            }
        });
    }

    private void startCall() {
        updateStatus("正在连接...");
        roomClient.connect();
//        mediasoupClient.connect("wss://your-mediasoup-server:3000");
    }

    private void startLocalMedia() {
        if (!isOnlyAudio && localVideoTrack != null) {
            roomClient.addTrack(localVideoTrack);
        }
        if (localAudioTrack != null) {
            roomClient.addTrack(localAudioTrack);
        }
    }

    private void toggleAudio() {
        isAudioEnabled = !isAudioEnabled;
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(isAudioEnabled);
        }
        toggleAudioButton.setImageResource(isAudioEnabled ?
                R.drawable.ic_mic_on : R.drawable.ic_mic_off);
    }

    private void toggleVideo() {
        if (isOnlyAudio) return;

        isVideoEnabled = !isVideoEnabled;
        if (localVideoTrack != null) {
            localVideoTrack.setEnabled(isVideoEnabled);
        }
        toggleVideoButton.setImageResource(isVideoEnabled ?
                R.drawable.ic_video_on : R.drawable.ic_video_off);
    }

    private void hangup() {
        if (roomClient != null) {
            roomClient.close();
            roomClient = null;
        }
        finish();
    }

    private void startCallTimer() {
        callStartTime = System.currentTimeMillis();
        timerHandler = new Handler(Looper.getMainLooper());
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateCallDuration();
                timerHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void updateCallDuration() {
        long duration = System.currentTimeMillis() - callStartTime;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(duration);

        String durationText = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        timerTextView.setText(durationText);
    }

    private void updateStatus(String status) {
        statusTextView.setText(status);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                PERMISSION_REQUEST_CODE);
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                initializeCall();
            } else {
                showToast("需要相机和麦克风权限才能进行视频通话");
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (timerHandler != null) {
            timerHandler.removeCallbacksAndMessages(null);
        }

        if (localVideoTrack != null) {
            localVideoTrack.dispose();
        }
        if (localAudioTrack != null) {
            localAudioTrack.dispose();
        }
        if (videoCapturer != null) {
            videoCapturer.dispose();
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
        }

        if (localVideoView != null) {
            localVideoView.release();
        }
        if (remoteVideoView != null) {
            remoteVideoView.release();
        }

        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
        }

        if (roomClient != null) {
            roomClient.close();
        }

        super.onDestroy();
    }
}