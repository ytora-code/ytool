package xyz.ytora.ytool.document.excel;

import java.io.IOException;
import java.io.InputStream;

/**
 * created by yangtong on 2025/5/30 11:56:33
 * <br/>
 * Excel文件版本对应的poi类
 */
public enum ExcelVersion {
    /**
     * 03版本的xls，HSSFWorkbook
     */
    V03,
    /**
     * 07版本的xlsx，XSSFWorkbook
     */
    V07,
    /**
     * 07版本，针对大数据优化的 SXSSFWorkbook，仅支持写
     */
    V07_PLUS;

    /**
     * 根据输入流，解析文件版本
     */
    public static ExcelVersion parseVersion(InputStream is) throws IOException {
        if (!is.markSupported()) {
            throw new IOException("输入流不支持 mark/reset，无法解析版本");
        }

        // 通过前八个字节判断魔术
        byte[] buffer = new byte[8];

        is.mark(buffer.length);
        int readSize = is.read(buffer, 0, buffer.length);
        is.reset();

        if (readSize < 8) {
            throw new IOException("无法读取足够的文件头信息，非法的 Excel 文件");
        }

        // 魔数判断
        // xls: D0 CF 11 E0 A1 B1 1A E1
        if (buffer[0] == (byte) 0xD0 &&
                buffer[1] == (byte) 0xCF &&
                buffer[2] == (byte) 0x11 &&
                buffer[3] == (byte) 0xE0) {
            return V03;
        }

        // xlsx: 50 4B 03 04 (zip 开头)
        if (buffer[0] == (byte) 0x50 &&
                buffer[1] == (byte) 0x4B &&
                buffer[2] == (byte) 0x03 &&
                buffer[3] == (byte) 0x04) {
            return V07;
        }

        throw new IOException("不支持的 Excel 文件类型：未知魔术头");
    }
}
