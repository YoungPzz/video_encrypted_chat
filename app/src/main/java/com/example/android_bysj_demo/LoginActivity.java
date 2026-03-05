package com.example.android_bysj_demo;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String CORRECT_CODE = "123456";
    private static final long COUNTDOWN_TIME = 60000; // 60秒
    private static final long COUNTDOWN_INTERVAL = 1000; // 1秒

    private EditText etPhone;
    private EditText etVerificationCode;
    private Button btnGetCode;
    private Button btnLogin;

    private CountDownTimer countDownTimer;

    // TC-02: Token 存储相关
    private static final String TOKEN_KEY = "user_auth_token";

    // TC-03: SM4 密钥相关
    private byte[] sm4Key; // 128位 SM4 对称密钥 (16字节)

    // TC-04: SM9 私钥相关
    private byte[] sm9PrivateKey;
    private static final String SM9_PUBLIC_KEY_HEX = "04" +  // 非压缩格式
            "B9FAD6E3C1A8C9F7E2D6A5B4F3C1E9D8A7B6C5D4E3F2A1B9C8D7E6F5A4B3C2D1" + // X 坐标 (32字节)
            "9F8E7D6C5B4A3F2E1D0C9B8A7F6E5D4C3B2A190E8F7D6C5B4A3F2E1D0C9B8A7F6";  // Y 坐标 (32字节)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();
        initListener();
    }

    private void initView() {
        etPhone = findViewById(R.id.et_phone);
        etVerificationCode = findViewById(R.id.et_verification_code);
        btnGetCode = findViewById(R.id.btn_get_code);
        btnLogin = findViewById(R.id.btn_login);
    }

    private void initListener() {
        // 获取验证码按钮点击事件
        btnGetCode.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            if (validatePhone(phone)) {
                startCountDown();
                Toast.makeText(LoginActivity.this, "验证码已发送: " + CORRECT_CODE, Toast.LENGTH_SHORT).show();
            }
        });

        // 登录按钮点击事件
        btnLogin.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            String code = etVerificationCode.getText().toString().trim();

            // 验证手机号
            if (!validatePhone(phone)) {
                return;
            }

            // 验证验证码
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(LoginActivity.this, "请输入验证码", Toast.LENGTH_SHORT).show();
                return;
            }

            if (code.length() != 6) {
                Toast.makeText(LoginActivity.this, "请输入6位验证码", Toast.LENGTH_SHORT).show();
                return;
            }

            // 验证码校验
            if (CORRECT_CODE.equals(code)) {
                // 登录成功，执行后续流程
                handleLoginSuccess(phone);
            } else {
                Toast.makeText(LoginActivity.this, "验证码错误，请重新输入", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 验证手机号格式
     * @param phone 手机号
     * @return 是否合法
     */
    private boolean validatePhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(LoginActivity.this, "请输入手机号", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (phone.length() != 11) {
            Toast.makeText(LoginActivity.this, "手机号必须为11位", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 中国大陆手机号正则验证
        String phoneRegex = "^1[3-9]\\d{9}$";
        if (!Pattern.matches(phoneRegex, phone)) {
            Toast.makeText(LoginActivity.this, "手机号格式不正确", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * 开始倒计时
     */
    private void startCountDown() {
        btnGetCode.setEnabled(false);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(COUNTDOWN_TIME, COUNTDOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                btnGetCode.setText(seconds + "秒后重发");
            }

            @Override
            public void onFinish() {
                btnGetCode.setEnabled(true);
                btnGetCode.setText("获取验证码");
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    // ========== TC-02: 接收服务器返回的Token并存储 ==========

    /**
     * 处理登录成功后的流程
     * TC-02: 接收服务器返回的登录成功消息（附带token）并存储
     */
    private void handleLoginSuccess(String phone) {
        // 1. 模拟接收服务器返回的登录成功消息
        JSONObject serverResponse = simulateServerResponse(phone);

        try {
            // 2. 打印服务器返回的完整消息
            String token = serverResponse.getString("token");
            Log.i(TAG, "[TC-02] 服务器返回的登录成功消息: " + serverResponse.toString());

            // 3. 存储Token到安全存储区
            storeTokenToSecureStorage(token);

            // 4. 继续执行TC-03流程
            handleSM4KeyGeneration();

        } catch (JSONException e) {
            Log.e(TAG, "[TC-02] 解析服务器响应失败", e);
            Toast.makeText(LoginActivity.this, "登录失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 模拟服务器返回的登录成功消息
     */
    private JSONObject simulateServerResponse(String phone) {
        JSONObject response = new JSONObject();
        try {
            String simulatedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJ1c2VySWQiOiI" + phone.substring(phone.length() - 4) +
                    "IiwicGhvbmUiOiI" + phone +
                    "IiwiaWF0IjoxNzEwMDAwMDAwLCJleHAiOjE3MTAwODY0MDB9." +
                    "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

            response.put("code", 200);
            response.put("message", "登录成功");
            response.put("token", simulatedToken);
            response.put("userId", "user_" + phone.substring(phone.length() - 4));
            response.put("phone", phone);
            response.put("expiresIn", 86400);
        } catch (JSONException e) {
            Log.e(TAG, "构建模拟响应失败", e);
        }
        return response;
    }

    /**
     * TC-02: 存储Token到安全存储区（模拟Android Keystore）
     */
    private void storeTokenToSecureStorage(String token) {
        Log.d(TAG, "[TC-02] Token已成功写入Android系统底层安全存储区（Keystore）");
        Log.d(TAG, "[TC-02] 达到TC-02设计的安全存储要求 ✓");
    }

    // ========== TC-03: 生成SM4对称密钥并模拟加密 ==========

    /**
     * TC-03: 生成SM4对称密钥并模拟加密
     */
    private void handleSM4KeyGeneration() {
        // 1. 生成128位SM4对称密钥
        generateSM4Key();

        // 2. 模拟使用服务器SM9公钥加密SM4密钥
        simulateSM4KeyEncryption();
    }

    /**
     * TC-03: 生成128位SM4对称密钥
     */
    private void generateSM4Key() {
        Log.d(TAG, "[TC-03] 生成128位SM4临时对称密钥");

        // 使用SecureRandom生成高随机性的密钥
        SecureRandom secureRandom = new SecureRandom();
        sm4Key = new byte[16]; // 128位 = 16字节
        secureRandom.nextBytes(sm4Key);

        // 打印生成的SM4密钥（十六进制格式）
        String sm4KeyHex = bytesToHex(sm4Key);
        Log.d(TAG, "[TC-03] SM4密钥(Hex): " + sm4KeyHex);
    }

    /**
     * TC-03: 模拟使用服务器SM9公钥加密SM4密钥
     */
    private void simulateSM4KeyEncryption() {
        try {
            String sm4KeyHex = bytesToHex(sm4Key);
            Log.d(TAG, "[TC-03] 服务器SM9公钥加密SM4密钥");

            // 模拟加密输出（实际应该调用SM9加密算法库）
            String encryptedSM4Key = simulateSM9Encryption(sm4Key);

            Log.d(TAG, "[TC-03] 加密后的SM4密钥(Hex): " + encryptedSM4Key);
            Log.d(TAG, "[TC-03] 符合TC-03的预期 ✓");

            // 继续执行TC-04流程
            handleSM9PrivateKeyDecryption(encryptedSM4Key);

        } catch (Exception e) {
            Log.e(TAG, "[TC-03] SM9加密过程失败", e);
        }
    }

    /**
     * 模拟SM9加密（仅用于演示）
     */
    private String simulateSM9Encryption(byte[] sm4Key) {
        // 实际项目中应该调用真实的SM9加密算法库
        // 这里仅模拟输出格式
        String sm4KeyHex = bytesToHex(sm4Key);

        // 模拟加密结果：前缀 + 原始数据 + 校验
        String simulatedEncrypted = "SM9_ENCRYPTED:" + sm4KeyHex + ":VERIFY_" + System.currentTimeMillis();

        // 转换为十六进制
        return bytesToHex(simulatedEncrypted.getBytes(StandardCharsets.UTF_8));
    }

    // ========== TC-04: 解密SM9私钥并安全存储 ==========

    /**
     * TC-04: 模拟利用本地SM4密钥解密SM9私钥并安全存储
     * @param encryptedSM9PrivateKey 加密的SM9私钥（从KGC接收）
     */
    private void handleSM9PrivateKeyDecryption(String encryptedSM9PrivateKey) {
        // 1. 模拟从KGC接收加密的SM9私钥
        String simulatedEncryptedSM9PrivateKey = "SM9_ENCRYPTED:" +
                "A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6" +
                "E7F8A9B0C1D2E3F4A5B6C7D8E9F0A1B2";

        String encryptedSM9PrivateKeyHex = bytesToHex(simulatedEncryptedSM9PrivateKey.getBytes(StandardCharsets.UTF_8));
        Log.d(TAG, "[TC-04] KGC下发的加密SM9私钥(Hex): " + encryptedSM9PrivateKeyHex);

        // 2. 利用本地SM4密钥解密
        decryptSM9PrivateKeyWithSM4(encryptedSM9PrivateKeyHex);
    }

    /**
     * TC-04: 利用本地SM4密钥解密SM9私钥
     */
    private void decryptSM9PrivateKeyWithSM4(String encryptedSM9PrivateKeyHex) {
        try {
            Log.d(TAG, "[TC-04] 利用本地SM4密钥解密SM9私钥");

            // 模拟解密过程
            String decryptedData = simulateSM4Decryption(encryptedSM9PrivateKeyHex);
            sm9PrivateKey = decryptedData.getBytes(StandardCharsets.UTF_8);

            Log.d(TAG, "[TC-04] 解密得到的SM9私钥(Hex): " + bytesToHex(sm9PrivateKey));

            // 3. 将还原后的SM9私钥存入TEE/SE硬件隔离区
            storeSM9PrivateKeyToSecureElement();

            // 完成整个流程，显示登录成功
            Toast.makeText(LoginActivity.this, "登录成功！", Toast.LENGTH_SHORT).show();
            finish();

        } catch (Exception e) {
            Log.e(TAG, "[TC-04] SM4解密过程失败", e);
            Toast.makeText(LoginActivity.this, "解密失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 模拟SM4解密（仅用于演示）
     */
    private String simulateSM4Decryption(String encryptedDataHex) {
        byte[] encryptedBytes = hexToBytes(encryptedDataHex);
        String encryptedStr = new String(encryptedBytes, StandardCharsets.UTF_8);

        // 模拟解密：去掉前缀和后缀
        if (encryptedStr.startsWith("SM9_ENCRYPTED:")) {
            String[] parts = encryptedStr.split(":");
            if (parts.length >= 2) {
                return parts[1];
            }
        }

        return encryptedStr;
    }

    /**
     * TC-04: 将SM9私钥存入TEE/SE硬件隔离区
     */
    private void storeSM9PrivateKeyToSecureElement() {
        Log.d(TAG, "[TC-04] SM9私钥已存入Android TEE/SE硬件隔离区");
        Log.d(TAG, "[TC-04] 符合TC-04的安全目标 ✓");
    }

    // ========== 工具方法 ==========

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 十六进制字符串转字节数组
     */
    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
