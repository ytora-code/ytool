package xyz.ytora.ytool.document.excel.factory;

import xyz.ytora.ytool.document.excel.IExcelFieldHandler;

/**
 * created by yangtong on 2025/5/30 10:51:16
 * <br/>
 * IExcelFieldHandler处理器工厂
 */
public interface ExcelFieldHandlerFactory {

    /**
     * 获取handler
     */
    <T extends IExcelFieldHandler<?, ?, ?>> T getHandler(Class<T> handlerClass);

}
