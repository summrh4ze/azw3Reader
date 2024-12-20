package org.example.miraireader.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Azw3Metadata {
    private static final Logger log = LoggerFactory.getLogger(Azw3Metadata.class);

    public record PalmDatabaseRecord(
            int offset,
            boolean isSecret,
            boolean inUse,
            boolean isDirty,
            boolean deleteOnNextHotSync,
            int uniqueId
    ){}

    public record PalmDatabaseHeader(
            String name,
            boolean readOnly,
            boolean dirtyAppInfoArea,
            boolean backupDatabase,
            boolean canOverwriteInstall,
            boolean resetAfterInstall,
            boolean disallowCopy,
            int fileVersion,
            int creationDate,
            int modificationDate,
            int lastBackupDate,
            int modificationNumber,
            int appInfoId,
            int sortInfoId,
            String type,
            String creator,
            int uniqueIdSeed,
            int recordsNumber,
            List<PalmDatabaseRecord> records
    ){}

    public record PalmDocHeader(
            int compression,
            int bookLen,
            int recordCount,
            int maxRecordSize,
            int encryptionType
    ){}

    public record MobiHeader(
            String identifier,
            int headerLen,
            int mobiType,
            int encoding,
            int firstRecordNumber,
            int fullNameOffset,
            int fullNameLength,
            int locale,
            int firstImageIndex
            //int firstContentIndex,
            //int lastContentIndex
    ){}

    public record EXTHRecord(
            int recordType,
            int recordLen,
            byte[] recordData
    ){}

    public record EXTHHeader(
            String identifier,
            int headerLen,
            int recordCount,
            List<EXTHRecord> records
    ){}

    private final PalmDatabaseHeader palmDatabaseHeader;
    private final PalmDocHeader palmDocHeader;
    private final MobiHeader mobiHeader;

    private Azw3Metadata(
            PalmDatabaseHeader palmDatabaseHeader,
            PalmDocHeader palmDocHeader,
            MobiHeader mobiHeader
    ) {
        this.palmDatabaseHeader = palmDatabaseHeader;
        this.palmDocHeader = palmDocHeader;
        this.mobiHeader = mobiHeader;
    }

    public static Azw3Metadata of(File file) throws IOException {
        try(RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            PalmDatabaseHeader pdh = readPalmDatabaseHeader(raf);
            long zeroRecordOffset = Integer.toUnsignedLong(pdh.records().getFirst().offset);
            raf.seek(zeroRecordOffset);
            PalmDocHeader pdo = readPalmDocHeader(raf);
            MobiHeader mobih = readMobiHeader(raf);
            return new Azw3Metadata(pdh, pdo, mobih);
        } catch (IOException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
    }

    private static PalmDatabaseHeader readPalmDatabaseHeader(RandomAccessFile raf) throws IOException {
        // offset 0, size 32 bytes = Palm database name
        byte[] buf = new byte[32];
        raf.readFully(buf);
        String name = new String(buf, StandardCharsets.UTF_8);

        // offset 32, size 2 = attributes
        short bitset = raf.readShort();
        boolean readOnly = (bitset & 0x0002) == 0x0002;
        boolean dirtyAppInfoArea = (bitset&0x0004) == 0x0004;
        boolean backupDatabase = (bitset&0x0008) == 0x0008;
        boolean canOverwriteInstall = (bitset&0x0010) == 0x0010;
        boolean resetAfterInstall = (bitset&0x0020) == 0x0020;
        boolean disallowCopy = (bitset&0x0040) == 0x0040;

        // offset 34, size 2 = file version
        short fileVersion = raf.readShort();

        // offset 36, size 4 = creation date timestamp
        int creationDate = raf.readInt();

        // offset 40, size 4 = modifcation date timestamp
        int modificationDate = raf.readInt();

        // offset 44, size 4 = last backup date
        int lastBackupDate = raf.readInt();

        // offset 48, size 4 = modification number
        int modificationNumber = raf.readInt();

        // offset 52, size 4 = app info id
        int appInfoId = raf.readInt();

        // offset 56, size 4 = sort info id
        int sortInfoId = raf.readInt();

        // offset 60, size 4 = type
        buf = new byte[4];
        raf.readFully(buf);
        String type = new String(buf, StandardCharsets.UTF_8);

        // offset 64, size 4 = creator
        raf.readFully(buf);
        String creator = new String(buf, StandardCharsets.UTF_8);

        // offset 68, size 4 = unique Id seed
        int uniqueIdSeed = raf.readInt();

        // offset 72, size 4 = next record list id should be 0
        int nextRecordListId = raf.readInt();
        if(nextRecordListId != 0) {
            throw new RuntimeException("NextRecordListId should be 0");
        }

        // offset 76, size 2 = number of records
        short recordsNumber = raf.readShort();

        List<PalmDatabaseRecord> records = new ArrayList<>(recordsNumber);

        for (int i = 0; i < recordsNumber; i++) {
            // size 4 = record offset
            int offset = raf.readInt();

            // size 1 = record attributes
            byte attributes = raf.readByte();
            boolean isSecret = (attributes&0x10) == 0x10;
            boolean inUse = (attributes&0x20) == 0x20;
            boolean isDirty = (attributes&0x40) == 0x40;
            boolean deleteOnNextHotSync = (attributes&0x80) == 0x80;

            // size 3 = unique id
            short mostSignificant = raf.readShort();
            byte leastSignificant = raf.readByte();
            int uniqueId = (mostSignificant << 8) | leastSignificant;

            records.add(new PalmDatabaseRecord(offset, isSecret, inUse, isDirty, deleteOnNextHotSync, uniqueId));
        }

        return new PalmDatabaseHeader(
                name,
                readOnly,
                dirtyAppInfoArea,
                backupDatabase,
                canOverwriteInstall,
                resetAfterInstall,
                disallowCopy,
                fileVersion,
                creationDate,
                modificationDate,
                lastBackupDate,
                modificationNumber,
                appInfoId,
                sortInfoId,
                type,
                creator,
                uniqueIdSeed,
                recordsNumber,
                records
        );
    }

     private static PalmDocHeader readPalmDocHeader(RandomAccessFile raf) throws IOException {
         short compression = raf.readShort();

         raf.skipBytes(2);

         int bookLen = raf.readInt();
         short recordCount = raf.readShort();
         short recordSize = raf.readShort();
         short encryptionType = raf.readShort();

         raf.skipBytes(2);

        return new PalmDocHeader(
                compression,
                bookLen,
                recordCount,
                recordSize,
                encryptionType
        );
    }

    private static MobiHeader readMobiHeader(RandomAccessFile raf) throws IOException {
        byte[] buf = new byte[4];
        raf.readFully(buf);
        String identifier = new String(buf, StandardCharsets.UTF_8);

        int headerLen = raf.readInt();

        int mobiType = raf.readInt();

        int textEnc = raf.readInt();

        raf.skipBytes(48);

        int firstRecord = raf.readInt();

        int fullNameOffset = raf.readInt();

        int fullNameLen = raf.readInt();

        int locale = raf.readInt();

        raf.skipBytes(12);

        int firstImageIndex = raf.readInt();

        return new MobiHeader(
                identifier,
                headerLen,
                mobiType,
                textEnc,
                firstRecord,
                fullNameOffset,
                fullNameLen,
                locale,
                firstImageIndex
        );
    }

    public PalmDatabaseHeader getPalmDatabaseHeader() {
        return palmDatabaseHeader;
    }

    public PalmDocHeader getPalmDocHeader() {
        return palmDocHeader;
    }

    public MobiHeader getMobiHeader() {
        return mobiHeader;
    }
}
