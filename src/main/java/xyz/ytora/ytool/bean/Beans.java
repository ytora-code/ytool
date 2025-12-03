package xyz.ytora.ytool.bean;

import xyz.ytora.ytool.classcache.ClassCache;
import xyz.ytora.ytool.classcache.classmeta.ClassMetadata;
import xyz.ytora.ytool.classcache.classmeta.FieldMetadata;
import xyz.ytora.ytool.classcache.classmeta.MethodMetadata;
import xyz.ytora.ytool.convert.Converts;
import xyz.ytora.ytool.invoke.Reflects;
import xyz.ytora.ytool.str.Strs;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * created by yangtong on 2025/4/4 下午6:40
 * <br/>
 * Bean 操作工具类
 */
public class Beans {

    /**
     * 将source对象的字段复制到target字段
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyProperties(Object source, Object target, String... ignoreFieldNames) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("源对象或目标对象不能为空");
        }
        Set<String> ignoreSet = Arrays.stream(ignoreFieldNames).collect(Collectors.toSet());
        Class<?> sourceClass = source.getClass();
        Class<?> targetClass = target.getClass();

        //获取源对象的所有字段
        ClassMetadata<?> classMetadata = ClassCache.get(sourceClass);
        List<FieldMetadata> sourceFieldMetas = classMetadata.getFields();
        for (FieldMetadata sourceFieldMeta : sourceFieldMetas) {
            Field sourceField = sourceFieldMeta.getSourceField();
            String fieldName = sourceField.getName();
            if (ignoreSet.contains(fieldName)) {
                continue;
            }
            //获取该字段的get方法和set方法
            String capitalized = Strs.firstCapitalize(fieldName);
            String getterName = "get" + capitalized;
            String setterName = "set" + capitalized;

            try {
                //判断该字段是否有getter方法
                MethodMetadata getterMethod = ClassCache.getMethod(sourceClass, getterName);
                if (getterMethod == null) {
                    // 如果该字段是 boolean 类型可能是 isXxx
                    if (sourceField.getType() == boolean.class || sourceField.getType() == Boolean.class) {
                        getterMethod = ClassCache.getMethod(sourceClass, "is" + capitalized);
                    }
                }
                //如果还是没有，则跳过
                if (getterMethod == null) {
                    continue;
                }

                //走到这，说明该源字段肯定有getter方法
                Object value = getterMethod.invoke(source);
                // 不复制 null 值
                if (value == null) continue;

                //查找目标对象里面的同名字段
                FieldMetadata targetField = ClassCache.getField(targetClass, fieldName);
                //目标对象无该字段，略过
                if (targetField == null) continue;

                //检查类型兼容性，源对象的字段是否能否赋值给目标字段
                if (!targetField.getSourceField().getType().isAssignableFrom(value.getClass())) {
                    // 如果不兼容，则尝试使用类型转换器
                    try {
                        value = Converts.convert(value, targetField.getType());
                    } catch (Exception e) {
                        // 转换失败，则跳过改字段
                        continue;
                    }
                }

                //如果兼容，则调用目标字段的setter方法调用
                MethodMetadata setterMethod = ClassCache.getMethod(targetClass, setterName, targetField.getSourceField().getType());
                setterMethod.invoke(target, value);
            } catch (Exception e) {
                //TODO 打印日志或记录异常
                //log.error("属性拷贝出错：{}", e.getMessage());
            }
        }
    }

    /**
     * 将source对象的字段复制到target字段
     *
     * @param source 源对象
     * @param targetClass 目标对象类型
     */
    public static <T> T copyProperties(Object source, Class<T> targetClass, String... ignoreFieldNames) {
        if (source == null || targetClass == null) {
            throw new IllegalArgumentException("源对象或目标类型不能为空");
        }
        try {
            // 通过反射创建目标对象实例（要求有无参构造）
            T target = Reflects.newInstance(targetClass);
            // 调用已有的方法进行字段复制
            copyProperties(source, target, ignoreFieldNames);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("无法创建目标对象：" + e.getMessage(), e);
        }
    }

    public static <T, R> List<R> transBean(List<T> sourceList, Class<R> targetClass) {
        return sourceList.stream().map(i -> copyProperties(i, targetClass)).toList();
    }

    /**
     * 得到clazz类型的继承层级，链表前面的元素层级低，后面的元素层级高
     * @param clazz 被解析的类型
     * @return 得到clazz类型的继承层级
     */
    public static List<Class<?>> extendsHierarchy(Class<?> clazz) {
        List<Class<?>> list = new LinkedList<>();
        while (clazz != null) {
            list.add(clazz);
            clazz = clazz.getSuperclass();
        }
        return list;
    }

    /**
     * 判断对象是否属于基本类型
     */
    public static boolean isPrimitiveWrapper(Object obj) {
        return obj instanceof Integer ||
                obj instanceof Double ||
                obj instanceof Boolean ||
                obj instanceof Character ||
                obj instanceof Byte ||
                obj instanceof Short ||
                obj instanceof Long ||
                obj instanceof Float;
    }
}
