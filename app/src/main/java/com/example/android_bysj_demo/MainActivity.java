package com.example.android_bysj_demo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText etUserId;
    private EditText etRoomId;
    private Button btnJump;
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
            Intent intent = new Intent(MainActivity.this, RoomCreationActivity.class);

            intent.putExtra("USER_ID", userId);
            intent.putExtra("ROOM_ID", roomId);
            startActivity(intent);
        });
    }

    private void initView() {
        // 绑定控件
        etUserId = findViewById(R.id.et_user_id);
        etRoomId = findViewById(R.id.et_room_id);
        btnJump = findViewById(R.id.btn_jump);
    }
}