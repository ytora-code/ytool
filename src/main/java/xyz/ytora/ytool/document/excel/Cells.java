package xyz.ytora.ytool.document.excel;

import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import xyz.ytora.ytool.document.DocException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * created by yang tong on 2024/6/4 22:04:59
 * <br/>
 * Excel 单元格工具类
 * 关键特性：
 * 1) 样式复用：减少 CellStyle 数量，避免样式上限与文件膨胀。
 * 2) 字符串强制文本：手机号/身份证等不会被 Excel 自动格式化。
 * 3) 数值精度保护：超 15 位有效数字转文本，避免双精度丢失。
 * 4) 日期/时间广泛支持：支持多种类型与字符串常见格式。
 * 5) HSSF/XSSF 颜色处理兼容。
 */
public class Cells {

    /* ==============================
     *   读写 API
     * ============================== */

    /**
     * 根据目标单元格类型读取值
     * @param excelCell 目标单元格
     * @return 单元格的值
     */
    public static Object readFromCell(ExcelCell excelCell) {
        Cell cell = excelCell.getCell();
        if (cell == null) return null;

        Object value = null;
        switch (cell.getCellType()) {
            case STRING -> value = cell.getStringCellValue();

            case NUMERIC -> {
                // 日期与数值都属于 NUMERIC，需要先判断是否为日期
                if (DateUtil.isCellDateFormatted(cell)) {
                    value = cell.getDateCellValue();
                } else {
                    // 这里返回 double。若是超长数字原本就以文本写入，则读到的会是 STRING，不会走到这里。
                    value = cell.getNumericCellValue();
                }
            }

            case BOOLEAN -> value = cell.getBooleanCellValue();

            case FORMULA -> {
                // 默认返回公式字符串（安全，且性能更好）
                value = cell.getCellFormula();

                // 如果需要“读计算结果”，可以启用下面三行（注意：Evaluator 有性能开销）
                // FormulaEvaluator evaluator = excelCell.getWorkbook().getCreationHelper().createFormulaEvaluator();
                // CellValue cv = evaluator.evaluate(cell);
                // value = cv == null ? null : switch (cv.getCellType()) {
                //     case STRING -> cv.getStringValue();
                //     case NUMERIC -> cv.getNumberValue();
                //     case BOOLEAN -> cv.getBooleanValue();
                //     case BLANK -> "";
                //     case ERROR -> FormulaError.forInt(cv.getErrorValue()).getString();
                //     default -> null;
                // };
            }

            case BLANK -> value = "";
            case _NONE -> value = null;
            case ERROR -> value = FormulaError.forInt(cell.getErrorCellValue()).getString();
        }
        return value;
    }

    /**
     * 向单元格写入内容
     * @param excelCell 写入的内容
     */
    public static void writeToCell(ExcelCell excelCell) {
        if (excelCell.getCell() == null) {
            throw new DocException("必须给ExcelCell设置单元格cell属性");
        }
        writeToCell(excelCell.getCell(), excelCell);
    }

    /**
     * 向单元格写入内容（核心方法）
     * - 字符串一律按文本写入（保护手机号等）
     * - 各类强类型优先处理
     * - 其余对象 fallback 为文本
     */
    public static void writeToCell(Cell cell, ExcelCell excelCell) {
        Object content = excelCell.getContent();
        Workbook wb = excelCell.getWorkbook();

        // 基础样式（可作为“模板”）；为避免修改共享样式，这里只做 clone + 变更并通过缓存复用
        CellStyle base = excelCell.getCellStyle();

        // 1) 原始就是字符串：无条件按文本写（手机号、身份证、编号、邮编等）
        if (content instanceof String str) {
            writeAsText(cell, str, base, wb);
            return;
        }

        // 2) null -> 空
        if (content == null) {
            cell.setBlank();
            cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).text, true));
            return;
        }

        // 3) 强类型优先：Boolean
        if (content instanceof Boolean b) {
            cell.setCellValue(b);
            cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, null, true)); // 布尔使用“常规”格式
            return;
        }

        // 4) 强类型优先：BigInteger / BigDecimal（带精度保护）
        if (content instanceof BigInteger bi) {
            writeNumberPreservingPrecision(cell, bi, base, wb);
            return;
        }
        if (content instanceof BigDecimal bd) {
            writeNumberPreservingPrecision(cell, bd, base, wb);
            return;
        }

        // 5) 强类型优先：整数
        if (content instanceof Byte || content instanceof Short || content instanceof Integer || content instanceof Long) {
            long v = ((Number) content).longValue();
            cell.setCellValue(v);
            cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).intFmt, true));
            return;
        }

        // 6) 强类型优先：浮点
        if (content instanceof Float || content instanceof Double) {
            double v = ((Number) content).doubleValue();
            if (Double.isFinite(v)) {
                cell.setCellValue(v);
                cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).decimal2, true));
            } else {
                // NaN/Infinity 无法作为数值：以文本写出
                writeAsText(cell, String.valueOf(content), base, wb);
            }
            return;
        }

        // 7) 强类型优先：日期/时间族
        if (content instanceof Date d) {
            cell.setCellValue(d);
            cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).dateTime, true));
            return;
        }
        if (content instanceof LocalDate ld) {
            Date d = Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
            cell.setCellValue(d);
            cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).date, true));
            return;
        }
        if (content instanceof LocalDateTime ldt) {
            Date d = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
            cell.setCellValue(d);
            cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).dateTime, true));
            return;
        }
        if (content instanceof Instant ins) {
            Date d = Date.from(ins);
            cell.setCellValue(d);
            cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).dateTime, true));
            return;
        }
        if (content instanceof OffsetDateTime odt) {
            Date d = Date.from(odt.toInstant());
            cell.setCellValue(d);
            cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).dateTime, true));
            return;
        }
        if (content instanceof ZonedDateTime zdt) {
            Date d = Date.from(zdt.toInstant());
            cell.setCellValue(d);
            cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).dateTime, true));
            return;
        }

        // 8) 其它对象：toString 后仍按文本写
        String str = String.valueOf(content).trim();
        if (str.isEmpty()) {
            cell.setBlank();
            cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, null, true));
            return;
        }

        // 安全保护：以 "=" 开头的字符串（潜在公式注入）-> 文本
        if (str.startsWith("=")) {
            writeAsText(cell, str, base, wb);
            return;
        }

        // 尝试解析为布尔字符串（中英文/缩写）
        Boolean parsedBool = tryParseBoolean(str);
        if (parsedBool != null) {
            cell.setCellValue(parsedBool);
            cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, null, true));
            return;
        }

        // 尝试解析为日期字符串（常见格式 & 时间戳），并区分“仅日期/日期时间”
        Date parsedDate = tryParseDateTime(str);
        if (parsedDate != null) {
            cell.setCellValue(parsedDate);
            short fmt = isDateOnly(str) ? Formats.of(wb).date : Formats.of(wb).dateTime;
            cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, fmt, true));
            return;
        }

        // 尝试解析为数值字符串：带分组/科学计数法也可；超 15 位有效数字 -> 文本保护
        NumericParseResult npr = tryParseNumeric(str);
        if (npr != null) {
            if (npr.writeAsText) {
                writeAsText(cell, npr.originalNormalizedText, base, wb);
            } else if (npr.isInteger) {
                cell.setCellValue(npr.longValue);
                cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).intFmt, true));
            } else {
                cell.setCellValue(npr.doubleValue);
                cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).decimal2, true));
            }
            return;
        }

        // fallback：文本
        writeAsText(cell, str, base, wb);
    }


    /* ==============================
     *   样式&格式 API（对外保持不变）
     * ============================== */

    /**
     * 向单元格设置边框样式
     * @param cell 样式对象
     * @param borderStyles 边框样式（0~4个传参分别代表：上/右/下/左；不足时按规则扩展）
     */
    public static CellStyle setBorderStyle(ExcelCell cell, BorderStyle... borderStyles) {
        CellStyle style = cell.getCellStyle();
        if (borderStyles.length == 0) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        } else if (borderStyles.length == 1) {
            style.setBorderTop(borderStyles[0]);
            style.setBorderBottom(borderStyles[0]);
            style.setBorderLeft(borderStyles[0]);
            style.setBorderRight(borderStyles[0]);
        } else if (borderStyles.length == 2) {
            style.setBorderTop(borderStyles[0]);
            style.setBorderBottom(borderStyles[0]);
            style.setBorderLeft(borderStyles[1]);
            style.setBorderRight(borderStyles[1]);
        } else if (borderStyles.length == 3) {
            style.setBorderTop(borderStyles[0]);
            style.setBorderBottom(borderStyles[0]);
            style.setBorderLeft(borderStyles[1]);
            style.setBorderRight(borderStyles[2]);
        } else {
            style.setBorderTop(borderStyles[0]);
            style.setBorderRight(borderStyles[1]);
            style.setBorderBottom(borderStyles[2]);
            style.setBorderLeft(borderStyles[3]);
        }
        return style;
    }

    /** 设置居中对齐 */
    public static CellStyle setCenterAlignment(ExcelCell cell) {
        return setAlignment(cell, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
    }

    /** 设置对齐样式 */
    public static CellStyle setAlignment(ExcelCell cell, HorizontalAlignment horizontal, VerticalAlignment vertical) {
        CellStyle style = cell.getCellStyle();
        style.setAlignment(horizontal);
        style.setVerticalAlignment(vertical);
        return style;
    }

    /** 设置字体（简单创建；如需复用可自行做字体缓存） */
    public static CellStyle setFont(ExcelCell cell, String fontName, short fontSize, boolean isBold, boolean isItalic) {
        CellStyle style = cell.getCellStyle();
        Font font = cell.getWorkbook().createFont();
        font.setFontName(fontName);
        font.setFontHeightInPoints(fontSize);
        font.setBold(isBold);
        font.setItalic(isItalic);
        style.setFont(font);
        return style;
    }

    /**
     * 设置背景色（HSSF/XSSF 兼容）
     * - HSSF：通过调色板找到近似 RGB 并设置 short 索引
     * - XSSF：直接使用 XSSFColor
     */
    public static CellStyle setBackgroundColor(ExcelCell cell, int r, int g, int b) {
        Workbook wb = cell.getWorkbook();
        CellStyle style = cell.getCellStyle();

        if (wb instanceof HSSFWorkbook hssfWb) {
            HSSFPalette palette = hssfWb.getCustomPalette();
            // findSimilarColor 会挑选最接近的调色板颜色
            short idx = palette.findSimilarColor((byte) r, (byte) g, (byte) b).getIndex();
            style.setFillForegroundColor(idx);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        } else {
            // XSSF：可以精确设置 RGB
            if (style instanceof XSSFCellStyle xssfStyle) {
                xssfStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(r, g, b), null));
                xssfStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            } else {
                // 兜底：某些实现上行不通时尝试 setFillForegroundColor(short)，但可能不生效
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
        }
        return style;
    }


    /* ==============================
     *   辅助：样式与格式缓存
     * ============================== */

    /**
     * 每个 Workbook 一份格式缓存，短格式索引（text/date/datetime/int/decimal2）
     */
    private static final Map<Workbook, Formats> FORMAT_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** 每个 Workbook 一份样式缓存（key = baseStyleId|dataFmt|wrap） */
    private static final Map<Workbook, Map<String, CellStyle>> STYLE_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** 封装常用格式索引 */
    private static final class Formats {
        final short text;      // "@"
        final short date;      // "yyyy-mm-dd"
        final short dateTime;  // "yyyy-mm-dd hh:mm:ss"
        final short intFmt;    // "#,##0"
        final short decimal2;  // "#,##0.00"

        private Formats(Workbook wb) {
            DataFormat df = wb.createDataFormat();
            this.text = df.getFormat("@");
            this.date = df.getFormat("yyyy-mm-dd");
            this.dateTime = df.getFormat("yyyy-mm-dd hh:mm:ss");
            this.intFmt = df.getFormat("#,##0");
            this.decimal2 = df.getFormat("#,##0.00");
        }

        static Formats of(Workbook wb) {
            return FORMAT_CACHE.computeIfAbsent(wb, Formats::new);
        }
    }

    /** 样式缓存工具 */
    private static final class StyleCache {
        static CellStyle getOrCreateStyled(Workbook wb, CellStyle base, Short dataFormat, boolean wrap) {
            Map<String, CellStyle> map = STYLE_CACHE.computeIfAbsent(wb, k -> new ConcurrentHashMap<>());
            String baseId = base == null ? "null" : Integer.toHexString(System.identityHashCode(base));
            String key = baseId + "|" + (dataFormat == null ? "null" : dataFormat) + "|" + wrap;

            // 命中则复用
            CellStyle cached = map.get(key);
            if (cached != null) return cached;

            // 未命中：创建并缓存（cloneFrom + 修改）
            CellStyle cs = wb.createCellStyle();
            if (base != null) cs.cloneStyleFrom(base);
            if (wrap) cs.setWrapText(true);
            if (dataFormat != null) cs.setDataFormat(dataFormat);
            map.put(key, cs);
            return cs;
        }
    }

    /* ==============================
     *   辅助：具体写入实现
     * ============================== */

    /** 文本写入（强制文本格式） */
    private static void writeAsText(Cell cell, String text, CellStyle base, Workbook wb) {
        cell.setCellValue(text);
        cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).text, true));
    }

    /** BigInteger 写入，保护超长精度 */
    private static void writeNumberPreservingPrecision(Cell cell, BigInteger bi, CellStyle base, Workbook wb) {
        String s = bi.toString();
        if (s.length() > 15) {
            // 超 15 位有效数字 -> 文本保护
            writeAsText(cell, s, base, wb);
        } else {
            cell.setCellValue(bi.longValue());
            cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).intFmt, true));
        }
    }

    /** BigDecimal 写入，保护超长精度 */
    private static void writeNumberPreservingPrecision(Cell cell, BigDecimal bd, CellStyle base, Workbook wb) {
        BigDecimal norm = bd.stripTrailingZeros();
        int precision = norm.precision();
        if (precision <= 15 && withinDoubleRange(norm)) {
            if (norm.scale() <= 0) {
                cell.setCellValue(norm.longValue());
                cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).intFmt, true));
            } else {
                cell.setCellValue(norm.doubleValue());
                cell.setCellStyle(StyleCache.getOrCreateStyled(wb, base, Formats.of(wb).decimal2, true));
            }
        } else {
            // 高精/超长 -> 文本保护
            writeAsText(cell, bd.toPlainString(), base, wb);
        }
    }

    private static boolean withinDoubleRange(BigDecimal bd) {
        try {
            double d = bd.doubleValue();
            return Double.isFinite(d);
        } catch (ArithmeticException ex) {
            return false;
        }
    }


    /* ==============================
     *   辅助：字符串判定/解析
     * ============================== */

    /** 常见布尔词：中英文/缩写 */
    private static Boolean tryParseBoolean(String s) {
        String v = s.trim().toLowerCase(Locale.ROOT);
        if (v.equals("true") || v.equals("t") || v.equals("yes") || v.equals("y") || v.equals("1")
                || v.equals("是") || v.equals("对") || v.equals("真")) return true;
        if (v.equals("false") || v.equals("f") || v.equals("no") || v.equals("n") || v.equals("0")
                || v.equals("否") || v.equals("错") || v.equals("假")) return false;
        return null;
    }

    // 秒/毫秒时间戳：10位（可带小数）或13位
    private static final Pattern EXCEL_TS = Pattern.compile("^\\d{10}(?:\\.\\d+)?$|^\\d{13}$");

    /** 尝试将字符串解析为日期/时间（支持多格式 & ISO & 时间戳） */
    private static Date tryParseDateTime(String s) {
        String num = s.trim();

        // 1) 时间戳（秒/毫秒）
        if (EXCEL_TS.matcher(num).matches()) {
            try {
                long epoch;
                if (num.length() == 13) {
                    epoch = Long.parseLong(num);
                } else {
                    double sec = Double.parseDouble(num);
                    epoch = (long) (sec * 1000);
                }
                return new Date(epoch);
            } catch (Exception ignore) {
            }
        }

        // 2) ISO 标准与常见格式
        List<DateTimeFormatter> fmts = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                DateTimeFormatter.ISO_ZONED_DATE_TIME,
                DateTimeFormatter.ISO_LOCAL_DATE,
                // 常见变体：yyyy-MM-dd HH:mm[:ss][.SSS]
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"),
                // 仅日期
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("yyyy.MM.dd"),
                // 中文常见
                DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy年MM月dd日")
        );

        for (DateTimeFormatter f : fmts) {
            try {
                if (f == DateTimeFormatter.ISO_LOCAL_DATE) {
                    LocalDate d = LocalDate.parse(s, f);
                    return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
                } else if (f == DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        || "yyyy-MM-dd HH:mm:ss.SSS".equals(f.toString())
                        || "yyyy-MM-dd HH:mm:ss".equals(f.toString())
                        || "yyyy-MM-dd HH:mm".equals(f.toString())
                        || "yyyy/MM/dd HH:mm:ss".equals(f.toString())
                        || "yyyy/MM/dd HH:mm".equals(f.toString())
                        || "yyyy.MM.dd HH:mm:ss".equals(f.toString())
                        || "yyyy.MM.dd HH:mm".equals(f.toString())
                        || "yyyy年MM月dd日 HH:mm:ss".equals(f.toString())) {
                    LocalDateTime dt = LocalDateTime.parse(s, f);
                    return Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
                } else if (f == DateTimeFormatter.ISO_OFFSET_DATE_TIME) {
                    OffsetDateTime odt = OffsetDateTime.parse(s, f);
                    return Date.from(odt.toInstant());
                } else if (f == DateTimeFormatter.ISO_ZONED_DATE_TIME) {
                    ZonedDateTime zdt = ZonedDateTime.parse(s, f);
                    return Date.from(zdt.toInstant());
                } else {
                    // 仅日期
                    LocalDate d = LocalDate.parse(s, f);
                    return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
                }
            } catch (DateTimeParseException ignore) {
            }
        }
        return null;
    }

    /** 简单判断字符串是否只包含“日期”而无“时间” */
    private static boolean isDateOnly(String s) {
        String t = s.trim();
        // 不包含 ":" 且不是 ISO 带 T 的时间形式
        return !t.contains(":") && !t.matches(".*[Tt].*\\d{1,2}:\\d{2}.*");
    }

    /* ===== 数字字符串解析（带精度保护） ===== */

    private static class NumericParseResult {
        boolean isInteger;
        boolean writeAsText;           // 超精度/范围 -> 文本写出
        double doubleValue;
        long longValue;
        String originalNormalizedText; // 清洗后的原始文本（用于文本写出）
    }

    private static final Pattern NUM_WITH_GROUPING = Pattern.compile("^[-+]?\\d{1,3}(?:[,_\\s]\\d{3})*(?:\\.\\d+)?$");
    private static final Pattern PLAIN_DECIMAL = Pattern.compile("^[-+]?\\d+(?:\\.\\d+)?$");
    private static final Pattern SCI_NOTATION = Pattern.compile("^[-+]?\\d+(?:\\.\\d+)?[eE][-+]?\\d+$");

    /**
     * 尝试把字符串解析为数字：
     * - 支持分组符（, _ 空格）与科学计数法
     * - 对前导零（如“00123”且非小数）保持文本语义
     * - 超 15 位有效数字 / 超 long 范围 -> 文本写出
     */
    private static NumericParseResult tryParseNumeric(String s) {
        // 前导零保护（如“00123”）：视为文本（除非是 "0" 或 "0.xxx"）
        if (s.startsWith("0") && !s.equals("0") && !s.startsWith("0.")) return null;

        String normalized = s.replace('，', ',').trim();
        boolean looksNumeric = PLAIN_DECIMAL.matcher(normalized).matches()
                || NUM_WITH_GROUPING.matcher(normalized).matches()
                || SCI_NOTATION.matcher(normalized).matches();
        if (!looksNumeric) return null;

        // 去掉分组符
        String cleaned = normalized.replace(",", "").replace("_", "").replace(" ", "");
        NumericParseResult r = new NumericParseResult();
        r.originalNormalizedText = cleaned;

        try {
            BigDecimal bd = new BigDecimal(cleaned);
            BigDecimal norm = bd.stripTrailingZeros();

            // >15 位有效数字 或 非有限 double -> 文本写出
            if (norm.precision() > 15 || !withinDoubleRange(norm)) {
                r.writeAsText = true;
                return r;
            }

            // 整数/小数判定
            if (norm.scale() <= 0) {
                try {
                    r.longValue = norm.longValueExact(); // 检查是否溢出 long
                    r.isInteger = true;
                    r.writeAsText = false;
                    return r;
                } catch (ArithmeticException e) {
                    // 超 long 范围 -> 文本写出（即便 double 能表示，也会丢精度）
                    r.writeAsText = true;
                    return r;
                }
            } else {
                r.doubleValue = norm.doubleValue();
                r.isInteger = false;
                r.writeAsText = false;
                return r;
            }
        } catch (NumberFormatException ignore) {
            return null;
        }
    }
}
