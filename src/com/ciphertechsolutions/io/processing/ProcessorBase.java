package com.ciphertechsolutions.io.processing;

import com.ciphertechsolutions.io.logging.Logging;

/**
 * Provides some of the basic functionality required to implement {@link IProcessor}. This implementation
 * assumes that the process will be spawning and managing child threads.
 */
public abstract class ProcessorBase implements IProcessor {
    protected boolean isRunning = true;
    private final Thread[] threads;
    private final String threadName;

    protected ProcessorBase(String threadName) {
       threads = new Thread[getThreadCount()];
       this.threadName = threadName;
    }

    protected abstract int getThreadCount();

    protected abstract void internalProcess();

    protected void startThreads() {
        for (int i =0; i< getThreadCount(); i++)
        {
            threads[i] = new Thread(() -> {internalProcess();}, threadName + (i > 0 ? i : ""));
            threads[i].start();
        }
    }

    protected void waitForThreads() {
        for (Thread thread : threads)
        {
            try {
                thread.join();
            }
            catch (InterruptedException e) {
                Logging.log(e);
                return;
            }
        }
    }

    @Override
    public void waitForExit() {
        waitForThreads();
    }

    @Override
    public void cancel() {
        cancel(true);
    }

    /**
     * Interrupts all threads that have been spawned.
     * @param wait A boolean to determine if this method waits for the threads to halt before returning.
     */
    public void cancel(boolean wait) {
        isRunning = false;
        for (Thread thread : threads)
        {
            thread.interrupt();
        }
        if (wait) {
            waitForThreads();
        }
    }


    @Override
    public void stop() {
        cancel(false);
    }
}
