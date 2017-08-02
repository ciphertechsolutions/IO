package com.ciphertechsolutions.io.processing;

import com.ciphertechsolutions.io.applicationLogic.IStoppable;

/**
 * An interface designed for interacting with {@link ProcessorManager} to process the contents of a media device.
 * Data will be given in discrete chunks via {@link #process(byte[])}.
 */
public interface IProcessor extends IStoppable {
    /**
     * Process the given byte array. This method should return as quickly as possible as any delays in this method will delay all processors.
     * @param toProcess
     */
    public void process(byte[] toProcess);

    /**
     * This method will be called before {@link #process(byte[])} and can be used for any initialization needed.
     */
    public void initialize();

    /**
     * This method will be called at some point after the final call of {@link #process(byte[])} to indicate that
     * all data has been given. There is no need to have this method block until all processing is complete,
     * {@link #waitForExit()} should be used for that purpose instead.
     */
    public void finish();

    /**
     * This method will be called at some point after {@link #finish()} is called. Use this method to block
     * until all processing is complete.
     */
    public void waitForExit();

    /**
     * Called when processing should be terminated early. Any and all threads spawned should be terminated in an
     * orderly fashion when this method is called.
     */
    public void cancel();
}
