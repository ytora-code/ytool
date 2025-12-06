package xyz.ytora.ytool.document.excel;

import xyz.ytora.ytool.classcache.classmeta.FieldMetadata;
import xyz.ytora.ytool.convert.Converts;
import xyz.ytora.ytool.date.Dates;
import xyz.ytora.ytool.document.excel.factory.ExcelFieldHandlerFactory;
import xyz.ytora.ytool.invoke.Reflects;
import xyz.ytora.ytool.str.Strs;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

/**
 * created by yangtong on 2025/5/29 21:54:49
 * <br/>
 * Excel注解解析器
 */
public class ExcelAnnoHandler {

    /**
     * 要处理的字段
     */
    private final Field field;

    /**
     * 该字段对应的Excel注解
     */
    private final Excel excel;

    /**
     * Excel注解的handler对象创建工厂
     */
    private ExcelFieldHandlerFactory handlerFactory;

    public ExcelAnnoHandler(FieldMetadata field, ExcelFieldHandlerFactory handlerFactory) {
        this.field = field.getSourceField();
        this.excel = field.getAnnotation(Excel.class);
        this.handlerFactory = handlerFactory;
    }

    public Boolean hasExcel() {
        return excel != null;
    }

    /**
     * 将EXCEL的值写入POJO对应的字段
     * @param targetObj 目标POJO对象
     * @param value 从EXCEL中读取的值
     */
    @SuppressWarnings("unchecked")
    public void parse(Object targetObj, Object value) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        if (value == null) {
            return;
        }
        //通过setter方法给targetObj对象的字段赋值
        String setter = "set" + Strs.firstCapitalize(field.getName());

        Object finalValue;
        //如果是日期Date类型
        if (value instanceof Date dateValue && Strs.isNotEmpty(excel.format())) {
            finalValue = Dates.format(dateValue, excel.format());

        } else {
            //依次调用处理器
            for (Class<? extends IExcelFieldHandler> handlerClass : excel.handlers()) {
                //从工厂中获取ExcelFieldHandler
                IExcelFieldHandler<Object, Object, Object> handler =
                        handlerFactory.getHandler(handlerClass);
                //执行处理器，并返回处理后的结果
                value = handler.handler(targetObj, field, value);
            }
            finalValue = value;
        }
        //判断finalValue能否赋值给字段，如果不一致则调用类型转换器尝试转换
        if (field.getType().isAssignableFrom(finalValue.getClass())) {
            Reflects.invokeMethod(targetObj, setter, finalValue);
        } else {
            Reflects.invokeMethod(targetObj, setter, Converts.convert(finalValue, field.getType()));
        }
    }

    public Field getField() {
        return field;
    }

    public Excel getExcel() {
        return excel;
    }

    public ExcelFieldHandlerFactory getHandlerFactory() {
        return handlerFactory;
    }

    public void setHandlerFactory(ExcelFieldHandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }
}
