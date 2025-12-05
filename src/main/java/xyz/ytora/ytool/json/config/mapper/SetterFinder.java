package xyz.ytora.ytool.json.config.mapper;

import xyz.ytora.ytool.classcache.classmeta.MethodMetadata;

import java.util.Map;

/**
 * created by yangtong on 2025/8/19 10:13:44
 * <br/>
 * JSON字段 -> POJO字段的映射规则
 * 比如JSON字段是user_name，POJO字段是userName，此时如果要根据JSON字段找到POJO字段，需要映射规则
 */
public interface SetterFinder {

    MethodMetadata getSetter(String name, Map<String, MethodMetadata> setters);

}
