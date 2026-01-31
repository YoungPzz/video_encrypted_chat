package com.example.android_bysj_demo.shamir;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import android.util.Log;

import com.example.android_bysj_demo.RoomInfo;

public class ShamirSm4KeyReconstructor {
    private static final String TAG = "ShamirSm4KeyReconstructor";

    /**
     * 从 Shamir 分片重建 SM4 密钥
     * @return 十六进制格式的 SM4 密钥，失败时返回 null
     */
    public static class Share {
        private int index; // 此字段在算法中未使用，仅保留结构
        private String share;

        public Share(int index, String share) {
            this.index = index;
            this.share = share;
        }

        public int getIndex() {
            return index;
        }

        public String getShare() {
            return share;
        }
    }

    /**
     * 从 Shamir 分片重建 SM4 密钥（全 int 无符号版本）
     * @param shares 分片列表
     * @return 十六进制 SM4 密钥，失败返回 null
     */
    public static String reconstructSm4Key(List<RoomInfo.ShamirShare> shares) {
        try {
            if (shares == null || shares.isEmpty()) {
                Log.e(TAG, "分片数组不能为空");
                return null;
            }

            // 校验分片格式
            for (RoomInfo.ShamirShare share : shares) {
                if (share == null || share.share == null) {
                    Log.e(TAG, "无效的分片: " + share);
                    throw new IllegalArgumentException("无效的Shamir分片格式");
                }
            }

            Log.d(TAG, "=== 开始从Shamir分片重建SM4密钥 ===");
            Log.d(TAG, "可用分片数: " + shares.size());

            List<BinaryShare> binaryShares = new ArrayList<>();
            for (RoomInfo.ShamirShare share : shares) {
                // 1. Base64 解码（兼容 Android 版本）
                byte[] decodedBytes = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // 处理 URL 安全 Base64 和填充符
                    String base64 = share.share.trim()
                            .replace('-', '+')
                            .replace('_', '/');
                    while (base64.length() % 4 != 0) {
                        base64 += "=";
                    }
                    decodedBytes = Base64.getDecoder().decode(base64);
                }

                // 2. 核心改造：byte[] 转为无符号 int[]（一次性转换，后续无需再处理）
                int[] unsignedData = new int[decodedBytes.length];
                for (int i = 0; i < decodedBytes.length; i++) {
                    unsignedData[i] = Byte.toUnsignedInt(decodedBytes[i]);
                }

                binaryShares.add(new BinaryShare(share.index, unsignedData));

                // 调试：打印无符号分片数据（与 JS 完全一致）
                StringBuilder decStr = new StringBuilder();
                decStr.append("解码后的分片 (十进制): [");
                for (int i = 0; i < unsignedData.length; i++) {
                    decStr.append(unsignedData[i]);
                    if (i < unsignedData.length - 1) decStr.append(", ");
                }
                decStr.append("]");
                Log.d(TAG, decStr.toString());
            }

            // 校验所有分片长度一致
            int shareLength = binaryShares.get(0).unsignedData.length;
            for (BinaryShare bs : binaryShares) {
                if (bs.unsignedData.length != shareLength) {
                    throw new IllegalStateException("所有分片必须具有相同的长度");
                }
            }

            int sharesCount = binaryShares.size();
            int secretLength = shareLength - 1;
            int[] secret = new int[secretLength]; // 最终存储仍用 byte[]，但计算用 int

            // 3. 核心改造：xSamples/ySamples 改为 int[]，存储无符号坐标
            int[] xSamples = new int[sharesCount];
            int[] ySamples = new int[sharesCount];

            for (int i = 0; i < secretLength; i++) {
                for (int j = 0; j < sharesCount; j++) {
                    // 直接取无符号数据，无需再转换
                    xSamples[j] = binaryShares.get(j).unsignedData[shareLength - 1]; // X坐标（最后一个字节）
                    ySamples[j] = binaryShares.get(j).unsignedData[i]; // Y坐标（当前字节）
                }
                // 4. 调用 int 版本的插值方法
                int secretInt = interpolatePolynomial(xSamples, ySamples, 0);
                secret[i] = secretInt; // 最终转为 byte 存储（二进制一致）
            }

            // 转换为十六进制密钥（直接用无符号转换）
            // 假设 secret 是 int[] 类型，每个元素是 0~255 的无符号值
            StringBuilder hexKey = new StringBuilder();
            for (int unsignedByte : secret) {
                // 直接格式化 int 为两位十六进制，无需任何转换
                hexKey.append(String.format("%02x", unsignedByte));
            }

            Log.d(TAG, "✅ SM4密钥重建成功");
            Log.d(TAG, "最终密钥: " + hexKey.toString());
            return hexKey.toString();

        } catch (Exception e) {
            Log.e(TAG, "❌ SM4密钥重建失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 拉格朗日插值（全 int 无符号版本，与 JS 1:1 对齐）
     * @param xSamples 无符号 X 坐标数组
     * @param ySamples 无符号 Y 坐标数组
     * @param x 插值点（固定为 0）
     * @return 无符号插值结果（0~255）
     */
    private static int interpolatePolynomial(int[] xSamples, int[] ySamples, int x) {
        if (xSamples.length != ySamples.length) {
            throw new IllegalArgumentException("sample length mismatch");
        }
        int limit = xSamples.length;
        int result = 0; // 无符号结果

        for (int i = 0; i < limit; i++) {
            int basis = 1; // 无符号基值
            for (int j = 0; j < limit; j++) {
                if (i == j) continue;

                int num = add(x, xSamples[j]);
                int denom = add(xSamples[i], xSamples[j]);
                int term = div(num, denom);
                basis = mult(basis, term);
            }
            // 累加：result = add(result, mult(y, basis))
            result = add(result, mult(ySamples[i], basis));
        }

        return result & 0xFF; // 确保结果在 0~255 范围内
    }

    // 有限域加法（异或，无符号）
    private static int add(int a, int b) {
        return (a ^ b) & 0xFF;
    }

    // 有限域乘法（无符号，与 JS 1:1 对齐）
    private static int mult(int a, int b) {
        // 1. 完全复刻 JS：先查表，再判断是否为 0（执行顺序不能变）
        int logA = LOG_TABLE[a] & 0xFF; // 修复符号位：byte 转无符号 int
        int logB = LOG_TABLE[b] & 0xFF; // 修复符号位：byte 转无符号 int

        // 2. 完全复刻 JS：计算对数和并取模 255
        int sum = (logA + logB) % 255;

        // 3. 完全复刻 JS：查指数表获取结果
        int result = EXP_TABLE[sum] & 0xFF; // 修复符号位：byte 转无符号 int

        // 4. 完全复刻 JS：最后判断 a 或 b 是否为 0，返回对应结果
        return (a == 0 || b == 0) ? 0 : result;
    }

    // 有限域除法（无符号，严格跟随 JS 逻辑）
    private static int div(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("cannot divide by zero");
        }

        // 严格复刻 JS：先查表，再判断 a 是否为 0
        int logA = LOG_TABLE[a] & 0xFF;
        int logB = LOG_TABLE[b] & 0xFF;
        int diff = (logA - logB + 255) % 255;
        int result = EXP_TABLE[diff] & 0xFF;

        // JS 逻辑：最后判断 a 是否为 0
        return a == 0 ? 0 : result;
    }

    private static class BinaryShare {
        int index;
        int[] unsignedData;

        BinaryShare(int index, int[] unsignedData) {
            this.index = index;
            this.unsignedData = unsignedData;
        }
    }



    // 预定义的对数表和指数表
    private static final byte[] LOG_TABLE = {
            (byte)0x00, (byte)0xff, (byte)0xc8, (byte)0x08, (byte)0x91, (byte)0x10, (byte)0xd0, (byte)0x36, (byte)0x5a, (byte)0x3e, (byte)0xd8, (byte)0x43, (byte)0x99, (byte)0x77, (byte)0xfe, (byte)0x18,
            (byte)0x23, (byte)0x20, (byte)0x07, (byte)0x70, (byte)0xa1, (byte)0x6c, (byte)0x0c, (byte)0x7f, (byte)0x62, (byte)0x8b, (byte)0x40, (byte)0x46, (byte)0xc7, (byte)0x4b, (byte)0xe0, (byte)0x0e,
            (byte)0xeb, (byte)0x16, (byte)0xe8, (byte)0xad, (byte)0xcf, (byte)0xcd, (byte)0x39, (byte)0x53, (byte)0x6a, (byte)0x27, (byte)0x35, (byte)0x93, (byte)0xd4, (byte)0x4e, (byte)0x48, (byte)0xc3,
            (byte)0x2b, (byte)0x79, (byte)0x54, (byte)0x28, (byte)0x09, (byte)0x78, (byte)0x0f, (byte)0x21, (byte)0x90, (byte)0x87, (byte)0x14, (byte)0x2a, (byte)0xa9, (byte)0x9c, (byte)0xd6, (byte)0x74,
            (byte)0xb4, (byte)0x7c, (byte)0xde, (byte)0xed, (byte)0xb1, (byte)0x86, (byte)0x76, (byte)0xa4, (byte)0x98, (byte)0xe2, (byte)0x96, (byte)0x8f, (byte)0x02, (byte)0x32, (byte)0x1c, (byte)0xc1,
            (byte)0x33, (byte)0xee, (byte)0xef, (byte)0x81, (byte)0xfd, (byte)0x30, (byte)0x5c, (byte)0x13, (byte)0x9d, (byte)0x29, (byte)0x17, (byte)0xc4, (byte)0x11, (byte)0x44, (byte)0x8c, (byte)0x80,
            (byte)0xf3, (byte)0x73, (byte)0x42, (byte)0x1e, (byte)0x1d, (byte)0xb5, (byte)0xf0, (byte)0x12, (byte)0xd1, (byte)0x5b, (byte)0x41, (byte)0xa2, (byte)0xd7, (byte)0x2c, (byte)0xe9, (byte)0xd5,
            (byte)0x59, (byte)0xcb, (byte)0x50, (byte)0xa8, (byte)0xdc, (byte)0xfc, (byte)0xf2, (byte)0x56, (byte)0x72, (byte)0xa6, (byte)0x65, (byte)0x2f, (byte)0x9f, (byte)0x9b, (byte)0x3d, (byte)0xba,
            (byte)0x7d, (byte)0xc2, (byte)0x45, (byte)0x82, (byte)0xa7, (byte)0x57, (byte)0xb6, (byte)0xa3, (byte)0x7a, (byte)0x75, (byte)0x4f, (byte)0xae, (byte)0x3f, (byte)0x37, (byte)0x6d, (byte)0x47,
            (byte)0x61, (byte)0xbe, (byte)0xab, (byte)0xd3, (byte)0x5f, (byte)0xb0, (byte)0x58, (byte)0xaf, (byte)0xca, (byte)0x5e, (byte)0xfa, (byte)0x85, (byte)0xe4, (byte)0x4d, (byte)0x8a, (byte)0x05,
            (byte)0xfb, (byte)0x60, (byte)0xb7, (byte)0x7b, (byte)0xb8, (byte)0x26, (byte)0x4a, (byte)0x67, (byte)0xc6, (byte)0x1a, (byte)0xf8, (byte)0x69, (byte)0x25, (byte)0xb3, (byte)0xdb, (byte)0xbd,
            (byte)0x66, (byte)0xdd, (byte)0xf1, (byte)0xd2, (byte)0xdf, (byte)0x03, (byte)0x8d, (byte)0x34, (byte)0xd9, (byte)0x92, (byte)0x0d, (byte)0x63, (byte)0x55, (byte)0xaa, (byte)0x49, (byte)0xec,
            (byte)0xbc, (byte)0x95, (byte)0x3c, (byte)0x84, (byte)0x0b, (byte)0xf5, (byte)0xe6, (byte)0xe7, (byte)0xe5, (byte)0xac, (byte)0x7e, (byte)0x6e, (byte)0xb9, (byte)0xf9, (byte)0xda, (byte)0x8e,
            (byte)0x9a, (byte)0xc9, (byte)0x24, (byte)0xe1, (byte)0x0a, (byte)0x15, (byte)0x6b, (byte)0x3a, (byte)0xa0, (byte)0x51, (byte)0xf4, (byte)0xea, (byte)0xb2, (byte)0x97, (byte)0x9e, (byte)0x5d,
            (byte)0x22, (byte)0x88, (byte)0x94, (byte)0xce, (byte)0x19, (byte)0x01, (byte)0x71, (byte)0x4c, (byte)0xa5, (byte)0xe3, (byte)0xc5, (byte)0x31, (byte)0xbb, (byte)0xcc, (byte)0x1f, (byte)0x2d,
            (byte)0x3b, (byte)0x52, (byte)0x6f, (byte)0xf6, (byte)0x2e, (byte)0x89, (byte)0xf7, (byte)0xc0, (byte)0x68, (byte)0x1b, (byte)0x64, (byte)0x04, (byte)0x06, (byte)0xbf, (byte)0x83, (byte)0x38
    };

    private static final byte[] EXP_TABLE = {
            (byte)0x01, (byte)0xe5, (byte)0x4c, (byte)0xb5, (byte)0xfb, (byte)0x9f, (byte)0xfc, (byte)0x12, (byte)0x03, (byte)0x34, (byte)0xd4, (byte)0xc4, (byte)0x16, (byte)0xba, (byte)0x1f, (byte)0x36,
            (byte)0x05, (byte)0x5c, (byte)0x67, (byte)0x57, (byte)0x3a, (byte)0xd5, (byte)0x21, (byte)0x5a, (byte)0x0f, (byte)0xe4, (byte)0xa9, (byte)0xf9, (byte)0x4e, (byte)0x64, (byte)0x63, (byte)0xee,
            (byte)0x11, (byte)0x37, (byte)0xe0, (byte)0x10, (byte)0xd2, (byte)0xac, (byte)0xa5, (byte)0x29, (byte)0x33, (byte)0x59, (byte)0x3b, (byte)0x30, (byte)0x6d, (byte)0xef, (byte)0xf4, (byte)0x7b,
            (byte)0x55, (byte)0xeb, (byte)0x4d, (byte)0x50, (byte)0xb7, (byte)0x2a, (byte)0x07, (byte)0x8d, (byte)0xff, (byte)0x26, (byte)0xd7, (byte)0xf0, (byte)0xc2, (byte)0x7e, (byte)0x09, (byte)0x8c,
            (byte)0x1a, (byte)0x6a, (byte)0x62, (byte)0x0b, (byte)0x5d, (byte)0x82, (byte)0x1b, (byte)0x8f, (byte)0x2e, (byte)0xbe, (byte)0xa6, (byte)0x1d, (byte)0xe7, (byte)0x9d, (byte)0x2d, (byte)0x8a,
            (byte)0x72, (byte)0xd9, (byte)0xf1, (byte)0x27, (byte)0x32, (byte)0xbc, (byte)0x77, (byte)0x85, (byte)0x96, (byte)0x70, (byte)0x08, (byte)0x69, (byte)0x56, (byte)0xdf, (byte)0x99, (byte)0x94,
            (byte)0xa1, (byte)0x90, (byte)0x18, (byte)0xbb, (byte)0xfa, (byte)0x7a, (byte)0xb0, (byte)0xa7, (byte)0xf8, (byte)0xab, (byte)0x28, (byte)0xd6, (byte)0x15, (byte)0x8e, (byte)0xcb, (byte)0xf2,
            (byte)0x13, (byte)0xe6, (byte)0x78, (byte)0x61, (byte)0x3f, (byte)0x89, (byte)0x46, (byte)0x0d, (byte)0x35, (byte)0x31, (byte)0x88, (byte)0xa3, (byte)0x41, (byte)0x80, (byte)0xca, (byte)0x17,
            (byte)0x5f, (byte)0x53, (byte)0x83, (byte)0xfe, (byte)0xc3, (byte)0x9b, (byte)0x45, (byte)0x39, (byte)0xe1, (byte)0xf5, (byte)0x9e, (byte)0x19, (byte)0x5e, (byte)0xb6, (byte)0xcf, (byte)0x4b,
            (byte)0x38, (byte)0x04, (byte)0xb9, (byte)0x2b, (byte)0xe2, (byte)0xc1, (byte)0x4a, (byte)0xdd, (byte)0x48, (byte)0x0c, (byte)0xd0, (byte)0x7d, (byte)0x3d, (byte)0x58, (byte)0xde, (byte)0x7c,
            (byte)0xd8, (byte)0x14, (byte)0x6b, (byte)0x87, (byte)0x47, (byte)0xe8, (byte)0x79, (byte)0x84, (byte)0x73, (byte)0x3c, (byte)0xbd, (byte)0x92, (byte)0xc9, (byte)0x23, (byte)0x8b, (byte)0x97,
            (byte)0x95, (byte)0x44, (byte)0xdc, (byte)0xad, (byte)0x40, (byte)0x65, (byte)0x86, (byte)0xa2, (byte)0xa4, (byte)0xcc, (byte)0x7f, (byte)0xec, (byte)0xc0, (byte)0xaf, (byte)0x91, (byte)0xfd,
            (byte)0xf7, (byte)0x4f, (byte)0x81, (byte)0x2f, (byte)0x5b, (byte)0xea, (byte)0xa8, (byte)0x1c, (byte)0x02, (byte)0xd1, (byte)0x98, (byte)0x71, (byte)0xed, (byte)0x25, (byte)0xe3, (byte)0x24,
            (byte)0x06, (byte)0x68, (byte)0xb3, (byte)0x93, (byte)0x2c, (byte)0x6f, (byte)0x3e, (byte)0x6c, (byte)0x0a, (byte)0xb8, (byte)0xce, (byte)0xae, (byte)0x74, (byte)0xb1, (byte)0x42, (byte)0xb4,
            (byte)0x1e, (byte)0xd3, (byte)0x49, (byte)0xe9, (byte)0x9c, (byte)0xc8, (byte)0xc6, (byte)0xc7, (byte)0x22, (byte)0x6e, (byte)0xdb, (byte)0x20, (byte)0xbf, (byte)0x43, (byte)0x51, (byte)0x52,
            (byte)0x66, (byte)0xb2, (byte)0x76, (byte)0x60, (byte)0xda, (byte)0xc5, (byte)0xf3, (byte)0xf6, (byte)0xaa, (byte)0xcd, (byte)0x9a, (byte)0xa0, (byte)0x75, (byte)0x54, (byte)0x0e, (byte)0x01
    };
}
