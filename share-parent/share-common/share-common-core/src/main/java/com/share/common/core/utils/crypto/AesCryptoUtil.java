package com.share.common.core.utils.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * 敏感信息 AES 加密与解密工具类。
 * 用于 API Key、第三方密钥等敏感字段防泄露加密持久化存储，严防明文入库与源码泄露。
 */
public class AesCryptoUtil {

    private static final Logger log = LoggerFactory.getLogger(AesCryptoUtil.class);
    private static final String ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String DEFAULT_SALT = "tianji-edu-security-secret-salt-2026@cloud";
    private static final int IV_LENGTH = 16;
    private static final byte[] KEY_BYTES;

    static {
        byte[] key;
        try {
            String customKey = System.getenv("TJ_ENCRYPTION_KEY");
            String seed = StringUtils.hasText(customKey) ? customKey.trim() : DEFAULT_SALT;
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(seed.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.error("初始化 AES 密钥生成失败，采用安全后备", ex);
            key = new byte[32];
        }
        KEY_BYTES = key;
    }

    private AesCryptoUtil() {}

    /**
     * 加密明文字符串
     * @param plainText 明文
     * @return Base64 编码的密文（包含随机 IV）
     */
    public static String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec secretKey = new SecretKeySpec(KEY_BYTES, ALGORITHM);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES 加密失败", e);
            throw new RuntimeException("密钥加密失败", e);
        }
    }

    /**
     * 解密密文字符串
     * @param cipherText Base64 编码的密文
     * @return 解密后的明文
     */
    public static String decrypt(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return cipherText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText.trim());
            if (combined.length <= IV_LENGTH) {
                return null;
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            int cipherLen = combined.length - IV_LENGTH;
            byte[] encrypted = new byte[cipherLen];
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, cipherLen);

            SecretKeySpec secretKey = new SecretKeySpec(KEY_BYTES, ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("AES 解密失败，可能为未加密文本或密钥不匹配: {}", e.getMessage());
            return null;
        }
    }
}
