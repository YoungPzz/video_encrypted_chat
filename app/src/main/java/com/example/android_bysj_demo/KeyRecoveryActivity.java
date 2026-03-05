package com.example.android_bysj_demo;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class KeyRecoveryActivity extends AppCompatActivity {

    private ImageView ivRotatingIcon;
    private TextView tvRecoveryMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_recovery);

        initViews();
        startRotationAnimation();
    }

    private void initViews() {
        ivRotatingIcon = findViewById(R.id.iv_rotating_icon);
        tvRecoveryMessage = findViewById(R.id.tv_recovery_message);

        tvRecoveryMessage.setText("有成员退出，重新恢复密钥中...");
    }

    private void startRotationAnimation() {
        // 加载旋转动画
        Animation rotationAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_animation);
        ivRotatingIcon.startAnimation(rotationAnimation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ivRotatingIcon != null) {
            ivRotatingIcon.clearAnimation();
        }
    }
}
