package com.github.changebooks.worksheet;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelAnalysisStopException;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.read.metadata.holder.ReadRowHolder;
import com.alibaba.excel.read.metadata.holder.ReadSheetHolder;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 读csv、xls和xlsx
 *
 * @author changebooks
 */
public final class ReadUtils {

    private ReadUtils() {
    }

    /**
     * 行数，包括标题
     * csv，精确值
     * xls、xlsx，近似值
     */
    public static Integer getLineNum(File file) throws IOException {
        return getLineNum(file, null);
    }

    /**
     * 行数，包括标题
     * csv，精确值
     * xls、xlsx，近似值
     */
    public static Integer getLineNum(File file, ReadSheet sheet) throws IOException {
        Preconditions.checkNotNull(file, "file can't be null");

        WorksheetType type = WorksheetType.fromFile(file);
        try (InputStream stream = new FileInputStream(file)) {
            return getLineNum(type, stream, sheet);
        }
    }

    /**
     * 行数，包括标题
     * csv，精确值
     * xls、xlsx，近似值
     */
    public static Integer getLineNum(WorksheetType type, InputStream stream, ReadSheet sheet) throws IOException {
        WorksheetType.checkSupport(type);
        Preconditions.checkNotNull(stream, "stream can't be null");

        switch (type) {
            case CSV:
                return getCsvRowSize(stream);
            case XLS:
                return getXlsApproximateRowSize(stream, sheet);
            default:
                throw new RuntimeException("unsupported's type: " + type);
        }
    }

    /**
     * csv，精确行数
     */
    public static Integer getCsvRowSize(InputStream stream) throws IOException {
        if (Objects.isNull(stream)) {
            return null;
        }

        try (Reader reader = new InputStreamReader(stream)) {
            try (LineNumberReader lineNumReader = new LineNumberReader(reader)) {
                long n = lineNumReader.skip(Long.MAX_VALUE);
                return lineNumReader.getLineNumber();
            }
        }
    }

    /**
     * xls、xlsx，近似行数
     */
    public static Integer getXlsApproximateRowSize(InputStream stream, ReadSheet sheet) {
        if (Objects.isNull(stream)) {
            return null;
        }

        final Integer[] result = new Integer[1];

        ExcelReader reader = EasyExcel.read(stream, new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                result[0] = getRowSize(context);
                throw new ExcelAnalysisStopException();
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).build();

        if (Objects.isNull(reader)) {
            return null;
        }

        if (Objects.isNull(sheet)) {
            sheet = EasyExcel.readSheet(0).build();
        }

        try {
            reader.read(sheet);
            return result[0];
        } finally {
            reader.finish();
        }
    }

    /**
     * read total row's number
     */
    public static Integer getRowSize(AnalysisContext c) {
        if (Objects.isNull(c)) {
            return null;
        }

        ReadSheetHolder sheetHolder = c.readSheetHolder();
        if (Objects.isNull(sheetHolder)) {
            return null;
        }

        return sheetHolder.getApproximateTotalRowNumber();
    }

    /**
     * read current row's index
     */
    public static Integer getRowIndex(AnalysisContext c) {
        if (Objects.isNull(c)) {
            return null;
        }

        ReadRowHolder rowHolder = c.readRowHolder();
        if (Objects.isNull(rowHolder)) {
            return null;
        }

        return rowHolder.getRowIndex();
    }

    /**
     * combine keys and values
     * {index => key} and {index => value} -> {key => value}
     */
    public static Map<String, String> combine(Map<Integer, String> keys, Map<Integer, String> values) {
        if (Objects.isNull(values)) {
            return null;
        }

        Preconditions.checkNotNull(keys, "keys can't be null");

        int keySize = keys.size();
        Preconditions.checkArgument(keySize > 0, "keys can't be empty");

        Map<String, String> result = new HashMap<>(keySize);

        for (Integer columnIndex : keys.keySet()) {
            String key = keys.get(columnIndex);
            String value = values.get(columnIndex);

            result.put(key, value);
        }

        return result;
    }

    /**
     * values -> keys
     * if empty, ignore column
     * if duplicate, throw exception
     */
    public static Map<Integer, String> asKey(Map<Integer, String> values) {
        Preconditions.checkNotNull(values, "values can't be null");

        int size = values.size();
        Preconditions.checkArgument(size > 0, "values can't be empty");

        Map<Integer, String> result = new HashMap<>(size);

        for (Map.Entry<Integer, String> entry : values.entrySet()) {
            Integer columnIndex = entry.getKey();
            if (Objects.isNull(columnIndex) || columnIndex < 0) {
                continue;
            }

            String value = entry.getValue();
            if (Objects.isNull(value)) {
                continue;
            }

            String key = value.trim();
            if (key.length() == 0) {
                continue;
            }

            Preconditions.checkArgument(!result.containsValue(key),
                    String.format("duplicated's key: %s, columnIndex: %d", key, columnIndex));
            result.put(columnIndex, key);
        }

        return result;
    }

    /**
     * Array -> Map
     */
    public static Map<Integer, String> asMap(String[] values) {
        if (Objects.isNull(values)) {
            return null;
        }

        int len = values.length;
        Map<Integer, String> result = new HashMap<>(len);

        for (int i = 0; i < len; i++) {
            result.put(i, values[i]);
        }

        return result;
    }

}
