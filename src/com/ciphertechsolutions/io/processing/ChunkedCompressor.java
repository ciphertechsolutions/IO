package com.ciphertechsolutions.io.processing;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.ciphertechsolutions.io.ewf.DataChunk;
import com.ciphertechsolutions.io.logging.Logging;

/**
 * Groups input into a consistently sized chunks, then compresses those chunks. The compression is done in a
 * multi-threaded manner. The output is guaranteed to be in the same order as the input, and is accessible via
 * {@link #getOutputQueue()}.
 */
public class ChunkedCompressor extends ProcessorBase {

    private final BlockingQueue<byte[]> byteQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private final BlockingQueue<Future<DataChunk>> compressedOutputQueue = new LinkedBlockingQueue<>();
    private final int CHUNK_SIZE;
    private final static int DEFAULT_CHUNK_SIZE = 1024 * 512;
    private final ByteBuffer localBuffer;
    private final int compressionLevel;

    /**
     * Creates a ChunkedCompressor with the given chunk size and compression level.
     * @param chunkSize The size, in bytes, to group data into.
     * @param compressionLevel The level of compression to use (uses z-lib for compression).
     */
    public ChunkedCompressor(int chunkSize, int compressionLevel) {
        super("ChunkCompressor");
        CHUNK_SIZE = chunkSize;
        localBuffer = ByteBuffer.allocate(CHUNK_SIZE);
        this.compressionLevel = compressionLevel;
    }

    /**
     * Convenience constructor, equivalent to {@link #ChunkedCompressor(int, int)} called with compressionLevel, {@link #DEFAULT_CHUNK_SIZE}}.
     * @param compressionLevel The compression level to use.
     */
    public ChunkedCompressor(int compressionLevel) {
        this(DEFAULT_CHUNK_SIZE, compressionLevel);
    }

    /**
     * Default constructor, equivalent to {@link #ChunkedCompressor(int) ChunkedCompressor(1)}.
     */
    public ChunkedCompressor() {
        this(1);
    }

    /**
     * Returns the output queue. The contents of this queue are the actual output, not copies of it.
     * @return The output queue.
     */
    public BlockingQueue<Future<DataChunk>> getOutputQueue() {
        return compressedOutputQueue;
    }

    @Override
    public void internalProcess() {
        try {
            while (isRunning && !Thread.currentThread().isInterrupted())
            {
                byte[] toRead = byteQueue.poll(5, TimeUnit.SECONDS);
                if (toRead != null) {
                    if (toRead.length == 0)
                    {
                        finalizeChunkStream();
                        return;
                    }
                    int offset = 0;
                    int remainingCapacity = localBuffer.remaining();
                    boolean isBufferEmpty = localBuffer.position() == 0;
                    boolean toReadOverCapacity = toRead.length >= remainingCapacity;
                    if (!isBufferEmpty && toReadOverCapacity) {
                        offset = fillAndFlushLocalBuffer(toRead, remainingCapacity);
                    }
                    int remainingLength = toRead.length - offset;
                    while (remainingLength != 0) {
                        if (remainingLength >= CHUNK_SIZE) {
                            compressedOutputQueue.add(executor.submit(new CompressionTask(Arrays.copyOfRange(toRead, offset, offset + CHUNK_SIZE),
                                                                                          compressionLevel)));
                            offset += CHUNK_SIZE;
                            remainingLength = toRead.length - offset;
                        }
                        else {
                            localBuffer.put(toRead, offset, remainingLength);
                            remainingLength = 0;
                        }
                    }
                }
            }
        }
        catch (InterruptedException e) {
            Logging.log(e);
        }
    }

    protected int fillAndFlushLocalBuffer(byte[] toRead, int remainingCapacity) {
        localBuffer.put(toRead, 0, remainingCapacity);
        compressedOutputQueue.add(executor.submit(new CompressionTask(Arrays.copyOf(localBuffer.array(), localBuffer.position()), compressionLevel)));
        localBuffer.clear();
        return remainingCapacity;
    }

    protected void finalizeChunkStream() {
        if (localBuffer.position() != 0) {
            compressedOutputQueue.add(executor.submit(new CompressionTask(Arrays.copyOf(localBuffer.array(), localBuffer.position()), compressionLevel)));
            localBuffer.clear();
        }
        isRunning = false;
        compressedOutputQueue.add(executor.submit(new CompressionTask(new byte[0], compressionLevel)));
        executor.shutdown();
    }

    @Override
    public void initialize() {
        startThreads();
    }

    @Override
    public void finish() {
        byteQueue.add(new byte[0]);
    }

    @Override
    public void process(byte[] toProcess) {
        byteQueue.add(toProcess);
    }

    @Override
    protected int getThreadCount() {
        return 1;
    }

}
