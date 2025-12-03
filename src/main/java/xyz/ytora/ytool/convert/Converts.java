package xyz.ytora.ytool.convert;

import xyz.ytora.ytool.convert.support.DefaultConversionService;

/**
 * created by yangtong on 2025/4/4 下午5:58
 * <类型转换工具类/>
 */
public class Converts {

    private static final DefaultConversionService conversionService;

    static {
        String basePackage = "org.ytor.common.util.convert.converters";
        conversionService = DefaultConversionService.init(basePackage);
    }

    public static DefaultConversionService get() {
        return conversionService;
    }

    /**
     * 将原数据转为目标类型
     */
    public static <T> T convert(Object source, Class<T> targetType) {
        return conversionService.convert(source, targetType);
    }

    /**
     * 将原数据转为目标类型，转换失败则返回默认值
     */
    public static <T> T convert(Object source, Class<T> targetType, T defaultValue) {
        try {
            T result = convert(source, targetType);
            if (result == null) {
                return defaultValue;
            }
            return result;
        } catch (Exception e) {
            System.err.println("转换失败，使用默认值" + e);
            return defaultValue;
        }
    }

}
