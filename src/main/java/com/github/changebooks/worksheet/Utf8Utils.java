package com.github.changebooks.worksheet;

import com.google.common.base.Preconditions;

import java.io.*;

/**
 * Utf-8 编码
 *
 * @author changebooks
 */
public final class Utf8Utils {
    /**
     * read stream bucket
     */
    public static final int BUCKET_SIZE = 1024;

    /**
     * 1 byte's bit num
     */
    public static final int BYTE_SIZE = Byte.SIZE;

    /**
     * 多字节
     */
    public static final class MultiByte {
        /**
         * byte[] is utf-8 ?
         * True : [ 10xxxxxx 10xxxxxx 10xxxxxx ]
         * False: [ 00xxxxxx 01xxxxxx 11xxxxxx ]
         */
        public static boolean checkSlice(byte[] data, int index, int size) {
            Preconditions.checkNotNull(data, "data can't be null");

            int len = data.length;
            Preconditions.checkArgument(index < len,
                    String.format("index out of data.len, index: %d, data.len: %d", index, len));

            for (int i = index, j = 0; i < len && j < size; i++, j++) {
                if (!checkByte(data[i])) {
                    return false;
                }
            }

            return true;
        }

        /**
         * byte is utf-8 ?
         * True : 10xxxxxx
         * False: 00xxxxxx、01xxxxxx、11xxxxxx
         */
        public static boolean checkByte(byte value) {
            return checkBit(value, 0) &&
                    !checkBit(value, 1);
        }

        /**
         * Multi byte's size
         * count first byte's 1
         * 110xxxxx - 2, e.g. 110xxxxx 10xxxxxx
         * 1110xxxx - 3, e.g. 110xxxxx 10xxxxxx 10xxxxxx
         * 11110xxx - 4, e.g. 110xxxxx 10xxxxxx 10xxxxxx 10xxxxxx
         * 111110xx - 5, e.g. 110xxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
         * 1111110x - 6, e.g. 110xxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
         */
        public static int size(byte first) {
            int result = 0;

            for (int i = 0; i < BYTE_SIZE; i++) {
                if (checkBit(first, i)) {
                    result++;
                } else {
                    break;
                }
            }

            return result;
        }

    }

    /**
     * 校验文件的前size字节
     * size = 0 ? 校验全部文件
     */
    private final int checkSize;

    public Utf8Utils() {
        this.checkSize = 0;
    }

    public Utf8Utils(int checkSize) {
        Preconditions.checkArgument(checkSize >= 0, "checkSize must be non-negative");
        this.checkSize = checkSize;
    }

    /**
     * file is utf-8 ?
     */
    public boolean check(File file) throws IOException {
        Preconditions.checkNotNull(file, "file can't be null");

        return checkHead(file) || checkBody(file, checkSize);
    }

    public int getCheckSize() {
        return checkSize;
    }

    /**
     * file's [0, 3]byte is utf-8 or utf-16 ?
     */
    public static boolean checkHead(File file) throws IOException {
        Preconditions.checkNotNull(file, "file can't be null");

        try (InputStream stream = new FileInputStream(file)) {
            return checkHead(stream);
        }
    }

    /**
     * file's [0, 3]byte is utf-8 or utf-16 ?
     */
    public static boolean checkHead(InputStream stream) throws IOException {
        Preconditions.checkNotNull(stream, "stream can't be null");

        byte[] head = new byte[3];
        int size = stream.read(head);
        if (size < 3) {
            return false;
        }

        if (head[0] == -1 && head[1] == -2) {
            // utf-16
            return true;
        }

        if (head[0] == -2 && head[1] == -1) {
            // utf-16
            return true;
        }

        // utf-8
        return head[0] == -17 &&
                head[1] == -69 &&
                head[2] == -65;
    }

    /**
     * file is utf-8 ?
     */
    public static boolean checkBody(File file, int checkSize) throws IOException {
        Preconditions.checkNotNull(file, "file can't be null");
        Preconditions.checkArgument(checkSize >= 0, "checkSize must be non-negative");

        try (InputStream stream = new FileInputStream(file)) {
            return checkBody(stream, checkSize);
        }
    }

    /**
     * stream is utf-8 ?
     */
    public static boolean checkBody(InputStream stream, int checkSize) throws IOException {
        Preconditions.checkNotNull(stream, "stream can't be null");
        Preconditions.checkArgument(checkSize >= 0, "checkSize must be non-negative");

        byte[] data = read(stream, checkSize);
        return check(data);
    }

    /**
     * byte[] is utf-8 ?
     */
    public static boolean check(byte[] data) {
        Preconditions.checkNotNull(data, "data can't be null");

        int len = data.length;
        Preconditions.checkArgument(len > 0, "data.len must be positive");

        for (int i = 0; i < len; i++) {
            byte value = data[i];
            if (isSingle(value)) {
                continue;
            }

            // Multi byte's size
            int size = MultiByte.size(value) - 1;

            // 110xxxxx - size = 1, e.g. 110xxxxx 10xxxxxx
            // 1110xxxx - size = 2, e.g. 110xxxxx 10xxxxxx 10xxxxxx
            if (size < 1) {
                return false;
            }

            // skip first
            int index = i + 1;
            if (!MultiByte.checkSlice(data, index, size)) {
                return false;
            }

            i += size;
        }

        return true;
    }

    /**
     * bit of byte is 1 ?
     * index >= 0 and index < 7
     */
    public static boolean checkBit(byte value, int index) {
        if (index < 0 || index >= BYTE_SIZE) {
            return false;
        }

        // index = 0, offset = 7
        // index = 1, offset = 6
        // index = 7, offset = 0
        int offset = BYTE_SIZE - index - 1;
        return (0x1 & value >> offset) == 1;
    }

    /**
     * Single byte ?
     * 0xxxxxxx
     */
    public static boolean isSingle(byte value) {
        return !checkBit(value, 0);
    }

    /**
     * Multi byte ?
     * 1xxxxxxx
     */
    public static boolean isMulti(byte value) {
        return checkBit(value, 0);
    }

    /**
     * 读stream的前size字节
     * size > 0 ? Result's size is [0, size + BUCKET_SIZE]
     * size = 0 ? Result's size is [0, stream.len]
     */
    public static byte[] read(InputStream stream, int size) throws IOException {
        Preconditions.checkNotNull(stream, "stream can't be null");
        Preconditions.checkArgument(size >= 0, "size must be non-negative");

        try (BufferedInputStream in = new BufferedInputStream(stream);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            int len;
            byte[] bucket = new byte[BUCKET_SIZE];
            while ((len = in.read(bucket)) != -1) {
                if (len > 0) {
                    out.write(bucket, 0, len);
                    out.flush();

                    if (size > 0 && out.size() >= size) {
                        break;
                    }
                }
            }

            return out.toByteArray();
        }
    }

}
