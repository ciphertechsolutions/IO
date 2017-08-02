package com.ciphertechsolutions.io.processing.triage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.ciphertechsolutions.io.logging.LogMessageType;
import com.ciphertechsolutions.io.logging.Logging;
import com.ciphertechsolutions.io.processing.ProcessorBase;

import javafx.util.Pair;

/**
 * Provides basic processing functionality to make implementing triaging classes easier.
 *
 */
public abstract class TriageProcessorBase extends ProcessorBase {
    /**
     * Set to {@link Integer#MAX_VALUE}.
     */
    private final int MAX_BACKLOG_SIZE_IN_BYTES = Integer.MAX_VALUE;
    protected final BlockingQueue<Pair<byte[], Long>> byteQueue;
    protected long currentLength = 0;
    private final String friendlyName;
    private boolean hasWarned = false;

    /**
     *
     * @param threadName
     * @param readSize
     */
    protected TriageProcessorBase(String threadName, int readSize) {
       super(threadName);
       friendlyName = threadName;
       byteQueue = new LinkedBlockingQueue<>(MAX_BACKLOG_SIZE_IN_BYTES/readSize);
    }

    /**
     * Attempts to add the given byte array to a queue with a max capacity of {@link #MAX_BACKLOG_SIZE_IN_BYTES} bytes.
     */
    @Override
    public void process(byte[] toProcess) {
        if (!byteQueue.offer(new Pair<>(toProcess, currentLength))) {
            if (!hasWarned) {
                hasWarned = true;
                Logging.log("Read speed is outpacing processor speed, " + friendlyName + " will not be able to process every byte.", LogMessageType.USER);
            }
            Logging.log("Read speed is outpacing processor speed, " + friendlyName + " was unable to process bytes "
                    + currentLength +"through " + (currentLength + toProcess.length) +".", LogMessageType.DEBUG);
            // TODO: Log ranges and report at end rather than during? Unsure.
        }
        currentLength += toProcess.length;
    }

    /**
     * Adds an empty byte array to signal the end of input.
     */
    @Override
    public void finish() {
        byteQueue.add(new Pair<>(new byte[0], currentLength));
    }

    @Override
    public void waitForExit() {
        waitForThreads();
    }

}
