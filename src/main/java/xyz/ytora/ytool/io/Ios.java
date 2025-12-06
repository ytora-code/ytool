package xyz.ytora.ytool.io;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * created by yangtong on 2025/4/10 20:44:58
 * <br/>
 * 工具类
 */
public class Ios {
    private static final int BUFFER_SIZE = 8192;

    // 关闭资源
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 复制流，并返回流的长度
     */
    public static long copy(InputStream in, OutputStream out) {
        try {
            long total = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                total += len;
            }
            return total;
        } catch (IOException e) {
            throw new IosException(e);
        }

    }

    /**
     * 复制流并关闭，返回流的长度
     */
    public static long copyAndClose(InputStream in, OutputStream out) {
        try {
            try (in; out) {
                return copy(in, out);
            }
        } catch (IOException e) {
            throw new IosException(e);
        }
    }

    /**
     * 将文件读取成流
     */
    public static InputStream readAsStream(File file) {
        try {
            return new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new IosException(e);
        }
    }

    /**
     * 将文件读取成流
     */
    public static InputStream readAsStream(String filePath) {
        try {
            return new BufferedInputStream(new FileInputStream(filePath));
        } catch (FileNotFoundException e) {
            throw new IosException(e);
        }
    }

    /**
     * InputStream 转 byte[]，会将数据全部读取到内存，谨慎调用
     */
    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    // 写字符串到 OutputStream
    public static void write(String data, OutputStream out, Charset charset) throws IOException {
        try (Writer writer = new OutputStreamWriter(out, charset)) {
            writer.write(data);
        }
    }

    /**
     * 读取文件为字符串，会将数据全部读取到内存，谨慎调用
     */
    public static String readFileToString(Path path, Charset charset) throws IOException {
        return Files.readString(path, charset);
    }

    /**
     * 读取文件为 byte[]，会将数据全部读取到内存，谨慎调用
     */
    public static byte[] readFileToBytes(Path path) throws IOException {
        long fileSize = Files.size(path);
        if (fileSize > 100 * 1024 * 1024) {
            throw new IOException("文件太大: " + fileSize + " bytes (最多能读取100MB)");
        }
        return Files.readAllBytes(path);
    }

    // 写字符串到文件
    public static void writeStringToFile(Path path, String content, Charset charset, boolean append) throws IOException {
        OpenOption option = append ? StandardOpenOption.APPEND : StandardOpenOption.CREATE;
        Files.writeString(path, content, charset, option, StandardOpenOption.WRITE);
    }

    // 写 byte[] 到文件
    public static void writeBytesToFile(Path path, byte[] data, boolean append) throws IOException {
        OpenOption option = append ? StandardOpenOption.APPEND : StandardOpenOption.CREATE;
        Files.write(path, data, option, StandardOpenOption.WRITE);
    }

    // 拷贝文件
    public static void copyFile(Path src, Path dest, boolean overwrite) throws IOException {
        CopyOption option = overwrite ? StandardCopyOption.REPLACE_EXISTING : StandardCopyOption.COPY_ATTRIBUTES;
        Files.copy(src, dest, option);
    }

    // 移动文件
    public static void moveFile(Path src, Path dest, boolean overwrite) throws IOException {
        CopyOption option = overwrite ? StandardCopyOption.REPLACE_EXISTING : StandardCopyOption.ATOMIC_MOVE;
        Files.move(src, dest, option);
    }

    // 删除文件或目录（如果存在）
    public static boolean deleteIfExists(Path path) throws IOException {
        return Files.deleteIfExists(path);
    }

    // 创建多级目录
    public static void createDirectories(Path path) throws IOException {
        Files.createDirectories(path);
    }

    // 解压 zip 文件
    public static void unzip(Path zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = targetDir.resolve(entry.getName()).normalize();
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    createDirectories(outPath.getParent());
                    try (OutputStream out = Files.newOutputStream(outPath)) {
                        copy(zis, out);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    // 将目录压缩为 zip 文件
    public static void zip(Path sourceDir, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            try (Stream<Path> walk = Files.walk(sourceDir)) {
                walk.forEach(path -> {
                    try {
                        if (Files.isDirectory(path)) return;
                        ZipEntry entry = new ZipEntry(sourceDir.relativize(path).toString().replace("\\", "/"));
                        zos.putNextEntry(entry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }

        }
    }

    // 判断流是否为空
    public static boolean isEmpty(InputStream in) throws IOException {
        in.mark(1);
        int read = in.read();
        in.reset();
        return read == -1;
    }

    // InputStream 转 String
    public static String toString(InputStream in, Charset charset) throws IOException {
        try (Reader reader = new InputStreamReader(in, charset)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[BUFFER_SIZE];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        }
    }

    // 默认 UTF-8 版本的字符串方法简化使用
    public static String toString(InputStream in) throws IOException {
        return toString(in, StandardCharsets.UTF_8);
    }

    public static String readFileToString(Path path) throws IOException {
        return readFileToString(path, StandardCharsets.UTF_8);
    }

    public static void writeStringToFile(Path path, String content) throws IOException {
        writeStringToFile(path, content, StandardCharsets.UTF_8, false);
    }

    public static void writeStringToFile(Path path, String content, boolean append) throws IOException {
        writeStringToFile(path, content, StandardCharsets.UTF_8, append);
    }

    /**
     * 从资源目录下读取流
     */
    public static InputStream readStreamFromResource(String resource) throws IOException {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    }
}
