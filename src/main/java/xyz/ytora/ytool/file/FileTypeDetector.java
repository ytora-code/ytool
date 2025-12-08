package xyz.ytora.ytool.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文件类型检测工具类
 * 基于文件头魔数检测文件真实类型，特别处理Office文档等复杂格式
 */
public class FileTypeDetector {

    private static final Logger log = LoggerFactory.getLogger(FileTypeDetector.class);

    // 基础魔数映射
    private static final Map<String, String> MAGIC_NUMBERS = new LinkedHashMap<>();

    // Office文档特征文件映射
    private static final Map<String, String> OFFICE_SIGNATURES = new HashMap<>();

    // 压缩包内特征文件映射
    private static final Map<String, String> ZIP_BASED_FORMATS = new HashMap<>();

    static {
        initMagicNumbers();
        initOfficeSignatures();
        initZipBasedFormats();
    }

    private static void initMagicNumbers() {
        // 图片格式
        MAGIC_NUMBERS.put("89504E470D0A1A0A", "image/png");           // PNG
        MAGIC_NUMBERS.put("FFD8FF", "image/jpeg");                    // JPEG
        MAGIC_NUMBERS.put("47494638", "image/gif");                   // GIF
        MAGIC_NUMBERS.put("424D", "image/bmp");                       // BMP
        MAGIC_NUMBERS.put("49492A00", "image/tiff");                  // TIFF (Little Endian)
        MAGIC_NUMBERS.put("4D4D002A", "image/tiff");                  // TIFF (Big Endian)
        MAGIC_NUMBERS.put("52494646", "image/webp");                  // WebP (需要进一步验证)
        MAGIC_NUMBERS.put("00000100", "image/ico");                   // ICO
        MAGIC_NUMBERS.put("00000200", "image/cur");                   // CUR

        // 音频格式
        MAGIC_NUMBERS.put("494433", "audio/mpeg");                    // MP3
        MAGIC_NUMBERS.put("4F676753", "audio/ogg");                   // OGG
        MAGIC_NUMBERS.put("664C6143", "audio/flac");                  // FLAC
        MAGIC_NUMBERS.put("4D546864", "audio/midi");                  // MIDI
        MAGIC_NUMBERS.put("52494646", "audio/wav");                   // WAV/RIFF (需要进一步检查)

        // 视频格式
        MAGIC_NUMBERS.put("000001BA", "video/mpeg");                  // MPEG
        MAGIC_NUMBERS.put("000001B3", "video/mpeg");                  // MPEG
        MAGIC_NUMBERS.put("1A45DFA3", "video/webm");                  // WebM/Matroska
        MAGIC_NUMBERS.put("52494646", "video/avi");                   // AVI (需要进一步检查)

        // 文档格式
        MAGIC_NUMBERS.put("25504446", "application/pdf");             // PDF
        MAGIC_NUMBERS.put("7B5C727466", "application/rtf");           // RTF
        MAGIC_NUMBERS.put("EFBBBF", "text/plain");                    // UTF-8 BOM
        MAGIC_NUMBERS.put("FFFE", "text/plain");                      // UTF-16 LE BOM
        MAGIC_NUMBERS.put("FEFF", "text/plain");                      // UTF-16 BE BOM

        // 压缩格式
        MAGIC_NUMBERS.put("504B0304", "application/zip");             // ZIP
        MAGIC_NUMBERS.put("504B0506", "application/zip");             // ZIP (empty)
        MAGIC_NUMBERS.put("504B0708", "application/zip");             // ZIP (spanned)
        MAGIC_NUMBERS.put("52617221", "application/x-rar");           // RAR v1.5+
        MAGIC_NUMBERS.put("526172211A0700", "application/x-rar");     // RAR v1.5+
        MAGIC_NUMBERS.put("526172211A070100", "application/x-rar");   // RAR v5.0+
        MAGIC_NUMBERS.put("377ABCAF271C", "application/x-7z-compressed"); // 7Z
        MAGIC_NUMBERS.put("1F8B08", "application/gzip");              // GZIP
        MAGIC_NUMBERS.put("425A68", "application/x-bzip2");           // BZIP2
        MAGIC_NUMBERS.put("FD377A585A00", "application/x-xz");        // XZ

        // 可执行文件
        MAGIC_NUMBERS.put("4D5A", "application/x-msdownload");        // EXE/DLL
        MAGIC_NUMBERS.put("7F454C46", "application/x-executable");    // ELF
        MAGIC_NUMBERS.put("CAFEBABE", "application/java-archive");    // Java Class
        MAGIC_NUMBERS.put("FEEDFACE", "application/x-mach-binary");   // Mach-O

        // 旧版Office格式
        MAGIC_NUMBERS.put("D0CF11E0A1B11AE1", "application/vnd.ms-office"); // OLE2 (DOC/XLS/PPT)

        // 其他格式
        MAGIC_NUMBERS.put("213C617263683E", "application/x-unix-archive"); // Unix Archive
        MAGIC_NUMBERS.put("EDABEEDB", "application/vnd.rpm");         // RPM
        MAGIC_NUMBERS.put("1F9E", "application/x-gzip");              // Old GZIP
    }

    private static void initOfficeSignatures() {
        // 基于ZIP的Office格式内部特征
        OFFICE_SIGNATURES.put("[Content_Types].xml", "office_base");
        OFFICE_SIGNATURES.put("word/document.xml", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        OFFICE_SIGNATURES.put("xl/workbook.xml", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        OFFICE_SIGNATURES.put("ppt/presentation.xml", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        OFFICE_SIGNATURES.put("word/", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        OFFICE_SIGNATURES.put("xl/", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        OFFICE_SIGNATURES.put("ppt/", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    }

    private static void initZipBasedFormats() {
        // 其他基于ZIP的格式
        ZIP_BASED_FORMATS.put("META-INF/MANIFEST.MF", "application/java-archive"); // JAR
        ZIP_BASED_FORMATS.put("AndroidManifest.xml", "application/vnd.android.package-archive"); // APK
        ZIP_BASED_FORMATS.put("META-INF/container.xml", "application/epub+zip"); // EPUB
        ZIP_BASED_FORMATS.put("mimetype", "application/epub+zip"); // EPUB
        ZIP_BASED_FORMATS.put("content.xml", "application/vnd.oasis.opendocument.text"); // ODT/ODS/ODP
        ZIP_BASED_FORMATS.put("META-INF/manifest.xml", "application/vnd.oasis.opendocument.text"); // LibreOffice
    }

    /**
     * 检测文件类型 - 从文件路径
     */
    public static String detectFileType(Path filePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return detectFileType(inputStream);
        }
    }

    /**
     * 检测文件类型 - 从输入流
     */
    public static String detectFileType(InputStream inputStream) throws IOException {
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }

        // 读取前64个字节进行基础检测
        byte[] buffer = new byte[64];
        inputStream.mark(buffer.length);
        int bytesRead = inputStream.read(buffer);
        inputStream.reset();

        if (bytesRead <= 0) {
            return "application/octet-stream";
        }

        // 转换为十六进制字符串
        String hex = bytesToHex(buffer, bytesRead);

        // 基础魔数检测
        String detectedType = detectByMagicNumber(hex);

        // 特殊处理
        if ("application/zip".equals(detectedType)) {
            // ZIP格式需要进一步检测内容
            String zipBasedType = detectZipBasedFormat(inputStream);
            if (zipBasedType != null) {
                return zipBasedType;
            }
        } else if ("application/vnd.ms-office".equals(detectedType)) {
            // 旧版Office格式进一步检测
            String officeType = detectOldOfficeFormat(inputStream);
            if (officeType != null) {
                return officeType;
            }
        } else if ("audio/wav".equals(detectedType) || "video/avi".equals(detectedType)) {
            // RIFF格式需要进一步检测
            String riffType = detectRiffFormat(buffer);
            if (riffType != null) {
                return riffType;
            }
        } else if ("image/webp".equals(detectedType)) {
            // WebP需要验证
            if (!isValidWebP(buffer)) {
                detectedType = "application/octet-stream";
            }
        }

        // 如果没有检测到已知类型，尝试文本检测
        if ("application/octet-stream".equals(detectedType)) {
            if (isTextFile(buffer, bytesRead)) {
                return detectTextFormat(inputStream);
            }
        }

        return detectedType != null ? detectedType : "application/octet-stream";
    }

    /**
     * 基于魔数检测文件类型
     */
    private static String detectByMagicNumber(String hex) {
        for (Map.Entry<String, String> entry : MAGIC_NUMBERS.entrySet()) {
            if (hex.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "application/octet-stream";
    }

    /**
     * 检测ZIP基础格式（Office文档、JAR、APK等）
     */
    private static String detectZipBasedFormat(InputStream inputStream) {
        try {
            // 重置流到开始位置
            inputStream.mark(Integer.MAX_VALUE);

            try (ZipInputStream zipStream = new ZipInputStream(inputStream)) {
                ZipEntry entry;
                Set<String> entryNames = new HashSet<>();

                // 读取ZIP条目
                while ((entry = zipStream.getNextEntry()) != null && entryNames.size() < 50) {
                    String entryName = entry.getName();
                    entryNames.add(entryName);

                    // 检查Office文档特征
                    for (Map.Entry<String, String> signature : OFFICE_SIGNATURES.entrySet()) {
                        if (entryName.equals(signature.getKey()) ||
                                (signature.getKey().endsWith("/") && entryName.startsWith(signature.getKey()))) {
                            return signature.getValue();
                        }
                    }

                    // 检查其他ZIP基础格式
                    for (Map.Entry<String, String> format : ZIP_BASED_FORMATS.entrySet()) {
                        if (entryName.equals(format.getKey())) {
                            return format.getValue();
                        }
                    }

                    zipStream.closeEntry();
                }

                // 基于条目组合判断
                if (entryNames.contains("[Content_Types].xml")) {
                    // 这是Office 2007+格式，但无法确定具体类型
                    if (entryNames.stream().anyMatch(name -> name.startsWith("word/"))) {
                        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    } else if (entryNames.stream().anyMatch(name -> name.startsWith("xl/"))) {
                        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    } else if (entryNames.stream().anyMatch(name -> name.startsWith("ppt/"))) {
                        return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                    }
                }

                // LibreOffice/OpenOffice格式
                if (entryNames.contains("META-INF/manifest.xml") && entryNames.contains("content.xml")) {
                    if (entryNames.contains("styles.xml")) {
                        return "application/vnd.oasis.opendocument.text"; // 可能是ODT/ODS/ODP，需要更详细检测
                    }
                }

            }
        } catch (IOException e) {
            log.debug("检测ZIP格式时出错: {}", e.getMessage());
        } finally {
            try {
                inputStream.reset();
            } catch (IOException e) {
                log.debug("重置流时出错: {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * 检测旧版Office格式（基于OLE2）
     */
    private static String detectOldOfficeFormat(InputStream inputStream) {
        try {
            inputStream.mark(8192);
            byte[] buffer = new byte[8192];
            int bytesRead = inputStream.read(buffer);

            if (bytesRead > 0) {
                String content = new String(buffer, 0, bytesRead, "ISO-8859-1");

                if (content.contains("Microsoft Office Word") || content.contains("Word.Document")) {
                    return "application/msword";
                } else if (content.contains("Microsoft Office Excel") || content.contains("Excel.Sheet")) {
                    return "application/vnd.ms-excel";
                } else if (content.contains("Microsoft Office PowerPoint") || content.contains("PowerPoint.Show")) {
                    return "application/vnd.ms-powerpoint";
                }
            }
        } catch (IOException e) {
            log.debug("检测旧版Office格式时出错: {}", e.getMessage());
        } finally {
            try {
                inputStream.reset();
            } catch (IOException e) {
                log.debug("重置流时出错: {}", e.getMessage());
            }
        }

        return "application/vnd.ms-office";
    }

    /**
     * 检测RIFF格式（WAV、AVI等）
     */
    private static String detectRiffFormat(byte[] buffer) {
        if (buffer.length >= 12) {
            // RIFF文件格式：RIFF + 4字节大小 + 格式标识
            String riffHeader = bytesToHex(buffer, 12);
            if (riffHeader.startsWith("52494646")) { // "RIFF"
                String formatType = riffHeader.substring(16, 24); // 8-12字节是格式标识
                switch (formatType) {
                    case "57415645": // "WAVE"
                        return "audio/wav";
                    case "41564920": // "AVI "
                        return "video/avi";
                    case "57454250": // "WEBP"
                        return "image/webp";
                    default:
                        return "application/octet-stream";
                }
            }
        }
        return null;
    }

    /**
     * 验证WebP格式
     */
    private static boolean isValidWebP(byte[] buffer) {
        if (buffer.length >= 12) {
            String header = bytesToHex(buffer, 12);
            return header.startsWith("52494646") && header.substring(16, 24).equals("57454250");
        }
        return false;
    }

    /**
     * 检测文本格式
     */
    private static String detectTextFormat(InputStream inputStream) {
        try {
            inputStream.mark(1024);
            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);

            if (bytesRead > 0) {
                String content = new String(buffer, 0, Math.min(bytesRead, 512)).toLowerCase();

                if (content.contains("<!doctype html") || content.contains("<html")) {
                    return "text/html";
                } else if (content.contains("<?xml")) {
                    return "application/xml";
                } else if (content.startsWith("{") || content.startsWith("[")) {
                    // 简单的JSON检测
                    return "application/json";
                } else if (content.contains("@charset") || content.contains("body{") || content.contains("margin:")) {
                    return "text/css";
                } else if (content.contains("function ") || content.contains("var ") || content.contains("console.")) {
                    return "application/javascript";
                }
            }
        } catch (Exception e) {
            log.debug("检测文本格式时出错: {}", e.getMessage());
        } finally {
            try {
                inputStream.reset();
            } catch (IOException e) {
                log.debug("重置流时出错: {}", e.getMessage());
            }
        }

        return "text/plain";
    }

    /**
     * 判断是否为文本文件
     */
    private static boolean isTextFile(byte[] buffer, int length) {
        int textChars = 0;
        int binaryChars = 0;

        for (int i = 0; i < length; i++) {
            byte b = buffer[i];
            if ((b >= 32 && b <= 126) || b == 9 || b == 10 || b == 13) {
                textChars++;
            } else if (b < 32 && b != 9 && b != 10 && b != 13) {
                binaryChars++;
            }
        }

        // 如果二进制字符超过20%，认为是二进制文件
        return binaryChars * 5 < textChars;
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes, int length) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            hexString.append(String.format("%02X", bytes[i] & 0xFF));
        }
        return hexString.toString();
    }

    /**
     * 获取文件类型的描述性名称
     */
    public static String getFileTypeDescription(String mimeType) {
        return switch (mimeType) {
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    "Microsoft Word文档 (DOCX)";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "Microsoft Excel工作表 (XLSX)";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" ->
                    "Microsoft PowerPoint演示文稿 (PPTX)";
            case "application/msword" -> "Microsoft Word文档 (DOC)";
            case "application/vnd.ms-excel" -> "Microsoft Excel工作表 (XLS)";
            case "application/vnd.ms-powerpoint" -> "Microsoft PowerPoint演示文稿 (PPT)";
            case "application/pdf" -> "PDF文档";
            case "image/jpeg" -> "JPEG图片";
            case "image/png" -> "PNG图片";
            case "image/gif" -> "GIF图片";
            case "image/webp" -> "WebP图片";
            case "application/zip" -> "ZIP压缩包";
            case "application/java-archive" -> "Java归档文件 (JAR)";
            case "application/vnd.android.package-archive" -> "Android应用包 (APK)";
            case "text/plain" -> "纯文本文件";
            case "text/html" -> "HTML文档";
            case "application/json" -> "JSON数据文件";
            case "application/xml" -> "XML文档";
            case "audio/mpeg" -> "MP3音频";
            case "audio/wav" -> "WAV音频";
            case "video/mp4" -> "MP4视频";
            case "video/avi" -> "AVI视频";
            default -> "未知文件类型";
        };
    }
}