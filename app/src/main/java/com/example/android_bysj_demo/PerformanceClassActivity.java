package com.example.android_bysj_demo;

import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.performance.DefaultDevicePerformance;

public class PerformanceClassActivity extends AppCompatActivity {
    private static final String TAG = "PerformanceClassActivity";

    // 性能等级常量（对应 Android 版本）
    private static final int PERFCLASS_11 = Build.VERSION_CODES.R;  // Android 11
    private static final int PERFCLASS_12 = Build.VERSION_CODES.S;  // Android 12
    private static final int PERFCLASS_13 = Build.VERSION_CODES.TIRAMISU;  // Android 13
    private static final int PERFCLASS_NONE = 0;

    private Button btnCheckPerformance;
    private TextView tvPerformanceResult;
    private DefaultDevicePerformance devicePerf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance_class);
        devicePerf = new DefaultDevicePerformance();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnCheckPerformance = findViewById(R.id.btn_check_performance);
        tvPerformanceResult = findViewById(R.id.tv_performance_result);
    }

    private void setupClickListeners() {
        btnCheckPerformance.setOnClickListener(v -> checkDevicePerformance());
    }

    /**
     * 检测设备媒体性能等级
     */
    private void checkDevicePerformance() {
        int mpcLevel = devicePerf.getMediaPerformanceClass();

        StringBuilder result = new StringBuilder();
        result.append("=== 设备性能检测结果 ===\n\n");
        result.append("媒体性能等级 (MPC): ").append(mpcLevel).append("\n\n");

        // 解释性能等级
        result.append("性能等级说明:\n");
        if (mpcLevel >= PERFCLASS_13) {
            result.append("• MPC >= 33 (Android 13+)\n");
            result.append("  - 支持 720p @ 60fps 视频编码\n");
            result.append("  - 支持硬件视频解码加速\n");
            result.append("  - 高端设备");
        } else if (mpcLevel == PERFCLASS_12) {
            result.append("• MPC = 31 (Android 12)\n");
            result.append("  - 支持 720p @ 30fps 视频编码\n");
            result.append("  - 支持基础硬件解码\n");
            result.append("  - 中端设备");
        } else if (mpcLevel == PERFCLASS_11) {
            result.append("• MPC = 30 (Android 11)\n");
            result.append("  - 基础视频编码能力\n");
            result.append("  - 入门级设备");
        } else if (mpcLevel == PERFCLASS_NONE) {
            result.append("• MPC = 0 (不支持)\n");
            result.append("  - 运行在 Android 11 以下版本\n");
            result.append("  - 或设备不在性能库列表中\n");
            result.append("  - 建议使用较低的视频质量");
        }

        result.append("\n设备信息:\n");
        result.append("• 品牌: ").append(Build.BRAND).append("\n");
        result.append("• 型号: ").append(Build.MODEL).append("\n");
        result.append("• 设备: ").append(Build.DEVICE).append("\n");
        result.append("• 产品: ").append(Build.PRODUCT).append("\n");
        result.append("• Android 版本: ").append(Build.VERSION.RELEASE).append("\n");
        result.append("• API Level: ").append(Build.VERSION.SDK_INT);

        result.append("\n\n检测方式:\n");

        // 显示检测方式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int systemMPC = Build.VERSION.MEDIA_PERFORMANCE_CLASS;
            if (systemMPC >= PERFCLASS_11) {
                result.append("• 使用系统属性检测\n");
                result.append("  Build.VERSION.MEDIA_PERFORMANCE_CLASS = ").append(systemMPC);
            } else {
                result.append("• 系统属性未声明性能等级\n");
                result.append("  使用设备指纹库进行匹配");
            }
        } else {
            result.append("• 使用设备指纹库进行匹配\n");
            result.append("  系统 API < 31，不支持原生性能查询");
        }

        // 显示设备指纹
        String fingerprint = Build.BRAND + "/" + Build.PRODUCT + "/" + Build.DEVICE + ":" + Build.VERSION.RELEASE;
        result.append("\n\n设备指纹:\n");
        result.append("  ").append(fingerprint);
        result.append("\n\n注: 该指纹用于与已知设备库匹配");

        tvPerformanceResult.setText(result.toString());
    }
}
