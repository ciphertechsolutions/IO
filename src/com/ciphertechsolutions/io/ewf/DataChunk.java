package com.ciphertechsolutions.io.ewf;

/**
 * Chunks of data for converting into sectors for ewf output.
 */
public class DataChunk {
    /**
     * The raw data.
     */
    public final byte[] data;
    /**
     * The size of the data, in bytes.
     */
    public final int size;
    /**
     * The size of the uncompressed data, may be equal to size if {@link #compressed} is false.
     */
    public final int originalSize;
    /**
     * Whether this data is compressed.
     */
    public final boolean compressed;

    /**
     * Makes a {@link DataChunk} with the given data.
     * @param originalSize The uncompressed size of the data.
     * @param data The (possibly compressed) data.
     * @param compressed Whether or not the data given is compressed.
     */
    public DataChunk(int originalSize, byte[] data, boolean compressed) {
        this.data = data;
        this.originalSize = originalSize;
        this.size = this.data.length;
        this.compressed = compressed;
    }
}
