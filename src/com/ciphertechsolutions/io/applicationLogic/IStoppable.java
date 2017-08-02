package com.ciphertechsolutions.io.applicationLogic;

/**
 * A basic interface for processing that can be reliably halted if needed.
 */
public interface IStoppable {
    /**
     * Stops any running threads that may have been spawned.
     */
    public void stop();
}
