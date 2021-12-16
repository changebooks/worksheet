package com.github.changebooks.worksheet;

import com.alibaba.excel.read.metadata.ReadSheet;
import com.google.common.base.Preconditions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 分页读
 * 读csv、xls和xlsx
 *
 * <pre>
 * <dependency>
 *     <groupId>com.alibaba</groupId>
 *     <artifactId>easyexcel</artifactId>
 * </dependency>
 * </pre>
 *
 * @author changebooks
 */
public final class ReadPage {
    /**
     * 监听页
     */
    public interface Listener {
        /**
         * 回调监听
         *
         * @param rowIndex 当前页的首行索引
         * @param data     当前页的数据列表，key => value
         */
        void invoke(Integer rowIndex, List<Map<String, String>> data);

    }

    /**
     * 每页行数
     */
    private final int pageSize;

    /**
     * 工作表
     */
    private final ReadSheet sheet;

    public static ReadPage create(int pageSize) {
        return create(pageSize, null);
    }

    public static ReadPage create(int pageSize, ReadSheet sheet) {
        return new ReadPage(pageSize, sheet);
    }

    private ReadPage(int pageSize, ReadSheet sheet) {
        Preconditions.checkArgument(pageSize > 0, "pageSize must be positive");

        this.pageSize = pageSize;
        this.sheet = sheet;
    }

    /**
     * 读文件
     */
    public void read(File file, Listener listener) throws IOException {
        read(file, sheet, listener);
    }

    /**
     * 读文件流
     */
    public void read(WorksheetType type, InputStream stream, Listener listener) {
        read(type, stream, sheet, listener);
    }

    /**
     * 读文件
     */
    public void read(File file, ReadSheet sheet, Listener listener) throws IOException {
        Preconditions.checkNotNull(file, "file can't be null");

        WorksheetType type = WorksheetType.fromFile(file);
        try (InputStream stream = new FileInputStream(file)) {
            read(type, stream, sheet, listener);
        }
    }

    /**
     * 读文件流
     */
    public void read(WorksheetType type, InputStream stream, ReadSheet sheet, Listener listener) {
        Preconditions.checkNotNull(listener, "listener can't be null");

        // 当前页的首行索引
        final Integer[] startRow = {null};

        // 当前页的数据列表
        List<Map<String, String>> data = new ArrayList<>();

        ReadLine.read(type, stream, sheet, new ReadLine.Listener() {
            @Override
            public void invoke(Integer rowIndex, Map<String, String> valueMap) {
                if (Objects.isNull(startRow[0])) {
                    startRow[0] = rowIndex;
                }

                if (data.size() < pageSize) {
                    data.add(valueMap);
                }

                if (data.size() >= pageSize) {
                    listener.invoke(startRow[0], data);
                    startRow[0] = null;
                    data.clear();
                }
            }

            @Override
            public void onComplete(Integer rowIndex) {
            }
        });

        if (data.size() > 0) {
            listener.invoke(startRow[0], data);
            data.clear();
        }
    }

    public int getPageSize() {
        return pageSize;
    }

    public ReadSheet getSheet() {
        return sheet;
    }

}
