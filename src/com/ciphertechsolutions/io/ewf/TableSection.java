package com.ciphertechsolutions.io.ewf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.Adler32;

import com.ciphertechsolutions.io.processing.triage.ByteUtils;

/**
 * Every segment file contains its own table section. It resides after the {@link com.ciphertechsolutions.io.ewf.SectorsSection}.
 *
 */
public class TableSection extends Section {

    private static final int MAX_ENTRIES = 16375; //TODO: Maybe 65534?
    // If needed, store as byte arrays instead to convert only once.
    private final List<Integer> offsetArray;
    private final Adler32 offsetChecksumCalc = new Adler32();
    private int secondaryAdler32 = 0;
    private int tableAdler32 = 0;

    public TableSection(long baseOffset) {
        this("table", baseOffset);
    }

    protected TableSection(String sectionName, long baseOffset) {
        super(sectionName);
        this.baseOffset = baseOffset;
        sectionSize += 28;
        nextOffset += 28;
        offsetArray = Collections.synchronizedList(new ArrayList<>()); //TODO: Do we need to bother with something threadsafe here?
    }

    protected TableSection(String sectionName, long currentOffset, long baseOffset, long sectionSize,
                           List<Integer> offsetArray, int secondaryAdler32, int tableAdler32) {
        super(currentOffset, sectionName, currentOffset + sectionSize, sectionSize);
        this.baseOffset = baseOffset;
        this.offsetArray = offsetArray;
        this.secondaryAdler32 = secondaryAdler32;
        this.tableAdler32 = tableAdler32;
    }
    int tableEntries;

    // 4 bytes of padding.
    static final byte[] padding = {0x00, 0x00, 0x00, 0x00};

    private final long baseOffset;

    static final byte[] morePadding = padding;

    public boolean isFull() {
        return offsetArray.size() == MAX_ENTRIES;
    }

    public long getBaseOffset() {
        return baseOffset;
    }

    public List<Integer> getArray() {
        return offsetArray;
    }

    public void add(long offset, boolean compressed) {
        //TODO: Do we need the actual chunk data to calculate a checksum?
        int relativeOffset = compressed ? (1 << 31) | (int) (offset - baseOffset) : (int) (offset - baseOffset);
        offsetArray.add(relativeOffset);
        offsetChecksumCalc.update(ByteUtils.intToBytes(relativeOffset));
        tableEntries++;
        sectionSize += 4;
        nextOffset += 4;
    }

    public int getSecondaryAdler32(){
        if (secondaryAdler32 == 0) {
            Adler32 adlerCalc = new Adler32();
            adlerCalc.update(ByteUtils.intToBytes(tableEntries));
            adlerCalc.update(padding);
            adlerCalc.update(ByteUtils.longToBytes(baseOffset));
            adlerCalc.update(morePadding);
            secondaryAdler32 = (int) adlerCalc.getValue();
        }
        return secondaryAdler32;
    }

    public int getTableAdler32(){
        if (tableAdler32 == 0) {
            tableAdler32 = (int) offsetChecksumCalc.getValue();
        }
        return tableAdler32;
    }
}
