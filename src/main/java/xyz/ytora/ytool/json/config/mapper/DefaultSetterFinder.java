package xyz.ytora.ytool.json.config.mapper;

import xyz.ytora.ytool.classcache.classmeta.MethodMetadata;
import xyz.ytora.ytool.str.Strs;

import java.util.Map;

/**
 * created by yangtong on 2025/8/19 10:27:24
 * <br/>
 * 默认查找策略，先根据字段名称直接查找，没找到再将字段名称变成下划线查找
 */
public class DefaultSetterFinder implements SetterFinder {
    @Override
    public MethodMetadata getSetter(String name, Map<String, MethodMetadata> setters) {
        MethodMetadata methodMetadata = setters.get(name);
        if (methodMetadata == null) {
            // 如果没找到，则JSON字段有可能是下划线小写形式，尝试转为小驼峰，再次匹配
            methodMetadata = setters.get(Strs.toLowerCamelCase(name));
        }
        return methodMetadata;
    }
}
