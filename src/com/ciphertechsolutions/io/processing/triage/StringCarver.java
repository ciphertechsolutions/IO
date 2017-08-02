package com.ciphertechsolutions.io.processing.triage;

import java.util.concurrent.TimeUnit;

import com.ciphertechsolutions.io.logging.Logging;

import javafx.util.Pair;

/**
 * A processor that can find strings while imaging the drive.
 */
public class StringCarver extends TriageProcessorBase {

    /**
     * The default minimum string length.
     */
    private final int DEFAULT_STRING_LENGTH = 10;

    private final int DEFAULT_RANDOM_THRESHOLD = 20;

    /**
     * Sole constructor.
     * @param readSize The default size of chunks that will be passed to this carver, used for memory management purposes.
     */
    public StringCarver(int readSize) {
        super("StringCarver", readSize);
    }

    @Override
    public void initialize() {
        startThreads();
    }

    @Override
    protected int getThreadCount() {
        return 1;
    }

    @Override
    protected void internalProcess() {
        try {
            while (isRunning && !Thread.currentThread().isInterrupted())
            {
                Pair<byte[], Long> toRead;
                toRead = byteQueue.poll(5, TimeUnit.SECONDS);
                if (toRead == null) {
                    continue;
                }
                byte[] bytesToRead = toRead.getKey();
                if (bytesToRead.length == 0)
                {
                    isRunning = false;
                    return;
                }
                ByteUtils.printableSpansWithIndexes(bytesToRead, DEFAULT_STRING_LENGTH, false, DEFAULT_RANDOM_THRESHOLD);
                // TODO: What to do with this?
            }
        } catch (InterruptedException e) {
            Logging.log(e);
            return;
        }
    }

}
