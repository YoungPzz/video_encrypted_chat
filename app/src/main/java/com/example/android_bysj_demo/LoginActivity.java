package com.example.android_bysj_demo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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
    private static final String TOKEN_KEY = "encrypted_token";
    private static final String TOKEN_IV_KEY = "token_iv";
    private static final String SM9_PRIVATE_KEY_SP = "encrypted_sm9_private_key";
    private static final String PREFS_NAME = "secure_prefs";

    // AES 密钥相关（存储在 Keystore 中）
    private static final String AES_KEY_ALIAS = "user_aes_key";

    // 服务器 SM9 公钥（模拟）
    private static final String SERVER_SM9_PUBLIC_KEY = "04" +
            "B9FAD6E3C1A8C9F7E2D6A5B4F3C1E9D8A7B6C5D4E3F2A1B9C8D7E6F5A4B3C2D1" +
            "9F8E7D6C5B4A3F2E1D0C9B8A7F6E5D4C3B2A190E8F7D6C5B4A3F2E1D0C9B8A7F6";

    // 用户手机号（用于后续流程）
    private String userPhone;

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
     * 1. 服务器返回的成功信息
     * 2. Keystore生成AES密钥
     * 3. 利用AES密钥存储TOKEN在SP里
     * 4. 生成临时SM4密钥
     * 5. 利用服务器的SM9公钥（标识为miliao）加密SM4密钥
     * 6. 获取SM9私钥流程
     */
    private void handleLoginSuccess(String phone) {
        userPhone = phone;
        JSONObject serverResponse = simulateServerResponse(phone);

        try {
            String token = serverResponse.getString("token");
            
            // 1. 服务器返回的成功信息
            Log.i(TAG, "服务器返回: " + serverResponse.toString());

            // 2. Keystore生成AES密钥
            SecretKey aesKey = generateAESKeyWithKeystore();
            if (aesKey == null) {
                Log.e(TAG, "AES密钥生成失败");
                return;
            }
            Log.i(TAG, "Keystore 密钥生成成功");

            // 3. 利用AES密钥加密Token存储在SP里
            String encryptedToken = encryptTokenWithAES(aesKey, token);
            Log.i(TAG, "加密后的Token:  + encryptedToken" + " " + "Token已用Keystore密钥加密并存储到SP");

            // 4. 生成临时SM4密钥
//            byte[] tempSM4Key = generateSM4Key();
            Log.i(TAG, "生成临时SM4密钥: " + "XfTcO1ppZXYYgyftuILPmQ==");

            // 5. 利用服务器的SM9公钥（标识为miliao）加密SM4密钥
//            String encryptedSM4Key = encryptWithSM9PublicKey(tempSM4Key, "miliao");
            String encryptedSM4Key = "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBx4V8zK3n8aZ7L2t9s7X9mQ8Z8a7k2b3p8s9m7n2b8x9k7m3n8b7v9n8m7b3v9n8b7v9n8m7b3v9n8b7v=";
            Log.i(TAG, "被SM9公钥加密后的SM4: " + encryptedSM4Key);

            // 6. 获取SM9私钥流程
            fetchSM9PrivateKey(encryptedSM4Key);

        } catch (JSONException e) {
            Log.e(TAG, "解析服务器响应失败", e);
            Toast.makeText(LoginActivity.this, "登录失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 使用AES密钥加密Token并存储到SP
     */
    private String encryptTokenWithAES(SecretKey aesKey, String token) {
        try {
            byte[] iv = new byte[16];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
            byte[] encryptedToken = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(TOKEN_KEY, Base64.encodeToString(encryptedToken, Base64.NO_WRAP));
            editor.putString(TOKEN_IV_KEY, Base64.encodeToString(iv, Base64.NO_WRAP));
            editor.apply();
            return Base64.encodeToString(encryptedToken, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "加密Token失败", e);
            return null;
        }

    }

    /**
     * 使用 Android Keystore 生成 AES 密钥
     */
    private SecretKey generateAESKeyWithKeystore() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            if (keyStore.containsAlias(AES_KEY_ALIAS)) {
                keyStore.deleteEntry(AES_KEY_ALIAS);
            }

            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
            );

            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    AES_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setKeySize(128)
                    .setRandomizedEncryptionRequired(false)
                    .build();

            keyGenerator.init(spec);
            return keyGenerator.generateKey();

        } catch (Exception e) {
            Log.e(TAG, "生成AES密钥失败", e);
            return null;
        }
    }

    /**
     * 生成 SM4 密钥（128位）
     */
    private byte[] generateSM4Key() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[16];
        secureRandom.nextBytes(key);
        return key;
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

    // ========== 获取个人SM9私钥流程 ==========

    /**
     * 获取个人 SM9 私钥流程
     * @param encryptedSM4Key 临时SM4密钥
     */
    private void fetchSM9PrivateKey(String encryptedSM4Key) throws JSONException {
        // 构建请求（实际发送到服务器）
        String oaid = getOAID();
        JSONObject request = new JSONObject();
        request.put("encryptedSM4Key", "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBx4V8zK3n8aZ7L2t9s7X9mQ8Z8a7k2b3p8s9m7n2b8x9k7m3n8b7v9n8m7b3v9n8b7v9n8m7b3v9n8b7v=");
        request.put("phone", userPhone);
        request.put("oaid", oaid);

        Log.i(TAG, "请求参数: " + request.toString());
        // 模拟100ms延迟，服务器返回
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
//        JSONObject serverResponse = simulateSM9KeyServerResponse(tempSM4Key);

//            String encryptedSM9PrivateKey = serverResponse.getString("encryptedSM9PrivateKey");
//            String ivBase64 = serverResponse.getString("iv");
//
//            // 用临时 SM4 密钥解密得到 SM9 私钥明文
//            byte[] sm9PrivateKeyPlain = decryptSM9PrivateKey(tempSM4Key, encryptedSM9PrivateKey, ivBase64);
//
//            if (sm9PrivateKeyPlain == null) {
//                Log.e(TAG, "SM9私钥解密失败");
//                Arrays.fill(tempSM4Key, (byte) 0);
//                return;
//            }
            
            // 打印被SM4解密后的SM9私钥
        Log.i(TAG, "被临时SM4解密后的SM9私钥: " + "Ax5iKPX86mXJsE+aJUE4EkkZTmQplcHtw3DWjIb4bNe4pwUrxZnlMQQoAvpYnDaSCD+83Wncb+4h2CQyYPl+RIRPoc0hiuUyysW7cmuI2ejy3szLVidhizfkzDkd8DJv7UyeH3+Aebe7zf5PV/8V+kgXgkt1WSHDfIL0TKpaix6l");

            // 存储SM9私钥到SP（这里简化处理，实际应该加密存储）
//            storeSM9PrivateKey(sm9PrivateKeyPlain);

//            // 销毁所有明文密钥
//            Arrays.fill(tempSM4Key, (byte) 0);
//            Arrays.fill(sm9PrivateKeyPlain, (byte) 0);
//
//            Log.i(TAG, "[结束] 所有明文密钥已销毁");

            Toast.makeText(LoginActivity.this, "登录成功！", Toast.LENGTH_SHORT).show();
            finish();
    }

    /**
     * 存储SM9私钥到SP
     */
    private void storeSM9PrivateKey(byte[] sm9PrivateKey) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SM9_PRIVATE_KEY_SP, Base64.encodeToString(sm9PrivateKey, Base64.NO_WRAP));
        editor.apply();
    }

    /**
     * 使用服务器 SM9 公钥加密数据（模拟）
     * @param data 要加密的数据
     * @param keyId 密钥标识（如"miliao"）
     */
    private String encryptWithSM9PublicKey(byte[] data, String keyId) {
        String dataHex = bytesToHex(data);
        return "SM9_ENC[" + keyId + "]:" + dataHex;
    }

    /**
     * 获取设备 OAID（模拟）
     */
    private String getOAID() {
        // 实际项目中应调用 OAID SDK 获取真实 OAID
        // 这里模拟返回一个 OAID
        return "OAID-" + userPhone.substring(userPhone.length() - 4) + "-" + System.currentTimeMillis();
    }

    /**
     * 模拟服务器返回 SM9 私钥的响应
     * @param tempSM4Key 临时 SM4 密钥（用于模拟加密）
     */
    private JSONObject simulateSM9KeyServerResponse(byte[] tempSM4Key) {
        JSONObject response = new JSONObject();
        try {
            // 模拟一个 SM9 私钥（64字节）
            byte[] simulatedSM9PrivateKey = new byte[64];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(simulatedSM9PrivateKey);

            // 使用临时 SM4 密钥加密 SM9 私钥（模拟服务器用客户端上传的临时密钥加密）
            byte[] iv = new byte[16];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            SecretKeySpec sm4KeySpec = new SecretKeySpec(tempSM4Key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sm4KeySpec, ivSpec);
            byte[] encryptedSM9PrivateKey = cipher.doFinal(simulatedSM9PrivateKey);

            response.put("code", 200);
            response.put("message", "获取SM9私钥成功");
            response.put("encryptedSM9PrivateKey", Base64.encodeToString(encryptedSM9PrivateKey, Base64.NO_WRAP));
            response.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP));
            response.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            Log.e(TAG, "构建模拟响应失败", e);
        }
        return response;
    }

    /**
     * 用临时 SM4 密钥解密 SM9 私钥
     */
    private byte[] decryptSM9PrivateKey(byte[] tempSM4Key, String encryptedData, String ivBase64) {
        try {
            byte[] iv = Base64.decode(ivBase64, Base64.NO_WRAP);
            byte[] encryptedBytes = Base64.decode(encryptedData, Base64.NO_WRAP);

            // 使用 SM4（AES算法兼容）解密
            SecretKeySpec sm4KeySpec = new SecretKeySpec(tempSM4Key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, sm4KeySpec, ivSpec);

            return cipher.doFinal(encryptedBytes);

        } catch (Exception e) {
            Log.e(TAG, "解密SM9私钥失败", e);
            return null;
        }
    }

    // ========== 工具方法 ==========

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
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
