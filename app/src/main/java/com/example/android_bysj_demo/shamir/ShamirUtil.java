package com.example.android_bysj_demo.shamir;

import java.util.List;

public class ShamirUtil {
    private static final int[] EXP = new int[512];
    private static final int[] LOG = new int[256];

    static {
        // shamir-secret-sharing 库使用的多项式是 0x11D
        int primitivePolynomial = 0x11D;
        int v = 1;
        for (int i = 0; i < 255; i++) {
            EXP[i] = v;
            EXP[i + 255] = v;
            LOG[v] = i;
            v <<= 1;
            if ((v & 0x100) != 0) {
                v ^= primitivePolynomial;
            }
        }
    }

    private static int add(int a, int b) { return a ^ b; }

    private static int mul(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return EXP[LOG[a] + LOG[b]];
    }

    private static int div(int a, int b) {
        if (b == 0) throw new ArithmeticException("Division by zero");
        if (a == 0) return 0;
        return EXP[LOG[a] - LOG[b] + 255];
    }

    public static class Share {
        public int id;
        public byte[] data;
        public Share(int id, byte[] data) { this.id = id; this.data = data; }
    }

    public static byte[] recover(List<Share> shares) {
        int length = shares.get(0).data.length;
        byte[] secret = new byte[length];

        for (int i = 0; i < length; i++) {
            int value = 0;
            for (int j = 0; j < shares.size(); j++) {
                int xj = shares.get(j).id;
                int yj = shares.get(j).data[i] & 0xFF;
                int num = 1, den = 1;
                for (int m = 0; m < shares.size(); m++) {
                    if (j == m) continue;
                    int xm = shares.get(m).id;
                    num = mul(num, xm);
                    den = mul(den, add(xm, xj));
                }
                value = add(value, mul(yj, div(num, den)));
            }
            secret[i] = (byte) value;
        }
        return secret;
    }
}