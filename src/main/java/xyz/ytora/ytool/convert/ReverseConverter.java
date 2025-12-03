package xyz.ytora.ytool.convert;

/**
 * created by yangtong on 2025/4/4 下午5:17
 * 逆向包装类
 */
public class ReverseConverter<S, T> implements Converter<S, T>  {

    private final Converter<T, S> converter;

    public ReverseConverter(Converter<T, S> converter) {
        this.converter = converter;
    }


    @Override
    public T convert(S source) {
        return converter.reverseConvert(source);
    }

    @Override
    public S reverseConvert(T source) {
        return converter.convert(source);
    }
}
