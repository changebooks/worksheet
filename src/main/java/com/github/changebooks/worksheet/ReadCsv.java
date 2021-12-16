package com.github.changebooks.worksheet;

import com.google.common.base.Preconditions;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 读csv
 *
 * <pre>
 * <dependency>
 *     <groupId>com.opencsv</groupId>
 *     <artifactId>opencsv</artifactId>
 * </dependency>
 * </pre>
 *
 * @author changebooks
 */
public final class ReadCsv {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadCsv.class);

    /**
     * RFC4180解析器
     */
    private static final RFC4180Parser RFC4180_PARSER = new RFC4180ParserBuilder().build();

    /**
     * 监听行
     */
    public interface Listener {
        /**
         * 标题行
         *
         * @param keyMap 行数据，columnIndex => value
         */
        void invokeKey(Map<Integer, String> keyMap);

        /**
         * 数据行
         *
         * @param rowIndex 行索引
         * @param valueMap 行数据，columnIndex => value
         */
        void invokeValue(int rowIndex, Map<Integer, String> valueMap);

        /**
         * 完成
         *
         * @param rowIndex 最后一行，行索引
         */
        void onComplete(int rowIndex);

    }

    /**
     * 每页行数
     */
    private final int pageSize;

    public static ReadCsv create(int pageSize) {
        return new ReadCsv(pageSize);
    }

    private ReadCsv(int pageSize) {
        Preconditions.checkArgument(pageSize > 0, "pageSize must be positive");

        this.pageSize = pageSize;
    }

    /**
     * 分页读
     */
    public void readPage(File file, Charset charset, ReadPage.Listener listener) throws IOException {
        Preconditions.checkNotNull(file, "file can't be null");

        try (InputStream stream = new FileInputStream(file)) {
            readPage(stream, charset, listener);
        }
    }

    /**
     * 分页读
     */
    public void readPage(InputStream stream, Charset charset, ReadPage.Listener listener) throws IOException {
        Preconditions.checkNotNull(listener, "listener can't be null");

        // 当前页的首行索引
        final Integer[] startRow = {null};

        // 当前页的数据列表
        List<Map<String, String>> data = new ArrayList<>();

        readLine(stream, charset, new ReadLine.Listener() {
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

    /**
     * 同步读
     */
    public static List<Map<String, String>> readSync(File file, Charset charset) throws IOException {
        Preconditions.checkNotNull(file, "file can't be null");

        try (InputStream stream = new FileInputStream(file)) {
            return readSync(stream, charset);
        }
    }

    /**
     * 同步读
     */
    public static List<Map<String, String>> readSync(InputStream stream, Charset charset) throws IOException {
        List<Map<String, String>> result = new ArrayList<>();

        readLine(stream, charset, new ReadLine.Listener() {
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

    /**
     * 逐行读
     */
    public static void readLine(File file, Charset charset, ReadLine.Listener listener) throws IOException {
        Preconditions.checkNotNull(file, "file can't be null");

        try (InputStream stream = new FileInputStream(file)) {
            readLine(stream, charset, listener);
        }
    }

    /**
     * 逐行读
     */
    public static void readLine(InputStream stream, Charset charset, ReadLine.Listener listener) throws IOException {
        Preconditions.checkNotNull(stream, "stream can't be null");
        Preconditions.checkNotNull(listener, "listener can't be null");

        try (InputStreamReader reader = Objects.isNull(charset) ?
                new InputStreamReader(stream) : new InputStreamReader(stream, charset)) {
            readLine(reader, listener);
        }
    }

    /**
     * 逐行读
     */
    public static void readLine(InputStreamReader stream, ReadLine.Listener listener) throws IOException {
        Preconditions.checkNotNull(stream, "stream can't be null");
        Preconditions.checkNotNull(listener, "listener can't be null");

        CSVReaderBuilder builder = new CSVReaderBuilder(stream).withCSVParser(RFC4180_PARSER);
        try (CSVReader reader = builder.build()) {
            readLine(reader, new Listener() {
                /**
                 * columnIndex => keyName
                 */
                private Map<Integer, String> keys;

                @Override
                public void invokeKey(Map<Integer, String> keyMap) {
                    keys = ReadUtils.asKey(keyMap);
                }

                @Override
                public void invokeValue(int rowIndex, Map<Integer, String> valueMap) {
                    Map<String, String> data = ReadUtils.combine(keys, valueMap);
                    listener.invoke(rowIndex, data);
                }

                @Override
                public void onComplete(int rowIndex) {
                    listener.onComplete(rowIndex);
                }
            });
        }
    }

    /**
     * 逐行读
     */
    public static void readLine(CSVReader reader, Listener listener) throws IOException {
        Preconditions.checkNotNull(reader, "reader can't be null");
        Preconditions.checkNotNull(listener, "listener can't be null");

        int rowIndex = 0;
        while (true) {
            String[] bucket;
            try {
                if ((bucket = reader.readNext()) == null) {
                    listener.onComplete(rowIndex - 1);
                    break;
                }
            } catch (CsvValidationException e) {
                LOGGER.error("readLine failed, rowIndex: {}, throwable: ", rowIndex, e);
                bucket = null;
            }

            Map<Integer, String> valueMap = ReadUtils.asMap(bucket);

            if (rowIndex++ == 0) {
                listener.invokeKey(valueMap);
                continue;
            }

            try {
                listener.invokeValue(rowIndex - 1, valueMap);
            } catch (StopException e) {
                break;
            }
        }
    }

}
