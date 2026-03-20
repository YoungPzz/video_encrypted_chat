package com.example.android_bysj_demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private FrameLayout remoteVideosContainer;
    private FrameLayout localVideoPipContainer;
    private SurfaceViewRenderer localVideoView;
    private TextView localUserNameView;
    private ImageButton toggleAudioButton;
    private ImageButton toggleVideoButton;
    private ImageButton hangupButton;
    private TextView statusTextView;
    private TextView timerTextView;

    // 用于管理多用户的轨道和视图映射
    private Map<String, UserMediaInfo> userMediaMap = new HashMap<>();
    private List<SurfaceViewRenderer> remoteVideoViews = new ArrayList<>();
    
    // 动态网格容器
    private GridLayout gridLayout;

    // 用户媒体信息类
    private static class UserMediaInfo {
        VideoTrack videoTrack;
        AudioTrack audioTrack;
        SurfaceViewRenderer videoView;
        FrameLayout videoContainer;
        TextView userNameTextView;
        String participantId;
        String userName;
    }

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
    private boolean useSM4Encryption = true;
    private boolean openEncryption = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);

        // Get intent extras
        roomId = getIntent().getStringExtra("ROOM_ID");
        userId = getIntent().getStringExtra("USER_ID");
        useSM4Encryption = getIntent().getBooleanExtra("USE_SM4_ENCRYPTION", true);
        openEncryption = getIntent().getBooleanExtra("OPEN_ENCRYPTION", true);
        isOnlyAudio = getIntent().getBooleanExtra("isOnlyAudio", false);

        initViews();
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            initializeCall();
        }
    }

    private void initializeCall() {
        roomClient = new RoomClient(this, userId, roomId);
        initWebRTC();
        initMediasoup();
        startCall();
    }

    private void initViews() {
        remoteVideosContainer = (FrameLayout) findViewById(R.id.remote_videos_container);
        localVideoPipContainer = (FrameLayout) findViewById(R.id.local_video_pip_container);
        localVideoView = (SurfaceViewRenderer) findViewById(R.id.local_video_view);
        localUserNameView = (TextView) findViewById(R.id.local_user_name);
        toggleAudioButton = (ImageButton) findViewById(R.id.toggle_audio_button);
        toggleVideoButton = (ImageButton) findViewById(R.id.toggle_video_button);
        hangupButton = (ImageButton) findViewById(R.id.hangup_button);
        statusTextView = (TextView) findViewById(R.id.status_text_view);
        timerTextView = (TextView) findViewById(R.id.timer_text_view);

        setupClickListeners();
    }

    private void setupClickListeners() {
        toggleAudioButton.setOnClickListener(v -> toggleAudio());
        toggleVideoButton.setOnClickListener(v -> toggleVideo());
        hangupButton.setOnClickListener(v -> hangup());
    }

    /**
     * 创建新的视频视图容器
     */
    private FrameLayout createVideoViewContainer(String participantId, String userName) {
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.parseColor("#1A1A2E"));

        // 创建 SurfaceViewRenderer
        SurfaceViewRenderer videoView = new SurfaceViewRenderer(this);
        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        videoView.setLayoutParams(videoParams);
        
        // 初始化视频视图
        videoView.init(eglBase.getEglBaseContext(), null);
        videoView.setMirror(false);
        videoView.setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        videoView.setEnableHardwareScaler(true);
        
        container.addView(videoView);

        // 创建用户名标签
        TextView userNameLabel = new TextView(this);
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.gravity = Gravity.BOTTOM | Gravity.START;
        labelParams.setMargins(16, 16, 16, 16);
        userNameLabel.setLayoutParams(labelParams);
        userNameLabel.setText(userName);
        userNameLabel.setTextColor(Color.WHITE);
        userNameLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        userNameLabel.setBackgroundColor(Color.parseColor("#80000000"));
        userNameLabel.setPadding(16, 8, 16, 8);
        
        container.addView(userNameLabel);

        remoteVideoViews.add(videoView);
        return container;
    }

    /**
     * 创建本地视频视图容器（用于网格模式）
     */
    private FrameLayout createLocalVideoContainer() {
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.parseColor("#1A1A2E"));

        // 创建新的本地视频视图（用于网格中显示）
        SurfaceViewRenderer localViewForGrid = new SurfaceViewRenderer(this);
        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        localViewForGrid.setLayoutParams(videoParams);
        
        localViewForGrid.init(eglBase.getEglBaseContext(), null);
        localViewForGrid.setMirror(true);
        localViewForGrid.setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        localViewForGrid.setEnableHardwareScaler(true);
        
        container.addView(localViewForGrid);

        // 创建用户名标签
        TextView userNameLabel = new TextView(this);
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.gravity = Gravity.BOTTOM | Gravity.START;
        labelParams.setMargins(16, 16, 16, 16);
        userNameLabel.setLayoutParams(labelParams);
        userNameLabel.setText("我");
        userNameLabel.setTextColor(Color.WHITE);
        userNameLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        userNameLabel.setBackgroundColor(Color.parseColor("#80000000"));
        userNameLabel.setPadding(16, 8, 16, 8);
        
        container.addView(userNameLabel);

        // 将本地视频轨道绑定到这个新视图
        if (localVideoTrack != null) {
            localVideoTrack.addSink(localViewForGrid);
        }

        return container;
    }

    /**
     * 更新整体视频布局
     * 总人数 = 远程用户数 + 本地自己
     */
    private void updateVideoLayout() {
        int remoteUserCount = userMediaMap.size();
        int totalUserCount = remoteUserCount + 1; // +1 是本地用户
        
        // 清空远程视频容器
        remoteVideosContainer.removeAllViews();
        if (gridLayout != null) {
            gridLayout.removeAllViews();
        }
        
        if (totalUserCount == 2) {
            // 2人：远程全屏 + 本地右上角小画中画
            setupPipLayout(remoteUserCount);
        } else if (totalUserCount == 3) {
            // 3人：远程上下分屏 + 本地右上角小画中画
            setupPipLayout(remoteUserCount);
        } else {
            // 4人+：所有人网格排列
            setupGridLayout(totalUserCount);
        }
    }

    /**
     * 画中画布局（2-3人）
     */
    private void setupPipLayout(int remoteUserCount) {
        // 显示本地小窗
        localVideoPipContainer.setVisibility(View.VISIBLE);
        
        // 创建 GridLayout 来放置远程用户
        gridLayout = new GridLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        gridLayout.setLayoutParams(params);
        gridLayout.setUseDefaultMargins(false);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        gridLayout.setPadding(4, 4, 4, 4);
        
        if (remoteUserCount == 1) {
            // 单个远程用户 - 全屏
            gridLayout.setColumnCount(1);
            gridLayout.setRowCount(1);
        } else if (remoteUserCount == 2) {
            // 两个远程用户 - 上下分屏
            gridLayout.setColumnCount(1);
            gridLayout.setRowCount(2);
        }
        
        // 添加远程用户视频
        for (UserMediaInfo userInfo : userMediaMap.values()) {
            FrameLayout container = userInfo.videoContainer;
            if (container != null && container.getParent() != null) {
                ((FrameLayout) container.getParent()).removeView(container);
            }
            
            if (container != null) {
                GridLayout.LayoutParams gridParams = new GridLayout.LayoutParams();
                gridParams.width = 0;
                gridParams.height = 0;
                gridParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
                gridParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
                gridParams.setMargins(4, 4, 4, 4);
                container.setLayoutParams(gridParams);
                gridLayout.addView(container);
            }
        }
        
        remoteVideosContainer.addView(gridLayout);
    }

    /**
     * 网格布局（4人及以上）
     */
    private void setupGridLayout(int totalUserCount) {
        // 隐藏本地小窗（本地视频将加入网格）
        localVideoPipContainer.setVisibility(View.GONE);
        
        // 创建 GridLayout
        gridLayout = new GridLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        gridLayout.setLayoutParams(params);
        gridLayout.setUseDefaultMargins(false);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        gridLayout.setPadding(4, 4, 4, 4);
        
        // 计算行列数
        int columns, rows;
        if (totalUserCount <= 4) {
            columns = 2;
            rows = 2;
        } else if (totalUserCount <= 6) {
            columns = 2;
            rows = 3;
        } else if (totalUserCount <= 9) {
            columns = 3;
            rows = 3;
        } else {
            columns = 4;
            rows = (totalUserCount + 3) / 4;
        }
        
        gridLayout.setColumnCount(columns);
        gridLayout.setRowCount(rows);
        
        // 首先添加本地视频到网格
        FrameLayout localContainer = createLocalVideoContainer();
        GridLayout.LayoutParams localParams = new GridLayout.LayoutParams();
        localParams.width = 0;
        localParams.height = 0;
        localParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        localParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        localParams.setMargins(4, 4, 4, 4);
        localContainer.setLayoutParams(localParams);
        gridLayout.addView(localContainer);
        
        // 添加远程用户视频
        for (UserMediaInfo userInfo : userMediaMap.values()) {
            FrameLayout container = userInfo.videoContainer;
            if (container != null && container.getParent() != null) {
                ((FrameLayout) container.getParent()).removeView(container);
            }
            
            if (container != null) {
                GridLayout.LayoutParams gridParams = new GridLayout.LayoutParams();
                gridParams.width = 0;
                gridParams.height = 0;
                gridParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
                gridParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
                gridParams.setMargins(4, 4, 4, 4);
                container.setLayoutParams(gridParams);
                gridLayout.addView(container);
            }
        }
        
        remoteVideosContainer.addView(gridLayout);
    }

    /**
     * 移除视频视图容器
     */
    private void removeVideoViewContainer(FrameLayout container, SurfaceViewRenderer videoView) {
        if (videoView != null) {
            videoView.clearImage();
            videoView.release();
            remoteVideoViews.remove(videoView);
        }
        
        if (container != null && container.getParent() != null) {
            ((FrameLayout) container.getParent()).removeView(container);
        }
        
        // 更新布局
        updateVideoLayout();
    }

    private void initWebRTC() {
        eglBase = EglBase.create();

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(null)
                .createPeerConnectionFactory();

        // Initialize local video view (PIP mode)
        localVideoView.init(eglBase.getEglBaseContext(), null);
        localVideoView.setMirror(true);
        localVideoView.setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        // 设置 Z-order 让本地小窗始终在远程视频之上
        localVideoView.setZOrderMediaOverlay(true);

        // Create video capturer
        if (!isOnlyAudio) {
            createVideoCapturer();
        }

        // Create audio track
        createAudioTrack();

        // Create video track
        if (!isOnlyAudio) {
            createVideoTrack();
        }

        // 初始显示本地小窗
        localVideoPipContainer.setVisibility(View.VISIBLE);
    }

    private void createVideoCapturer() {
        videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
    }

    private void createVideoTrack() {
        if (videoCapturer != null) {
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
            VideoSource videoSource = peerConnectionFactory.createVideoSource(false);
            videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
            videoCapturer.startCapture(1280, 720, 30);

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
                VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) {
                    return capturer;
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) {
                    return capturer;
                }
            }
        }

        return null;
    }

    private void initMediasoup() {
        roomClient.setSM4Encryption(useSM4Encryption);
        roomClient.setOpenEncryption(openEncryption);

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
            public void onRemoteTrackAdded(String participantId, String userName, VideoTrack remoteVideoTrack, AudioTrack audioTrack) {
                runOnUiThread(() -> {
                    Log.d(TAG, "onRemoteTrackAdded: 用户=" + userName + ", participantId=" + participantId);

                    // 获取或创建用户媒体信息
                    UserMediaInfo userInfo = userMediaMap.get(participantId);
                    if (userInfo == null) {
                        userInfo = new UserMediaInfo();
                        userInfo.participantId = participantId;
                        userInfo.userName = userName;
                        userMediaMap.put(participantId, userInfo);
                    }

                    // 处理视频轨道
                    if (remoteVideoTrack != null) {
                        remoteVideoTrack.setEnabled(true);
                        userInfo.videoTrack = remoteVideoTrack;

                        // 动态创建视频视图
                        if (userInfo.videoContainer == null) {
                            userInfo.videoContainer = createVideoViewContainer(participantId, userName);
                            // 获取刚创建的 SurfaceViewRenderer
                            userInfo.videoView = (SurfaceViewRenderer) userInfo.videoContainer.getChildAt(0);
                            userInfo.userNameTextView = (TextView) userInfo.videoContainer.getChildAt(1);
                        }

                        if (userInfo.videoView != null) {
                            remoteVideoTrack.addSink(userInfo.videoView);
                            Log.d(TAG, "用户 " + userName + " 的视频已添加到视图");
                        }
                    }

                    // 处理音频轨道
                    if (audioTrack != null) {
                        audioTrack.setEnabled(true);
                        userInfo.audioTrack = audioTrack;
                        Log.d(TAG, "用户 " + userName + " 的音频已启用");
                    }

                    // 更新整体布局
                    updateVideoLayout();

                    // 启动通话计时器
                    if (!isCallConnected) {
                        isCallConnected = true;
                        startCallTimer();
                    }
                });
            }

            @Override
            public void onParticipantLeft(String participantId) {
                runOnUiThread(() -> {
                    Log.d(TAG, "用户离开: " + participantId);

                    UserMediaInfo userInfo = userMediaMap.get(participantId);
                    if (userInfo != null) {
                        String userName = userInfo.userName;

                        // 清理视频轨道
                        if (userInfo.videoTrack != null && userInfo.videoView != null) {
                            userInfo.videoTrack.removeSink(userInfo.videoView);
                            userInfo.videoTrack = null;
                        }

                        // 清理音频轨道
                        if (userInfo.audioTrack != null) {
                            userInfo.audioTrack.setEnabled(false);
                            userInfo.audioTrack = null;
                        }

                        // 移除视频视图容器
                        removeVideoViewContainer(userInfo.videoContainer, userInfo.videoView);

                        // 从映射中移除
                        userMediaMap.remove(participantId);

                        showToast("用户 " + userName + " 已离开");
                    }
                });
            }
        });
    }

    private void startCall() {
        updateStatus("正在连接...");
        roomClient.connect();
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

        if(KNSConnectManager.getInstance().isConnected()){
            KNSConnectManager.getInstance().disconnect();
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
        statusTextView.setVisibility(View.VISIBLE);
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

        // 清理所有远程视频视图
        for (SurfaceViewRenderer videoView : remoteVideoViews) {
            if (videoView != null) {
                videoView.release();
            }
        }
        remoteVideoViews.clear();

        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
        }

        if (roomClient != null) {
            roomClient.close();
        }

        super.onDestroy();
    }
}
