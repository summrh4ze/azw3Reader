package org.example.miraireader.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Book {
    private static final Logger log = LoggerFactory.getLogger(Book.class);
    private final File file;
    private final Azw3Metadata metadata;

    public Book(File file, Azw3Metadata metadata) {
        this.file = file;
        this.metadata = metadata;
    }

    private boolean invalidBookFormat() {
        if (metadata.getPalmDatabaseHeader() == null || metadata.getPalmDatabaseHeader().recordsNumber() < 1) {
            return true;
        }
        if (metadata.getMobiHeader() == null) {
            return true;
        }
        return metadata.getPalmDocHeader() == null;
    }

    public String getTitle() {
        if (invalidBookFormat()) {
            throw new RuntimeException("Invalid book format");
        }
        int zeroRecordOffset = metadata.getPalmDatabaseHeader().records().getFirst().offset();
        int fullNameOffset = metadata.getMobiHeader().fullNameOffset();
        int fullNameLen = metadata.getMobiHeader().fullNameLength();
        if (fullNameLen <= 0) {
            throw new RuntimeException("fullNameLen overflows integer");
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] buf = new byte[fullNameLen];
            int offset = zeroRecordOffset + fullNameOffset;
            raf.seek(offset);
            raf.readFully(buf);
            return new String(buf, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Could not read book file: ", ex);
            throw new RuntimeException(ex);
        }
    }

    public byte[] getPage(int pageIndex) {
        if (invalidBookFormat()) {
            throw new RuntimeException("Invalid book format");
        }
        if (pageIndex < 0) {
            throw new IllegalArgumentException("page index must be greater than 0");
        }
        int firstImageIndex = metadata.getMobiHeader().firstImageIndex();
        int imageIndex = firstImageIndex + pageIndex;
        int imageOffset = metadata
                .getPalmDatabaseHeader()
                .records()
                .get(imageIndex)
                .offset();
        int nextImageOffset = metadata
                .getPalmDatabaseHeader()
                .records()
                .get(imageIndex + 1)
                .offset();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            int len = nextImageOffset - imageOffset;
            byte[] buf = new byte[len];
            raf.seek(imageOffset);
            raf.readFully(buf);
            return buf;
        } catch (IOException ex) {
            log.error("Could not read book file: ", ex);
            throw new RuntimeException(ex);
        }
    }
}
