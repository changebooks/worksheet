package com.github.changebooks.worksheet;

import com.alibaba.excel.read.metadata.ReadSheet;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 同步读
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
public final class ReadSync {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadSync.class);

    /**
     * 工作表
     */
    private final ReadSheet sheet;

    public static ReadSync create() {
        return create(null);
    }

    public static ReadSync create(ReadSheet sheet) {
        return new ReadSync(sheet);
    }

    private ReadSync(ReadSheet sheet) {
        this.sheet = sheet;
    }

    /**
     * 读文件
     */
    public List<Map<String, String>> read(File file) throws IOException {
        return read(file, sheet);
    }

    /**
     * 读文件流
     */
    public List<Map<String, String>> read(WorksheetType type, InputStream stream) {
        return read(type, stream, sheet);
    }

    public ReadSheet getSheet() {
        return sheet;
    }

    /**
     * 读文件
     */
    public static List<Map<String, String>> read(File file, ReadSheet sheet) throws IOException {
        Preconditions.checkNotNull(file, "file can't be null");

        WorksheetType type = WorksheetType.fromFile(file);
        try (InputStream stream = new FileInputStream(file)) {
            return read(type, stream, sheet);
        }
    }

    /**
     * 读文件流
     */
    public static List<Map<String, String>> read(WorksheetType type, InputStream stream, ReadSheet sheet) {
        List<Map<String, String>> result = new ArrayList<>();

        ReadLine.read(type, stream, sheet, new ReadLine.Listener() {
            @Override
            public void invoke(Integer rowIndex, Map<String, String> valueMap) {
                if (Objects.isNull(valueMap)) {
                    LOGGER.error("read null, skip rowIndex: " + rowIndex);
                } else {
                    result.add(valueMap);
                }
            }

            @Override
            public void onComplete(Integer rowIndex) {
            }
        });

        return result;
    }

}
