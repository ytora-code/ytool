package xyz.ytora.ytool.convert.support;

import xyz.ytora.ytool.convert.*;
import xyz.ytora.ytool.number.Numbers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * created by yangtong on 2025/4/4 下午4:59
 * 默认的类型转换器
 */
public class DefaultConversionService implements ConverterRegistry, ConversionService {
    private final Map<TypePair, Converter<?, ?>> converterMap = new ConcurrentHashMap<>();

    public static DefaultConversionService init(String basePackage) {
        DefaultConversionService service = new DefaultConversionService();
        String path = basePackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            // 使用 getResources 获取所有可能的路径（包括依赖包里的）
            Enumeration<URL> resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();

                if ("file".equals(protocol)) {
                    // 处理本地开发环境
                    scanFromDirectory(service, basePackage, new File(resource.toURI()));
                } else if ("jar".equals(protocol)) {
                    // 处理 JAR 包环境
                    scanFromJar(service, basePackage, resource);
                }
            }
        } catch (Exception e) {
            throw new ConverterException("初始化转换器失败", e);
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
     * 扫描本地目录
     */
    private static void scanFromDirectory(DefaultConversionService service, String basePackage, File directory) throws Exception {
        if (!directory.exists()) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // 如果需要递归扫描，可以递归调用，这里演示单层
                continue;
            }
            String fileName = file.getName();
            if (fileName.endsWith(".class")) {
                String className = basePackage + "." + fileName.replace(".class", "");
                registerClass(service, className);
            }
        }
    }

    /**
     * 扫描 JAR 包
     */
    private static void scanFromJar(DefaultConversionService service, String basePackage, URL resource) throws IOException {
        JarURLConnection jarURLConnection = (JarURLConnection) resource.openConnection();
        try (JarFile jarFile = jarURLConnection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            String path = basePackage.replace('.', '/');

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                // 查找以包路径开头且以 .class 结尾的文件
                if (name.startsWith(path) && name.endsWith(".class")) {
                    // 排除目录本身和内部类（根据需求）
                    if (name.contains("$") || name.endsWith("/")) continue;

                    String className = name.replace("/", ".").replace(".class", "");
                    registerClass(service, className);
                }
            }
        }
    }

    /**
     * 核心注册逻辑
     */
    @SuppressWarnings("unchecked")
    private static void registerClass(DefaultConversionService service, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (Converter.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
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
            // 记录日志或抛出异常
            System.err.println("加载类失败: " + className);
        }
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
