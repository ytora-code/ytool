package xyz.ytora.ytool.document.excel.write;

import java.util.List;

/**
 * Excel 写出
 */
public interface IExcelWriter<T> {

    /**
     * 将内存数据写出 EXCEL
     * @param data 数据
     */
    void doWrite(List<T> data);

}
