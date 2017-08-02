package com.ciphertechsolutions.io.ewf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.Adler32;

import com.ciphertechsolutions.io.processing.triage.ByteUtils;


/**
 * A base class for sections for EnCase6 Format.
 *
 */
public abstract class Section {

    protected static final int SECTION_HEADER_SIZE = 76;

    private static final int MAX_TYPE_LENGTH = 16;

    // Max length of 16 bytes, should we just use a string?
    String typeString;

    protected long currentOffset;

    protected long nextOffset;

    protected long sectionSize;

    private static final byte[] padding = new byte[40];


    // This goes at the very end of the section header.
    int adler32;

    protected Section(String typeString) {
        this(0, typeString);
    }

    protected Section(long currentOffset, String typeString) {
        this(currentOffset, typeString, currentOffset + SECTION_HEADER_SIZE, SECTION_HEADER_SIZE);
    }

    protected Section(long currentOffset, String typeString, long nextOffset, long sectionSize) {
        this.currentOffset = currentOffset;
        if (typeString.length() > MAX_TYPE_LENGTH) {
            throw new IllegalArgumentException();
        }
        this.typeString = typeString;
        this.nextOffset = nextOffset;
        this.sectionSize = sectionSize;
    }

    /**
     * Get the section header bytes, although initialized with only the type string.
     * @return A mostly empty byte array that can serve as a placeholder for the section header.
     */
    public byte[] getInitialHeader() {
        return Arrays.copyOf(typeString.getBytes(), SECTION_HEADER_SIZE);
    }

    /**
     * Get the section header as a byte array.
     * @return A byte array containing the full section header.
     */
    public byte[] getFullHeader() {
        // TODO: Use a straight byte array instead if efficiency is an issue.
        ByteBuffer buffer = ByteBuffer.allocate(SECTION_HEADER_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Arrays.copyOf(typeString.getBytes(), MAX_TYPE_LENGTH));
        buffer.putLong(nextOffset);
        buffer.putLong(sectionSize);
        buffer.put(padding);
        buffer.putInt(getAdler32());
        return buffer.array();
    }

    /**
     * Get the offset of this section.
     * @return The offset.
     */
    public long getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(long currentOffset) {
        this.nextOffset = this.nextOffset - (this.currentOffset - currentOffset);
        this.currentOffset = currentOffset;
    }

    /**
     * Get the offset of the next section.
     * @return The offset of the next section.
     */
    public long getNextOffset() {
        return nextOffset;
    }

    /**
     * Get the adler32 checksum of the section header.
     * @return The section header checksum.
     */
    public int getAdler32() {
        if (adler32 == 0) {
            adler32 = calcAdler32();
        }
        return adler32;
    }

    private int calcAdler32() {
        Adler32 adlerCalc = new Adler32();
        adlerCalc.update(Arrays.copyOf(typeString.getBytes(), MAX_TYPE_LENGTH));
        adlerCalc.update(ByteUtils.longToBytes(nextOffset));
        adlerCalc.update(ByteUtils.longToBytes(sectionSize));
        adlerCalc.update(padding);
        return (int) adlerCalc.getValue();
    }

    /**
     * Get the size of this section.
     * @return The section size.
     */
    public long getSectionSize() {
        return sectionSize;
    }
}
