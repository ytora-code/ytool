package xyz.ytora.ytool.document.excel.factory;

import xyz.ytora.ytool.document.excel.IExcelFieldHandler;
import xyz.ytora.ytool.invoke.Reflects;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * created by yangtong on 2025/5/30 10:53:25
 * <br/>
 * 默认的ExcelFieldHandler工厂
 */
public class DefaultExcelFieldHandlerFactory implements ExcelFieldHandlerFactory {

    private final Map<Class<? extends IExcelFieldHandler<?, ?, ?>>, IExcelFieldHandler<?, ?, ?>> handlerCache = new ConcurrentHashMap<>();
    private static final ExcelFieldHandlerFactory DEFAULT_FACTORY = new DefaultExcelFieldHandlerFactory();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IExcelFieldHandler<?, ?, ?>> T getHandler(Class<T> handlerClass) {

        try {
            //首先从缓存中获取
            IExcelFieldHandler<?, ?, ?> excelFieldHandler = handlerCache.get(handlerClass);
            if (excelFieldHandler != null) {
                return (T) excelFieldHandler;
            }
            //缓存中没有则创建
            IExcelFieldHandler<Object, Object, Object> handler =
                    Reflects.newInstance(handlerClass.asSubclass(IExcelFieldHandler.class));
            handlerCache.put(handlerClass, handler);

            return (T) handler;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static ExcelFieldHandlerFactory getInstance() {
        return DEFAULT_FACTORY;
    }
}
