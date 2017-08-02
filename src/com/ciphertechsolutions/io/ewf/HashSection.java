package com.ciphertechsolutions.io.ewf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Adler32;

/**
 * Hash section is optional, it does not need to be present in an EWF file. It resides in the last segment file before the done section.
 *
 */
public class HashSection extends Section {
    private static final int ADDITIONAL_SECTION_SIZE = 36;
    private final byte[] md5Hash;
    private final static byte[] padding = new byte[16]; // Possibly put stuff in here?

	public HashSection(long currentOffset, byte[] md5Hash) {
		super(currentOffset, "hash");
		this.md5Hash = md5Hash;
		this.sectionSize += ADDITIONAL_SECTION_SIZE;
		this.nextOffset += ADDITIONAL_SECTION_SIZE;
	}

	public byte[] getFullBytes() {
	    ByteBuffer buffer = ByteBuffer.allocate(ADDITIONAL_SECTION_SIZE);
	    buffer.order(ByteOrder.LITTLE_ENDIAN);
	    buffer.put(md5Hash);
	    buffer.put(padding);
	    buffer.putInt(getSecondaryAdler32());
	    return buffer.array();
	}

    private int getSecondaryAdler32() {
        Adler32 adlerCalc = new Adler32();
        adlerCalc.update(md5Hash);
        adlerCalc.update(padding);
        return (int) adlerCalc.getValue();
    }
}
