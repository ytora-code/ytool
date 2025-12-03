package xyz.ytora.ytool.id.support;

import xyz.ytora.ytool.id.IdGenerator;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Monotonic ULID 生成器（实现 IdGenerator<String>）
 *
 * ULID 结构：128 bit = 48 bit 毫秒时间戳（大端） + 80 bit 随机数
 * 字符串编码：Crockford Base32，固定 26 字符（不含 I、L、O、U）
 *
 * 单调性（Monotonic）说明：
 * - 不同毫秒：时间戳自然递增，随机段重置为新的随机值。
 * - 同一毫秒：对随机段做大端自增（进位），从而保证生成顺序 == 字典序排序（单调非降）。
 * - 若 80bit 随机段在同一毫秒被“加到溢出”（极罕见），则等待到下一毫秒再继续生成。
 * - 若检测到时钟回拨（currentMs < lastMs），则将 currentMs 置为 lastMs，并继续自增随机段，保持单调。
 *
 * 线程安全：使用 synchronized 简洁保证；在大多数微服务场景已足够。
 */
public final class ULID implements IdGenerator<String> {

    /** Crockford Base32 字母表（不含 I L O U） */
    private static final char[] CROCKFORD =
            "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    /** ULID 固定长度：16字节（128 bit）与 26 字符 */
    private static final int BYTES_LEN = 16;
    private static final int STR_LEN = 26;

    /** 随机源（可换成 ThreadLocalRandom；此处默认 SecureRandom） */
    private final Random random;

    /** 单调状态：上次毫秒与上次随机段（10字节） */
    private long lastMs = -1L;
    private final byte[] lastRand = new byte[10];

    public ULID() {
        this(new SecureRandom());
    }

    public ULID(Random random) {
        this.random = (random != null) ? random : new SecureRandom();
    }

    /**
     * 生成下一个 ULID 字符串（26位，Base32），保证单调非降。
     */
    @Override
    public synchronized String nextId() {
        long ms = System.currentTimeMillis();

        if (ms > lastMs) {
            // 新毫秒：随机段重置
            random.nextBytes(lastRand);
            lastMs = ms;
        } else if (ms == lastMs) {
            // 同毫秒：随机段 +1（大端）
            if (incrementRandom(lastRand)) {
                // 极端：80bit 全部进位溢出 → 等待到下一毫秒
                ms = waitUntil(lastMs + 1);
                random.nextBytes(lastRand);
                lastMs = ms;
            }
        } else { // ms < lastMs → 时钟回拨
            // 保持单调：锚定到 lastMs，并对随机段 +1
            ms = lastMs;
            if (incrementRandom(lastRand)) {
                // 同上，溢出则等待下一毫秒
                ms = waitUntil(lastMs + 1);
                random.nextBytes(lastRand);
                lastMs = ms;
            }
        }

        // 组装为 16 字节（6 字节时间 + 10 字节随机），再 Base32 编码成 26 字符
        byte[] bytes = new byte[BYTES_LEN];
        // 时间戳 48 bit（大端）
        bytes[0] = (byte) (ms >>> 40);
        bytes[1] = (byte) (ms >>> 32);
        bytes[2] = (byte) (ms >>> 24);
        bytes[3] = (byte) (ms >>> 16);
        bytes[4] = (byte) (ms >>> 8);
        bytes[5] = (byte) (ms);
        // 随机段 80 bit
        System.arraycopy(lastRand, 0, bytes, 6, 10);

        return base32Encode128(bytes);
    }

    /* ================= 工具方法 ================= */

    /**
     * 随机段做大端自增；返回是否发生整体溢出（true 表示 80bit 从 0xFF..FF 进位到 0x00..00）
     */
    private static boolean incrementRandom(byte[] buf) {
        for (int i = buf.length - 1; i >= 0; i--) {
            int v = (buf[i] & 0xFF) + 1;
            buf[i] = (byte) v;
            if ((v & 0x100) == 0) {
                // 无进位，结束
                return false;
            }
        }
        // 全部进位，表示溢出
        // 约等于同一毫秒内生成了 2^80 个 ID（几乎不可能），交给调用处处理（等下一毫秒）
        return true;
    }

    /**
     * 忙等直到时间 >= targetMs（通常 <1ms）
     */
    private static long waitUntil(long targetMs) {
        long now = System.currentTimeMillis();
        while (now < targetMs) {
            now = System.currentTimeMillis();
        }
        return now;
    }

    /**
     * 将 16 字节（128bit）编码为固定 26 字符的 Crockford Base32。
     * 位打包实现：把所有比特顺序读出，每 5 bit 编成一个字符，不足 5 bit 的高位补 0。
     */
    private static String base32Encode128(byte[] bytes) {
        // 安全断言：固定 16 字节
        if (bytes.length != BYTES_LEN) {
            throw new IllegalArgumentException("ULID requires 16 bytes");
        }

        char[] out = new char[STR_LEN];
        int outPos = 0;

        int bitBuffer = 0;   // 累计的比特缓存
        int bitCount = 0;   // 缓存中当前比特数

        for (int i = 0; i < bytes.length; i++) {
            bitBuffer = (bitBuffer << 8) | (bytes[i] & 0xFF);
            bitCount += 8;

            while (bitCount >= 5) {
                int shift = bitCount - 5;
                int idx = (bitBuffer >> shift) & 0x1F; // 取最高的 5 bit
                out[outPos++] = CROCKFORD[idx];
                bitCount -= 5;
                bitBuffer &= (1 << shift) - 1; // 清掉已输出的高位
            }
        }

        if (bitCount > 0) {
            // 末尾剩余 <5bit，用 0 补到 5bit
            int idx = (bitBuffer << (5 - bitCount)) & 0x1F;
            out[outPos++] = CROCKFORD[idx];
        }

        // ULID 固定 26 字符，按 128/5=25.6 → 向上取整，必须是 26
        // 正常应当恰好 26
        while (outPos < STR_LEN) {
            out[outPos++] = CROCKFORD[0]; // 理论上不会走到这里，容错补'0'
        }

        return new String(out);
    }
}
