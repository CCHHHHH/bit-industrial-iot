package bit.iot.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * MD5 加密工具类
 * 
 * @author chenhao
 * @date 2026/3/9
 */
public class MD5Util {
    
    /**
     * 全局数组，防止下方方法使用反射破解
     */
    private final static String[] hexDigits = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
    
    /**
     * MD5 加密
     * @param source 原始字符串
     * @return 加密后的字符串（32 位小写）
     */
    private static String md5(String source) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte item : array) {
                sb.append(byteToHexString(item));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 encryption error", e);
        }
    }

    /**
     * 加盐 MD5 加密
     * @param source 原始字符串
     * @param salt 盐值
     * @return 加密后的字符串（32 位小写）
     */
    public static String md5WithSalt(String source, String salt) {
        return md5(source + salt);
    }
    
    /**
     * 验证加盐密码
     * @param source 原始密码
     * @param salt 盐值
     * @param encryptedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean verifyMd5WithSalt(String source, String salt, String encryptedPassword) {
        return md5WithSalt(source, salt).equals(encryptedPassword);
    }
    
    /**
     * 将字节转换为十六进制字符串
     * @param b 字节
     * @return 十六进制字符串
     */
    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) {
            n += 256;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }
}
