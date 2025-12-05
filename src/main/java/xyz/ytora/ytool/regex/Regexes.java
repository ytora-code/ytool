package xyz.ytora.ytool.regex;

import java.util.regex.Pattern;

/**
 * 正则表达式工具类
 * 提供常用的数据格式校验方法
 */
public final class Regexes {

    // 私有构造器，防止实例化
    private Regexes() {
    }

    // =========================================================================
    // 账号与身份相关 (Account & Identity)
    // =========================================================================
    /**
     * 普通手机号
     * 规则：1开头，11位数字
     */
    public static final String REGEX_MOBILE = "^1[3-9]\\d{9}$";
    private static final Pattern PATTERN_MOBILE = Pattern.compile(REGEX_MOBILE);

    /**
     * 固定电话 (座机)
     * 规则：
     * - 区号：0开头，2到3位 (如 010, 021, 0755)
     * - 分隔符：可选 - 或 空格
     * - 号码：7到8位
     * - 示例：010-12345678, 0755 1234567, 02112345678
     */
    public static final String REGEX_LANDLINE = "^0\\d{2,3}[- ]?\\d{7,8}$";
    private static final Pattern PATTERN_LANDLINE = Pattern.compile(REGEX_LANDLINE);

    /**
     * 服务热线 / 特服号 / 400电话
     * 规则：
     * - 3位紧急电话：110, 120, 119
     * - 5位服务号：12345, 10086, 95555
     * - 400/800电话：400-xxx-xxxx
     */
    public static final String REGEX_SERVICE_NUM = "^(1\\d{2}|1\\d{4}|9\\d{4}|(400|800)[- ]?\\d{3}[- ]?\\d{4})$";
    private static final Pattern PATTERN_SERVICE_NUM = Pattern.compile(REGEX_SERVICE_NUM);

    /**
     * 电子邮箱校验
     * 规则：常见的邮箱格式，支持字母、数字、下划线、点、中划线
     */
    public static final String REGEX_EMAIL = "^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$";
    private static final Pattern PATTERN_EMAIL = Pattern.compile(REGEX_EMAIL);

    /**
     * 15位身份证校验 (一代身份证)
     * 规则：
     * 1. 6位地址码
     * 2. 6位出生日期 (年份仅2位)
     * 3. 3位顺序码 (无校验码)
     */
    public static final String REGEX_ID_CARD_15 = "^[1-9]\\d{7}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}$";
    private static final Pattern PATTERN_ID_CARD_15 = Pattern.compile(REGEX_ID_CARD_15);

    /**
     * 中国居民身份证校验 (18位)
     * 规则：前17位为数字，最后一位是数字或X/x
     * 注意：此正则仅校验格式，不校验出生日期和校验码的逻辑合法性
     */
    public static final String REGEX_ID_CARD_18 = "^[1-9]\\d{5}[1-9]\\d{3}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}([0-9Xx])$";
    private static final Pattern PATTERN_ID_CARD_18 = Pattern.compile(REGEX_ID_CARD_18);

    /**
     * 强密码校验
     * 规则：8-20位，必须包含大小写字母和数字，特殊字符可选
     */
    public static final String REGEX_PASSWORD_STRONG = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d\\W]{8,20}$";
    private static final Pattern PATTERN_PASSWORD_STRONG = Pattern.compile(REGEX_PASSWORD_STRONG);


    // =========================================================================
    // 字符与数字相关 (Characters & Numbers)
    // =========================================================================

    /**
     * 纯汉字校验
     * 规则：仅包含中文字符
     */
    public static final String REGEX_CHINESE = "^[\\u4e00-\\u9fa5]+$";
    private static final Pattern PATTERN_CHINESE = Pattern.compile(REGEX_CHINESE);

    /**
     * 整数校验 (包括正负数)
     */
    public static final String REGEX_INTEGER = "^-?\\d+$";
    private static final Pattern PATTERN_INTEGER = Pattern.compile(REGEX_INTEGER);

    /**
     * 正整数校验
     */
    public static final String REGEX_POSITIVE_INTEGER = "^[1-9]\\d*$";
    private static final Pattern PATTERN_POSITIVE_INTEGER = Pattern.compile(REGEX_POSITIVE_INTEGER);

    /**
     * 浮点数/小数校验 (包括正负)
     */
    public static final String REGEX_DECIMAL = "^-?\\d+(\\.\\d+)?$";
    private static final Pattern PATTERN_DECIMAL = Pattern.compile(REGEX_DECIMAL);


    // =========================================================================
    // 网络与链接相关 (Network & URL)
    // =========================================================================

    /**
     * URL 链接校验 (Http/Https/Ftp)
     */
    public static final String REGEX_URL = "^(https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$";
    private static final Pattern PATTERN_URL = Pattern.compile(REGEX_URL);

    /**
     * IPv4 地址校验
     */
    public static final String REGEX_IPV4 = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";
    private static final Pattern PATTERN_IPV4 = Pattern.compile(REGEX_IPV4);


    // =========================================================================
    // 通用校验方法 (Public Methods)
    // =========================================================================

    /**
     * 校验是否为有效的电话号码
     * 只要是 手机号 OR 座机 OR 服务号 任意一种，即返回 true
     */
    public static boolean isPhone(CharSequence input) {
        return isMobile(input) || isLandline(input) || isServiceNumber(input);
    }

    /**
     * 仅校验 11位手机号
     */
    public static boolean isMobile(CharSequence input) {
        return matches(PATTERN_MOBILE, input);
    }

    /**
     * 仅校验 固定电话 (带区号)
     */
    public static boolean isLandline(CharSequence input) {
        return matches(PATTERN_LANDLINE, input);
    }

    /**
     * 仅校验 服务号 (12345, 10086, 400xxx)
     */
    public static boolean isServiceNumber(CharSequence input) {
        return matches(PATTERN_SERVICE_NUM, input);
    }

    /**
     * 校验邮箱
     */
    public static boolean isEmail(CharSequence input) {
        return matches(PATTERN_EMAIL, input);
    }

    /**
     * 校验18位身份证格式
     */
    public static boolean isIdCard18(CharSequence input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        if (input.length() == 15) {
            return matches(PATTERN_ID_CARD_15, input);
        } else if (input.length() == 18) {
            return matches(PATTERN_ID_CARD_18, input);
        } else {
            return false;
        }
    }

    /**
     * 校验是否为强密码 (8-20位，含大小写字母+数字)
     */
    public static boolean isStrongPassword(CharSequence input) {
        return matches(PATTERN_PASSWORD_STRONG, input);
    }

    /**
     * 校验是否全是中文
     */
    public static boolean isChinese(CharSequence input) {
        return matches(PATTERN_CHINESE, input);
    }

    /**
     * 校验是否为整数
     */
    public static boolean isInteger(CharSequence input) {
        return matches(PATTERN_INTEGER, input);
    }

    /**
     * 校验是否为URL
     */
    public static boolean isUrl(CharSequence input) {
        return matches(PATTERN_URL, input);
    }

    /**
     * 校验是否为IPv4地址
     */
    public static boolean isIpv4(CharSequence input) {
        return matches(PATTERN_IPV4, input);
    }

    /**
     * 通用正则匹配方法
     * @param regex 正则表达式字符串
     * @param input 待校验的字符串
     * @return 是否匹配
     */
    public static boolean match(String regex, CharSequence input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return Pattern.matches(regex, input);
    }

    // =========================================================================
    // 私有辅助方法
    // =========================================================================

    /**
     * 内部使用的正则匹配，使用预编译的Pattern以提高性能
     */
    private static boolean matches(Pattern pattern, CharSequence input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return pattern.matcher(input).matches();
    }
}