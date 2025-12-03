package xyz.ytora.ytool.convert.support;

import xyz.ytora.ytool.convert.*;
import xyz.ytora.ytool.number.Numbers;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * created by yangtong on 2025/4/4 下午4:59
 * 默认的类型转换器
 */
public class DefaultConversionService implements ConverterRegistry, ConversionService {
    private final Map<TypePair, Converter<?, ?>> converterMap = new ConcurrentHashMap<>();

    public static DefaultConversionService init(String basePackage) {
        return init(basePackage, () -> {
            String path = basePackage.replace('.', '/');
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(path);
            if (resource == null) {
                throw new ConverterException("未找到包路径：" + path);
            }
            return classLoader.getResource(path);
        });
    }

    public static DefaultConversionService init(String basePackage, Supplier<URL> supplier) {
        DefaultConversionService service = new DefaultConversionService();
        URL resource = supplier.get();

        File directory = new File(resource.getFile());
        if (!directory.exists()) {
            throw new ConverterException("目录不存在：" + directory.getAbsolutePath());
        }

        for (File file : Objects.requireNonNull(directory.listFiles())) {
            try {
                String className = basePackage + "." + file.getName().replace(".class", "");
                Class<?> clazz = Class.forName(className);

                // 必须是 Converter 的实现类，排除接口本身
                if (Converter.class.isAssignableFrom(clazz) && !clazz.isInterface()) {

                    // 尝试推断泛型类型
                    Type[] genericInterfaces = clazz.getGenericInterfaces();
                    for (Type type : genericInterfaces) {
                        if (type instanceof ParameterizedType pt) {
                            if (pt.getRawType() == Converter.class) {
                                Class<?> sourceType = (Class<?>) pt.getActualTypeArguments()[0];
                                Class<?> targetType = (Class<?>) pt.getActualTypeArguments()[1];
                                // 实例化并注册
                                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();
                                service.addConverter(sourceType, targetType, converter);
                                System.out.println("注册转换器：" + clazz.getSimpleName());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new ConverterException("加载类失败：" + file.getName());
            }
        }

        return service;
    }

    /**
     * 进行类型转换
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T convert(Object source, Class<T> targetType) {
        // 如果为空直接返回
        if (source == null) {
            return null;
        }

        // 如果类型已经匹配，直接返回
        if (targetType.isInstance(source)) {
            return (T) source;
        }

        // 数字类型直接在这里快速处理
        if (source instanceof Number && Number.class.isAssignableFrom(targetType)
                || targetType.isPrimitive() && Numbers.isPrimitive(targetType)) {
            return (T) convertNumber((Number) source, targetType);
        }

        //使用注册的类型转换器进行转换
        Class<?> sourceType = source.getClass();

        for (TypePair typePair : converterMap.keySet()) {
            if (typePair.getSourceType().isAssignableFrom(sourceType) &&
                    typePair.getTargetType().isAssignableFrom(targetType)) {
                Converter<Object, T> converter = (Converter<Object, T>) converterMap.get(typePair);
                return converter.convert(source);
            }
        }

        // 转换失败
        throw new ClassCastException("无法进行类型转换： " + sourceType.getName() + " -> " + targetType.getName());
    }

    /**
     * 注册类型转换器
     */
    @Override
    public <S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Converter<S, T> converter) {
        converterMap.put(new TypePair(sourceType, targetType), converter);
        converterMap.put(new TypePair(targetType, sourceType), new ReverseConverter<>(converter));
    }

    /**
     * 数字类型直接转换，不用调用底层的转换组件
     */
    private Object convertNumber(Number number, Class<?> targetType) {
        if (targetType == int.class || targetType == Integer.class) {
            return number.intValue();
        } else if (targetType == long.class || targetType == Long.class) {
            return number.longValue();
        } else if (targetType == double.class || targetType == Double.class) {
            return number.doubleValue();
        } else if (targetType == float.class || targetType == Float.class) {
            return number.floatValue();
        } else if (targetType == short.class || targetType == Short.class) {
            return number.shortValue();
        } else if (targetType == byte.class || targetType == Byte.class) {
            return number.byteValue();
        } else if (targetType == java.math.BigInteger.class) {
            return java.math.BigInteger.valueOf(number.longValue());
        } else if (targetType == java.math.BigDecimal.class) {
            return new java.math.BigDecimal(number.toString());
        }
        throw new IllegalArgumentException("不支持的数字类型: " + targetType.getName());
    }
}
