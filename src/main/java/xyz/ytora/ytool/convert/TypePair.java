package xyz.ytora.ytool.convert;

import java.util.Objects;

/**
 * created by yangtong on 2025/4/4 下午4:56
 * <br/>
 * 类型对包装类
 */
public record TypePair(Class<?> sourceType, Class<?> targetType) {
    public Class<?> getSourceType() {
        return sourceType;
    }

    public Class<?> getTargetType() {
        return targetType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypePair pair)) return false;
        return sourceType.equals(pair.sourceType) && targetType.equals(pair.targetType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceType, targetType);
    }
}
