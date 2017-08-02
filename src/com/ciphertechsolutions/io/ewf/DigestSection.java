package com.ciphertechsolutions.io.ewf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Adler32;

/**
 * TODO: Replacement for {@link com.ciphertechsolutions.io.ewf.HashSection}?
 * Has section data type of 'hash' found in EnCase 6.12.
 * Additional digest section is 80 byte of size and consist of below fields.
 */
public class DigestSection extends Section {
    private static final int ADDITIONAL_SECTION_SIZE = 80;
    private final byte[] md5Hash;
    private final byte[] sha1Hash;
    private final static byte[] padding = new byte[40];

    public DigestSection(long currentOffset, byte[] md5Hash, byte[] sha1Hash) {
        super(currentOffset, "digest");
        this.md5Hash = md5Hash;
        this.sha1Hash = sha1Hash;
        this.sectionSize += ADDITIONAL_SECTION_SIZE;
        this.nextOffset += ADDITIONAL_SECTION_SIZE;
    }

    public byte[] getFullBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(ADDITIONAL_SECTION_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(md5Hash);
        buffer.put(sha1Hash);
        buffer.put(padding);
        buffer.putInt(getSecondaryAdler32());
        return buffer.array();
    }

    private int getSecondaryAdler32() {
        Adler32 adlerCalc = new Adler32();
        adlerCalc.update(md5Hash);
        adlerCalc.update(sha1Hash);
        adlerCalc.update(padding);
        return (int) adlerCalc.getValue();
    }
}
