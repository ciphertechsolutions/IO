package com.ciphertechsolutions.io.ewf;


/**
 * Resides after the {@link com.ciphertechsolutions.io.ewf.VolumeSection} in the first segment file
 * or after the {@link com.ciphertechsolutions.io.ewf.DataSection} in other segment files.
 * Default size is 32k 64 sectors * 512 bytes. First chunk is located at offset 76
 * The most significant bit in the offset in the table section defines if a chunk is compressed or not.
 *
 * Stores chunk data.
 */
public class SectorsSection extends Section {

    public SectorsSection(long currentOffset) {
        super(currentOffset, "sectors");
    }

    public void add(int length) {
        this.sectionSize += length;
        this.nextOffset += length;
    }

}
