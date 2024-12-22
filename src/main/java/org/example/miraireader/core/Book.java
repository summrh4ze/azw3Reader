package org.example.miraireader.core;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class Book {
    private static final Logger log = LoggerFactory.getLogger(Book.class);
    private final File file;
    private final Azw3Metadata metadata;
    private final int pageCount;

    public Book(File file, Azw3Metadata metadata) {
        this.file = file;
        this.metadata = metadata;

        int thumbIndex = getThumbnailIndex();
        int coverIndex = getCoverIndex();
        int count = getResourceCount();
        if (thumbIndex > count - 5) {
            if (coverIndex > count - 5) {
                count = Math.min(thumbIndex, coverIndex);
            } else {
                count -= 1;
            }
        }
        this.pageCount = count;
    }

    public String getTitle() {
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

    public Image getPage(int index) {
        if (index < 0 || index >= pageCount) {
            return null;
        }
        int firstImageIndex = metadata.getMobiHeader().firstImageIndex();
        int imageIndex = firstImageIndex + index;
        int imageOffset = metadata
                .getPalmDatabaseHeader()
                .records()
                .get(imageIndex)
                .offset();
        int nextRecordOffset = metadata
                .getPalmDatabaseHeader()
                .records()
                .get(imageIndex + 1)
                .offset();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            int len = nextRecordOffset - imageOffset;
            byte[] buf = new byte[len];
            raf.seek(imageOffset);
            raf.readFully(buf);
            return new Image(new ByteArrayInputStream(buf));
        } catch (IOException ex) {
            log.error("Could not read book file: ", ex);
            throw new RuntimeException(ex);
        }
    }

    public Image getCover() {
        int coverIndex = this.getCoverIndex();
        int firstImageIndex = metadata.getMobiHeader().firstImageIndex();
        int imageIndex = firstImageIndex + coverIndex;
        int imageOffset = metadata
                .getPalmDatabaseHeader()
                .records()
                .get(imageIndex)
                .offset();
        int nextRecordOffset = metadata
                .getPalmDatabaseHeader()
                .records()
                .get(imageIndex + 1)
                .offset();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            int len = nextRecordOffset - imageOffset;
            byte[] buf = new byte[len];
            raf.seek(imageOffset);
            raf.readFully(buf);
            return new Image(new ByteArrayInputStream(buf));
        } catch (IOException ex) {
            log.error("Could not read book file: ", ex);
            throw new RuntimeException(ex);
        }
    }

    public boolean leftToRight() {
        List<Azw3Metadata.EXTHRecord> records = this.metadata.getExthHeader().records();
        Optional<Azw3Metadata.EXTHRecord> record = records.stream().filter(r -> r.recordType() == 527 || r.recordType() == 525).findFirst();
        if(record.isEmpty()) {
            return true; // if there is no direction defined use standard left to right
        }
        String writingMode = new String(record.get().recordData(), StandardCharsets.UTF_8);
        return !writingMode.equals("rtl") && !writingMode.equals("horizontal-rl");
    }

    public int getCoverIndex() {
        List<Azw3Metadata.EXTHRecord> records = this.metadata.getExthHeader().records();
        Optional<Azw3Metadata.EXTHRecord> coverRecord = records.stream()
                .filter(r -> r.recordType() == 201)
                .findFirst();
        return coverRecord.map(exthRecord -> ByteBuffer.wrap(exthRecord.recordData()).getInt()).orElse(-1);
    }

    public int getThumbnailIndex() {
        List<Azw3Metadata.EXTHRecord> records = this.metadata.getExthHeader().records();
        Optional<Azw3Metadata.EXTHRecord> thumbRecord = records.stream()
                .filter(r -> r.recordType() == 202)
                .findFirst();
        return thumbRecord.map(exthRecord -> ByteBuffer.wrap(exthRecord.recordData()).getInt()).orElse(-1);
    }

    public int getResourceCount() {
        List<Azw3Metadata.EXTHRecord> records = this.metadata.getExthHeader().records();
        Optional<Azw3Metadata.EXTHRecord> thumbRecord = records.stream()
                .filter(r -> r.recordType() == 125)
                .findFirst();
        return thumbRecord.map(exthRecord -> ByteBuffer.wrap(exthRecord.recordData()).getInt()).orElse(-1);
    }

    public int getPageCount() {
        return this.pageCount;
    }
}
