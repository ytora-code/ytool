package xyz.ytora.ytool.document.excel;

import xyz.ytora.ytool.document.DocException;
import xyz.ytora.ytool.document.excel.read.DefaultExcelReader;
import xyz.ytora.ytool.document.excel.read.IExcelReader;
import xyz.ytora.ytool.document.excel.write.DefaultExcelWriter;
import xyz.ytora.ytool.document.excel.write.IExcelWriter;

import java.io.*;
import java.util.List;

/**
 * created by yangtong on 2025/5/30 11:25:38
 * <br/>
 * Excel操作静态工具类
 */
public class Excels {

    /*=======================================Excel读取=================================================*/

    /**
     * 从指定Excel文件excelFile中读取数据，并将读取到的数据转换成数组返回
     * @param excelFile excel文件对象
     * @param clazz 要转换成的类型
     * @return excel读取结果
     * @param <T> 要转换成的类型
     */
    public static <T> List<T> read(File excelFile, Class<T> clazz) {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(excelFile))) {
            return read(clazz, bis);
        } catch (IOException e) {
            throw new DocException(e);
        }
    }

    /**
     * 从指定输入流中读取数据，并将读取到的数据转换成数组返回
     * @param inputStream excel输入流
     * @param clazz 要转换成的类型
     * @return excel读取结果
     * @param <T> 要转换成的类型
     */
    public static <T> List<T> read(Class<T> clazz, InputStream inputStream) {
        ExcelConfig<T> config = new ExcelConfig<>(clazz);
        return read(inputStream, config);
    }

    /**
     * 从指定输入流中读取数据，并将读取到的数据转换成数组返回，使用者可以自己指定ExcelConfig配置信息
     * @param inputStream excel输入流
     * @param config 读取Excel时的配置信息
     * @return excel读取结果
     * @param <T> 要转换成的类型
     */
    public static <T> List<T> read(InputStream inputStream, ExcelConfig<T> config) {
        IExcelReader<T> reader = new DefaultExcelReader<>(config);
        //doRead会关闭inputStream，所以调用者不用处理inputStream
        return reader.doRead(inputStream);
    }

    /*=======================================Excel写入=================================================*/

    /**
     * 将指定类型的数组data写入Excel文件
     * @param data 数据
     * @param excelFile Excel文件对象
     * @param <T> 指定类型
     */
    public static <T> void write(Class<T> clazz, List<T> data, File excelFile) {
        check(excelFile);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(excelFile))) {
            write(clazz, data, bos);
            bos.flush();
        } catch (IOException e) {
            throw new DocException(e);
        }
    }

    /**
     * 将指定类型的数组data写入Excel输出流out
     * @param data 数据
     * @param out Excel输出流
     * @param <T> 指定类型
     */
    public static <T> void write(Class<T> clazz, List<T> data, OutputStream out) {
        if (out == null) {
            throw new DocException("写出的输出流不能为空");
        }
        ExcelConfig<T> config = new ExcelConfig<>(clazz)
                .setOut(out)
                .setCount(data.size());
        write(data, config);
    }

    /**
     * 将指定类型的数组data写入Excel输出流,使用者可以自己指定ExcelConfig配置信息
     * @param data 数据
     * @param config 写入Excel时的配置信息
     * @param <T> 指定类型
     */
    public static <T> void write(List<T> data, ExcelConfig<T> config) {
        IExcelWriter<T> writer = new DefaultExcelWriter<>(config);
        writer.doWrite(data);
    }

    private static void check(File excelFile) {
        File parent = excelFile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (parent.mkdirs()) {
                System.out.println("父目录不存在，已创建");
            } else {
                throw new DocException("文件目录创建失败");
            }
        }
    }

}
