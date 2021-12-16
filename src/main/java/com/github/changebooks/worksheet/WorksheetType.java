package com.github.changebooks.worksheet;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Objects;

/**
 * 表格类型
 *
 * @author changebooks
 */
public enum WorksheetType {
    // .csv
    CSV,

    // .xls
    // .xlsx
    XLS,

    ;

    /**
     * 支持的类型？
     */
    public static void checkSupport(WorksheetType type) {
        Preconditions.checkNotNull(type, "type can't be null");

        boolean supported = WorksheetType.CSV == type ||
                WorksheetType.XLS == type;

        Preconditions.checkArgument(supported, "unsupported's type: " + type);
    }

    /**
     * csv ?
     */
    public static boolean isCsv(WorksheetType type) {
        if (Objects.isNull(type)) {
            return false;
        } else {
            return CSV == type;
        }
    }

    /**
     * xls or xlsx ?
     */
    public static boolean isXls(WorksheetType type) {
        if (Objects.isNull(type)) {
            return false;
        } else {
            return XLS == type;
        }
    }

    /**
     * 扩展名：csv
     */
    public static final String EXTENSION_CSV = "csv";

    /**
     * 扩展名：03 Excel
     */
    public static final String EXTENSION_XLS = "xls";

    /**
     * 扩展名：07 Excel
     */
    public static final String EXTENSION_XLSX = "xlsx";

    /**
     * 扩展名分隔符
     */
    public static final String EXTENSION_SEPARATOR = ".";

    /**
     * 文件对象 -> 类型
     */
    public static WorksheetType fromFile(File file) {
        if (Objects.isNull(file)) {
            return null;
        } else {
            return fromName(file.getName());
        }
    }

    /**
     * 文件名 -> 类型
     */
    public static WorksheetType fromName(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return null;
        }

        String fileExtension = getExtension(fileName);
        if (StringUtils.isEmpty(fileExtension)) {
            return null;
        } else {
            return fromExtension(fileExtension);
        }
    }

    /**
     * 扩展名 -> 类型
     */
    public static WorksheetType fromExtension(String fileExtension) {
        if (StringUtils.isEmpty(fileExtension)) {
            return null;
        }

        fileExtension = fileExtension.toLowerCase();

        if (EXTENSION_CSV.equals(fileExtension)) {
            return CSV;
        }

        if (EXTENSION_XLS.equals(fileExtension) ||
                EXTENSION_XLSX.equals(fileExtension)) {
            return XLS;
        }

        return null;
    }

    /**
     * 文件名 -> 扩展名
     */
    public static String getExtension(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return "";
        }

        int index = fileName.lastIndexOf(EXTENSION_SEPARATOR);
        if (index < 0) {
            return "";
        } else {
            return fileName.substring(index + 1);
        }
    }

}
