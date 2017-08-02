package com.ciphertechsolutions.io.processing;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * An interface specifying the basic operations needed to be a data source for ION's processing.
 */
public interface IMediaReader extends AutoCloseable {

    /**
     * Read the next set of bytes from the data. -1 is returned to indicate the end of file has been reached.
     * @return The number of bytes read.
     * @throws IOException An error occurred while trying to read data.
     */
    public int read() throws IOException;

    /**
     * Get the ByteBuffer backing this {@link IMediaReader}.
     * @return The ByteBuffer.
     */
    public ByteBuffer getBuffer();

    /**
     * Get the bytes read by the last {@link #read} operation. This method must be thread-safe,
     * and the array returned must not be modified further by the {@link IMediaReader}. The caller of this
     * method may freely modify the array.
     * @return The bytes read.
     */
    public byte[] getBytes();

    /**
     * Get the bytes read by the last {@link #read} operation. The array returned by this method
     * may be modified further by the {@link IMediaReader}. Wherever possible, use of {@link #getBytes} is preferred.
     * @return The bytes read.
     */
    public byte[] getUnsafeBytes();

    /**
     * Gets the total number of bytes read by this {@link IMediaReader} across all calls to {@link #read()}.
     * @return The total bytes read.
     */
    public long getBytesRead();

    /**
     * Get the default read size of this {@link IMediaReader}. Not all calls to {@link #read()} will read this many bytes.
     * @return The default read size.
     */
    public int getReadSize();
}
