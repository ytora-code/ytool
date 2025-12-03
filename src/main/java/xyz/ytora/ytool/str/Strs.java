package xyz.ytora.ytool.str;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * created by yangtong on 2025/4/4 下午3:40
 * 字符串工具类
 */
public class Strs {
    private static final char[] RESOURCE_CHARS;
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB", "PB"};
    private static final SecureRandom secureRandom = new SecureRandom();
    //默认日期格式
    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyy/MM/dd HH:mm:ss"
    };

    static {
        String resourceStr = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM`~!@#$%^&*()_+[];',./【】；‘，。、{}\\|:\"<>?";
        RESOURCE_CHARS = resourceStr.toCharArray();
    }

    /**
     * 得到指定长度的随机字符串
     * <br/>
     * @param n     指定长度
     * @param supplier 产生随机数的方式
     * @return 随机字符串
     */
    public static String randomStr(int n, Supplier<Integer> supplier) {
        char[] str = new char[n];
        for (int i = 0; i < n; i++) {
            //生成一个大于等于0且小于bound的随机整数。
            int index = supplier.get();
            //根据随机整数获取字符串
            str[i] = RESOURCE_CHARS[index];
        }
        return new String(str);
    }

    /**
     * 以高性能、快速产生随机数的方式生成随机字符串
     */
    public static String randomStr(int n, int start, int end) {
        if (start < 0 || end > RESOURCE_CHARS.length || start >= end) {
            throw new IllegalArgumentException("start/end参数非法，start必须>=0且小于end，end必须小于资源字符长度");
        }
        return randomStr(n, () -> ThreadLocalRandom.current().nextInt(end - start) + start);
    }

    /**
     * 以安全、强随机产生随机数的方式生成随机字符串
     */
    public static String secureRandomStr(int n, int start, int end) {
        if (start < 0 || end > RESOURCE_CHARS.length || start >= end) {
            throw new IllegalArgumentException("start/end参数非法，start必须>=0且小于end，end必须小于资源字符长度");
        }
        return randomStr(n, () -> secureRandom.nextInt(end - start) + start);
    }


    /**
     * 得到指定的长度的随机数字<br/>
     *
     * @param n 长度
     * @return 随机数字
     */
    public static String randomNumber(int n) {
        return randomStr(n, 0, 10);
    }

    /**
     * 得到指定的长度的随机字符串<br/>
     *
     * @param n 长度
     * @return 随机字符串
     */
    public static String randomStr(int n) {
        return randomStr(n, 0, RESOURCE_CHARS.length);
    }

    /**
     * 将目标数字变成指定位数的字符串，不足前面填充0<br/>
     *  fillZero(123, 5) -> "00123"
     * @param targetNum 目标数字
     * @param length    字符串长度
     * @return 指定长度的字符串
     */
    public static String fillZero(int targetNum, int length) {
        String targetNumStr = String.valueOf(targetNum);
        if (targetNumStr.length() >= length) return targetNumStr;
        return "0".repeat(length - targetNumStr.length()) + targetNumStr;
    }

    /**
     * 判断target是否为空白字符串
     *
     * @param target 目标字符串
     * @return 是否为空白字符串
     */
    public static boolean isEmpty(String target) {
        return target == null || target.trim().isBlank();
    }

    /**
     * 判断target是否不为空白字符串
     *
     * @param target 目标字符串
     * @return 是否不为空白字符串
     */
    public static boolean isNotEmpty(String target) {
        return !isEmpty(target);
    }

    /**
     * 判断是否为数字
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        return str.matches("\\d+");
    }

    /**
     * 判断字符是否是汉字
     */
    public static boolean isChinese(char ch) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(ch);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT;
    }

    /**
     * 判断字符串是否全为汉字
     */
    public static boolean isChinese(String str) {
        if (str == null || str.isEmpty()) return false;
        for (char c : str.toCharArray()) {
            if (!isChinese(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据前缀、后缀、数据源解析占模板字符串的占位符并填充数据到对应占位符<br/>
     *
     * @param templateStr 模板字符串
     * @param datasource  数据源
     * @param prefix      占位符前缀
     * @param suffix      占位符后缀
     * @return 解析占位符并填充数据后的字符串
     */
    public static String parseByPlaceholder(String templateStr, Map<String, Object> datasource, Placeholder prefix, Placeholder suffix) {
        if (isEmpty(templateStr)) {
            return "";
        }
        //用来保存格式化之后的字符串
        StringBuilder resultStr = new StringBuilder();

        //设置快慢指针，low遇到prefix停下来，high遇到suffix时如果low处于停止状态则high也停下来
        int low = 0, high = 0;
        //记录low、high指针的停止与否，false表示没有停止
        boolean lowStatus = false, highStatus = false;
        //缓存lowStatus = true时，遍历到的字符串
        StringBuilder cache = new StringBuilder();
        for (int i = 0; i < templateStr.length(); i++) {
            char currentChar = templateStr.charAt(i);

            //前缀成功匹配，停下慢指针
            if (templateStr.startsWith(prefix.placeholder, i)) {
                lowStatus = true;
                //如果遇到新的前缀时，上一个前缀还没有闭合，就将缓存写入sb，并清空缓存
                if (!cache.isEmpty()) {
                    resultStr.append(cache);
                    cache = new StringBuilder();
                    low = i;
                }
            }
            //前缀后缀都匹配成功，停下快指针
            if (lowStatus && templateStr.startsWith(suffix.placeholder, i)) {
                highStatus = true;
            }

            //如果快慢指针都是停下的，说明占位符前后缀成功闭合了，根据占位符从数据源获取数据追加到sb，并清空缓存
            if (lowStatus && highStatus) {
                String key = templateStr.substring(low + prefix.placeholder.length(), high);
                String value = String.valueOf(datasource.getOrDefault(key, ""));
                resultStr.append(value);
                //reset
                lowStatus = false;
                highStatus = false;
                low = high = i + suffix.placeholder.length();
                //"${"成功闭合后
                cache = new StringBuilder();
                continue;
            }

            //low指针没有停下来
            if (!lowStatus) {
                low++;
                resultStr.append(currentChar);
            }
            //low指针停下里了，就缓存当前遍历的字符串
            else {
                cache.append(currentChar);
            }
            //为了防止缓存还没有写入resultStr循环就结束了的情况，在最后一次循环时将缓存数据写入sb
            if (i == templateStr.length() - 1) {
                resultStr.append(cache);
            }

            high++;
        }

        return resultStr.toString();
    }

    /**
     * 解析并格式化模板字符串中的"${xxx}" <br/>
     * eg: 数据源{name:"zs", age:23} <br/>
     * "hello ${name}!!!${age}" ---> "hello zs!!!23" <br/>
     * "hello ${name!!!${age}" ---> "hello ${name!!!23" <br/>
     * "hello ${name!!!${age${$" ---> "hello zs!!!${age${$" <br/>
     *
     * @param templateStr 模板字符串
     * @param datasource  数据源
     * @return 格式化之后的字符串
     */
    public static String format(String templateStr, Map<String, Object> datasource) {
        return parseByPlaceholder(templateStr, datasource, Placeholder.LEFT_CURLY_BRACE_DOLLAR, Placeholder.RIGHT_CURLY_BRACE);
    }

    /**
     * 格式化模板字符串，将不定参数填充到目标字符串的对应位置上 <br/>
     * eg: <br/>
     * format("my name is {}, and age is {}", "zs", 23) -> "my name is zs, and age is 23" <br/>
     * format("my name is {}, and age is {}", "zs") -> "my name is zs, and age is " <br/>
     * format("my name is {}", "狗剩", 22) -> "my name is 狗剩" <br/>
     *
     * @param templateStr 模板字符串
     * @param args        参数
     * @return 格式化之后的字符串
     */
    public static String format(String templateStr, Object... args) {
        //用来保存格式化之后的字符串
        StringBuilder resultStr = new StringBuilder();

        //指向args数组的指针
        int index = 0;
        for (int i = 0; i < templateStr.length(); i++) {
            char currentChar = templateStr.charAt(i);
            char nextChar = templateStr.charAt(Integer.min(i + 1, templateStr.length() - 1));
            if (currentChar == '{' && nextChar == '}') {
                i++;
                if (index < args.length) {
                    resultStr.append(args[index++]);
                }
                continue;
            }
            resultStr.append(templateStr.charAt(i));
        }

        return resultStr.toString();
    }

    /**
     * 首字母大写
     */
    public static String firstCapitalize(String name) {
        if (Strs.isEmpty(name)) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * 判断目标字符串是否属于候选字符串数组中的一个
     * <br/>
     * 匹配的字符串非空
     */
    public static Boolean contain(String targetStr, List<String> candidateList) {
        if (isEmpty(targetStr)) {
            return false;
        }
        if (candidateList == null || candidateList.isEmpty()) {
            return false;
        }
        for (String candidateStr : candidateList) {
            if (isEmpty(candidateStr)) {
                continue;
            }
            if (candidateStr.equals(targetStr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断目标字符串是否属于候选字符串数组中的一个
     * <br/>
     * 匹配的字符串非空
     */
    public static Boolean contain(String targetStr, String... candidateArr) {
        if (candidateArr == null) {
            return false;
        }
        return contain(targetStr, Arrays.stream(candidateArr).toList());
    }

    /**
     * 将以指定分隔符进行分割的字符串变成小驼峰
     * <br/>
     * 比如分割符是"_"，那么user_name -> userName
     */
    public static String toLowerCamelCase(String targetStr, Character delimiter) {
        StringBuilder result = new StringBuilder();
        boolean toUpper = false;
        for (int i = 0; i < targetStr.length(); i++) {
            char c = targetStr.charAt(i);
            if (c == delimiter) {
                toUpper = true;
            } else {
                result.append(toUpper ? Character.toUpperCase(c) : c);
                toUpper = false;
            }
        }
        return result.toString();
    }

    /**
     * 将下划线分割的字符串变成小驼峰
     * <br/>
     * user_name -> userName
     */
    public static String toLowerCamelCase(String targetStr) {
        return toLowerCamelCase(targetStr, '_');
    }

    /**
     * 将以指定分隔符进行分割的字符串变成大驼峰
     * <br/>
     * 比如分割符是"_"，那么user_name -> UserName
     */
    public static String toUpperCamelCase(String targetStr, Character delimiter) {
        String lowerCamel = toLowerCamelCase(targetStr, delimiter);
        return firstCapitalize(lowerCamel);
    }

    /**
     * 将驼峰命名（userName、UserName）转为指定连接符连接的小写形式（如 user-name）
     */
    public static String splitCamelCase(String targetStr, String delimiter) {
        if (targetStr == null || targetStr.isEmpty()) {
            return targetStr;
        }

        StringBuilder result = new StringBuilder();
        char[] chars = targetStr.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (Character.isUpperCase(ch)) {
                // 前面不是开头，也不是连续大写，则插入分隔符
                if (i > 0 && (Character.isLowerCase(chars[i - 1]) ||
                        (i + 1 < chars.length && Character.isLowerCase(chars[i + 1])))) {
                    result.append(delimiter);
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * 将驼峰命名（userName、UserName）转为下划线连接的小写形式（如 user_name）
     */
    public static String toUnderline(String targetStr) {
        return splitCamelCase(targetStr, "_");
    }

    /**
     * 将下划线分割的字符串变成大驼峰
     * <br/>
     * user_name -> UserName
     */
    public static String toUpperCamelCase(String targetStr) {
        return toUpperCamelCase(targetStr, '_');
    }

    /**
     * 格式化字节数为带单位的字符串（保留两位小数）
     * @param bytes 字节数
     * @return 格式化字符串，如 1.23 MB
     */
    public static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";

        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < UNITS.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, UNITS[unitIndex]);
    }

    /**
     * 将带单位的文件大小字符串（如 1.23 MB）转换为字节数
     * 支持单位：B、KB、MB、GB、TB、PB（不区分大小写）
     * @param sizeStr 带单位的字符串
     * @return 字节数
     */
    public static long parseSize(String sizeStr) {
        if (Strs.isEmpty(sizeStr)) {
            throw new IllegalArgumentException("Size string cannot be null or empty");
        }

        String normalized = sizeStr.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        String numberPart = normalized.replaceAll("[A-Z]+", "");
        String unitPart = normalized.replaceAll("[^A-Z]+", "");

        if (numberPart.isEmpty() || unitPart.isEmpty()) {
            throw new IllegalArgumentException("Invalid size format: " + sizeStr);
        }

        double number = Double.parseDouble(numberPart);
        int unitIndex = -1;

        for (int i = 0; i < UNITS.length; i++) {
            if (UNITS[i].equals(unitPart)) {
                unitIndex = i;
                break;
            }
        }

        if (unitIndex == -1) {
            throw new IllegalArgumentException("Unknown size unit: " + unitPart);
        }

        return (long) (number * Math.pow(1024, unitIndex));
    }

    /**
     * 将毫秒转换为可读的时间格式
     * @param uptimeMillis 毫秒数
     * @return 格式化后的字符串，如："2天3小时45分钟12秒"
     */
    public static String formatMillis(long uptimeMillis) {
        if (uptimeMillis <= 0) {
            return "0秒";
        }

        long seconds = uptimeMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        // 计算余数
        long remainingHours = hours % 24;
        long remainingMinutes = minutes % 60;
        long remainingSeconds = seconds % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append("天");
        }
        if (remainingHours > 0) {
            sb.append(remainingHours).append("小时");
        }
        if (remainingMinutes > 0) {
            sb.append(remainingMinutes).append("分钟");
        }
        if (remainingSeconds > 0) {
            sb.append(remainingSeconds).append("秒");
        }

        // 如果所有值都是0，返回小于1秒
        if (sb.length() == 0) {
            sb.append("小于1秒");
        }

        return sb.toString();
    }

    /**
     * 将二进制数据变成16进制
     */
    public static String toHex(byte[] data) {
        StringBuilder builder = new StringBuilder();
        if (data == null || data.length == 0) {
            return "";
        }

        for (byte datum : data) {
            int v = datum & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                builder.append(0);
            }
            hv = hv.toUpperCase();
            builder.append(hv);
        }
        return builder.toString();
    }

    /**
     * 尝试将字符串转为Int
     */
    public static Integer tryToInt(String val, Integer defaultVal) {
        try {
            return val == null ? defaultVal : Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /**
     * 尝试将字符串转为Long
     */
    public static Long tryToLong(String val, Long defaultVal) {
        try {
            return val == null ? defaultVal : Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /**
     * 尝试将字符串转为Float
     */
    public static Float tryToFloat(String val, Float defaultVal) {
        try {
            return val == null ? defaultVal : Float.parseFloat(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /**
     * 尝试将字符串转为Double
     */
    public static Double tryToDouble(String val, Double defaultVal) {
        try {
            return val == null ? defaultVal : Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /**
     * 尝试将字符串转为Bool
     */
    public static Boolean tryToBool(String val, Boolean defaultVal) {
        if (val == null) return defaultVal;
        String trimmed = val.trim().toLowerCase();
        return switch (trimmed) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> defaultVal;
        };
    }

    /**
     * 尝试将字符串转为Decimal
     */
    public static BigDecimal tryToBigDecimal(String val, BigDecimal defaultVal) {
        try {
            return val == null ? defaultVal : new BigDecimal(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /**
     * 尝试将字符串转为Date
     */
    public static Date tryToDate(String val, Date defaultVal) {
        if (val == null || val.trim().isEmpty()) return defaultVal;
        for (String pattern : DATE_PATTERNS) {
            try {
                return new SimpleDateFormat(pattern).parse(val.trim());
            } catch (ParseException ignored) {
            }
        }
        return defaultVal;
    }

    //快捷重载（无默认值，返回 null）
    public static Integer tryToInt(String val) {
        return tryToInt(val, null);
    }

    public static Long tryToLong(String val) {
        return tryToLong(val, null);
    }

    public static Float tryToFloat(String val) {
        return tryToFloat(val, null);
    }

    public static Double tryToDouble(String val) {
        return tryToDouble(val, null);
    }

    public static Boolean tryToBool(String val) {
        return tryToBool(val, null);
    }

    public static BigDecimal tryToBigDecimal(String val) {
        return tryToBigDecimal(val, null);
    }

    public static Date tryToDate(String val) {
        return tryToDate(val, null);
    }


    /**
     * HTML转义
     */
    public static String escapeHtml(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * HTML反转义
     */
    public static String unescapeHtml(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    /**
     * 返回对象md5加密后的字符串
     */
    public static String md5(Object obj) {
        String objString = obj.toString();
        try {
            // 获取 MD5 摘要算法的 MessageDigest 对象
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算 MD5 哈希
            byte[] digest = md.digest(objString.getBytes());

            // 将 byte[] 转换成十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new StrException("MD5算法不可用", e);
        }
    }
}
