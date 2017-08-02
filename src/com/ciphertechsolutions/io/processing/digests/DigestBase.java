package com.ciphertechsolutions.io.processing.digests;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.ciphertechsolutions.io.logging.Logging;
import com.ciphertechsolutions.io.processing.ProcessorBase;

/**
 * A class for computing digest hashes during imaging.
 */
public abstract class DigestBase extends ProcessorBase implements Callable<byte[]> {

    protected DigestBase(String threadName) {
        super(threadName);
    }

    private MessageDigest digest;
    private final Object lock = new Object();
    private volatile byte[] md5 = null;
    private final BlockingQueue<byte[]> byteQueue = new LinkedBlockingQueue<>();

    @Override
    public void initialize() {
        try {
            digest = getDigest();
            startThreads();
        }
        catch (NoSuchAlgorithmException e) {
            // What to do here?
            Logging.log(e);
        }
    }

    protected abstract MessageDigest getDigest() throws NoSuchAlgorithmException;

    /**
     * Get the hash resulting from this digest. This method will return immediately, but accessing the contents of the
     * {@link FutureTask} will block until the digest is complete.
     * @return A {@link FutureTask} that will contain the hash upon completion.
     */
    public FutureTask<byte[]> getDigestResult() {
        return new FutureTask<>(this);
    }

    @Override
    public byte[] call() {
        synchronized(lock) {
            while (md5 == null) {
                try {
                    lock.wait(5000);
                }
                catch (InterruptedException e) {
                    return null;
                }
            }
            return md5;
        }
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

    @Override
    protected void internalProcess() {
        try {
            while (isRunning && !Thread.currentThread().isInterrupted())
            {
                byte[] toRead = byteQueue.poll(5, TimeUnit.SECONDS);
                if (toRead != null) {
                    if (toRead.length == 0)
                    {
                        synchronized(lock) {
                            md5 = digest.digest();
                            lock.notifyAll();
                        }
                        return;
                    }
                    digest.update(toRead);
                }
            }
        }
        catch (InterruptedException e) {
            Logging.log(e);
        }

    }

}
