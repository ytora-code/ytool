package xyz.ytora.ytool.document.excel.read;

import java.io.InputStream;
import java.util.List;

/**
 * EXCEL 读入
 */
public interface IExcelReader<T> {

    /**
     * 将 EXCEL 数据读入内存
     * @param is EXCEL 输入流
     * @return 读取到的数据
     */
    List<T> doRead(InputStream is);

}
