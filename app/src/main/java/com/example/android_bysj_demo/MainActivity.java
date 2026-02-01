package com.example.android_bysj_demo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText etUserId;
    private EditText etRoomId;
    private Button btnJump;
    private Button btnPerformance;
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

            // 携带数据跳转到密钥协商Activity
//            Intent intent = new Intent(MainActivity.this, KeyNegotiationActivity.class);
//            Intent intent = new Intent(MainActivity.this, RoomCreationActivity.class);
            Intent intent = new Intent(MainActivity.this, VideoChatActivity.class);
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
    }

    private void initView() {
        // 绑定控件
        etUserId = findViewById(R.id.et_user_id);
        etRoomId = findViewById(R.id.et_room_id);
        btnJump = findViewById(R.id.btn_jump);
        btnPerformance = findViewById(R.id.btn_performance);
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
}