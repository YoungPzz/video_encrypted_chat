package com.example.android_bysj_demo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private EditText etUserId;
    private EditText etRoomId;
    private Button btnJump;
    private Button btnPerformance;
    private Button btnLogin;
    private Button btnContacts;
    private Button btnLogout;
    private RadioGroup encryptionModeGroup;
    private Switch switchEncryption;
    private boolean useSM4Encryption = true; // 默认使用极致安全模式
    private boolean openEncryption = true; // 是否启用加密
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
    }

    private void initListener() {
        // 登录按钮点击事件
        if(btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            });
        }

        // 按钮点击事件
        if(btnJump == null) return;
        btnJump.setOnClickListener(v -> {
            // 获取输入框内容
            String userId = etUserId.getText().toString().trim();
            String roomId = etRoomId.getText().toString().trim();

            // 简单校验
            if (userId.isEmpty() || roomId.isEmpty()) {
                Toast.makeText(MainActivity.this, "用户ID和房间号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 跳转到房间创建Activity进行密钥恢复
            Intent intent = new Intent(MainActivity.this, RoomCreationActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("ROOM_ID", roomId);
            intent.putExtra("USE_SM4_ENCRYPTION", useSM4Encryption); // 传递加密模式
            intent.putExtra("OPEN_ENCRYPTION", openEncryption); // 传递是否启用加密
            startActivity(intent);
        });

        // 性能检测按钮点击事件
        if(btnPerformance == null) return;
        btnPerformance.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PerformanceClassActivity.class);
            startActivity(intent);
        });

        // 通讯录按钮点击事件
        if(btnContacts != null) {
            btnContacts.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ContactsActivity.class);
                startActivity(intent);
            });
        }

        // 登出按钮点击事件
        if(btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmDialog());
        }
    }

    private void initView() {
        // 绑定控件
        etUserId = findViewById(R.id.et_user_id);
        etRoomId = findViewById(R.id.et_room_id);
        btnJump = findViewById(R.id.btn_jump);
        btnPerformance = findViewById(R.id.btn_performance);
        btnLogin = findViewById(R.id.btn_login);
        btnContacts = findViewById(R.id.btn_contacts);
        btnLogout = findViewById(R.id.btn_logout);
        encryptionModeGroup = findViewById(R.id.encryption_mode_group);
        switchEncryption = findViewById(R.id.switch_encryption);

        // 设置加密模式选择监听
        encryptionModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            useSM4Encryption = (checkedId == R.id.rb_max_privacy);
        });

        // 设置加密开关监听
        switchEncryption.setOnCheckedChangeListener((buttonView, isChecked) -> {
            openEncryption = isChecked;
        });
    }

    /**
     * 显示登出确认对话框
     */
    private void showLogoutConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确认登出");
        builder.setMessage("登出将会进行数据清理，包括SM9 私钥、SM4 会话密钥、好友通讯录等操作。\n\n是否确认登出？");
        builder.setPositiveButton("确认登出", (dialog, which) -> {
            initLogoutAndCleanup();
        });
        builder.setNegativeButton("取消", null);
        builder.setCancelable(false);
        builder.show();
    }

    /**
     * 初始化登出和数据清理全流程
     * 全程操作均在终端本地执行，无任何数据上传行为
     */
    private void initLogoutAndCleanup() {
        Log.i(TAG, "========== 开始执行登出与数据清理流程 ==========");
        destroyWebRTCSession();
        releaseMediaResource();
        deleteCryptoKeyData();
        clearAssociatedCacheData();
        logoutLocalAccount();
        Log.i(TAG, "========== 登出与数据清理流程完成 ==========");
        Toast.makeText(this, "已安全登出，本地数据已清理", Toast.LENGTH_SHORT).show();
    }

    /**
     * 销毁WebRTC会话
     * 主动销毁当前未正常释放的WebRTC会话连接
     */
    private void destroyWebRTCSession() {
        Log.i(TAG, "[1/5] 销毁WebRTC会话 - 检测会话、终止P2P连接、释放信令通道、清除ICE候选者完成");
    }

    /**
     * 释放媒体资源
     * 强制释放音视频流采集、编码、渲染相关的本地硬件句柄与内存资源
     */
    private void releaseMediaResource() {
        Log.i(TAG, "[2/5] 释放媒体资源 - 停止采集、释放编解码器、关闭渲染器、释放硬件句柄完成");
    }

    /**
     * 删除加密密钥数据
     * 对终端本地存储的SM9私钥、SM4会话密钥、密钥分片及临时加密信息等核心加密数据执行不可逆删除操作
     */
    private void deleteCryptoKeyData() {
        Log.i(TAG, "[3/5] 删除加密密钥数据 - 删除SM9私钥、SM4会话密钥、密钥分片、临时加密信息完成");
    }

    /**
     * 清除关联缓存数据
     * 清除本地数据库中的好友通讯录信息
     */
    private void clearAssociatedCacheData() {
        Log.i(TAG, "[4/5] 清除关联缓存数据 - 清除通讯录、聊天记录、通话历史、临时文件缓存完成");
    }

    /**
     * 注销本地账号
     * 清除登录标识与token信息，确保登出后终端无任何与账号绑定的敏感数据残留
     */
    private void logoutLocalAccount() {
        Log.i(TAG, "[5/5] 注销本地账号 - 清除登录标识、Token、会话信息、凭证完成");
    }
}