package xyz.ytora.ytool.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * created by yangtong on 2025/4/4 下午8:12
 * <br/>
 * 对称加密工具类<br/>
 * 对称加密：加密和解密使用同一把密钥，加密速度快，适合加密大量数据<br/>
 * AES是对称加密的经典算法
 */
public class AESUtil {
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * 获取AES密钥
     */
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(AES_KEY_SIZE);
        return generator.generateKey();
    }

    /**
     * IV，全称 Initialization Vector（初始化向量）<br/>
     * 它是对称加密（比如 AES）中用于增强安全性的一个 随机值，通常配合加密算法一起使用。<br/>
     * 为什么需要 IV？<br/>
     * 因为如果用同一个密钥加密两份相同的内容，结果会一样！<br/>
     * 这就带来严重的安全隐患：<br/>
     * 攻击者可以发现“哪些内容是重复的”，甚至进行模式分析。<br/>
     * 所以我们引入 IV，它能让：<br/>
     * 即使两份内容完全一样，只要 IV 不同，结果也不同。<br/>
     */
    public static byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public static String encrypt(String plainText, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(cipherText);
    }

    public static String decrypt(String encryptedText, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static String encodeBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] decodeBase64(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    public static SecretKey restoreKey(String base64Key) {
        return new SecretKeySpec(decodeBase64(base64Key), "AES");
    }
}
