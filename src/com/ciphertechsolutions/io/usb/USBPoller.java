package com.ciphertechsolutions.io.usb;

import com.ciphertechsolutions.io.applicationLogic.IProcessController;
import com.ciphertechsolutions.io.applicationLogic.IStoppable;
import com.ciphertechsolutions.io.device.Disk;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

/**
 * A poller to monitor the connected devices as well as the USBWriteBlock registry key status.
 */
public class USBPoller implements IStoppable {

    private final Thread pollingThread;
    private boolean running = false;
    private final IProcessController manager;
    private volatile Object hasPolled = new Object();
    private final SimpleBooleanProperty isWriteblockEnabled;

    /**
     * Sole constructor. Will notify the given {@link IProcessController} of any newly connected drives.
     * @param manager
     */
    public USBPoller(IProcessController manager) {
        this.manager = manager;
        this.isWriteblockEnabled = new SimpleBooleanProperty(UsbWriteBlock.getWriteBlockStatus());
        pollingThread = new Thread(new Poller(), "DevicePolling");
    }

    /**
     * Begins polling for connected devices and USBWriteBlock status.
     */
    public void startPolling() {
        running = true;
        pollingThread.start();
        waitForFirstPoll();
    }

    /**
     * Stops polling for connected devices and USBWriteBlock status.
     */
    public void stopPolling() {
        running = false;
        pollingThread.interrupt();
    }

    /**
     * Waits until the initial polling for devices and USBWriteBlock status is completed. Will wait a maximum of 6.5 seconds.
     */
    public void waitForFirstPoll() {
        synchronized (hasPolled) {
            try {
                hasPolled.wait(6500);
            }
            catch (InterruptedException e) {
                // We tried to wait.
            }
        }
    }

    /**
     * Register a listener for changes in USBWriteBlock status.
     * @param listener The listener to register.
     */
    public void addWriteBlockListener(ChangeListener<Boolean> listener) {
        isWriteblockEnabled.addListener(listener);
    }

    /**
     * Unregister a listener for changes in USBWriteBlock status.
     * @param listener The listener to unregister.
     */
    public void removeWriteBlockListener(ChangeListener<Boolean> listener) {
        isWriteblockEnabled.removeListener(listener);
    }

    @Override
    public void stop() {
        stopPolling();
    }

    private void notifyDisk(Disk insertedDisk) {
        manager.handleDiskInsertion(insertedDisk);
    }

    class Poller implements Runnable {

        @Override
        public void run() {
            synchronized (hasPolled) {
                manager.getAvailableDisks();
                hasPolled.notifyAll();
            }
            Disk insertedDisk = null;
            while (running) {
                while (running && insertedDisk == null) {
                    try {
                        Thread.sleep(2500);
                        insertedDisk = manager.findNewDisk();
                        boolean currentStatus = UsbWriteBlock.getWriteBlockStatus();
                        if (currentStatus != isWriteblockEnabled.get()) {
                            isWriteblockEnabled.set(currentStatus);
                        }
                    }
                    catch (InterruptedException e) {
                        // Don't care.
                    }
                }
                if (insertedDisk != null) {
                    notifyDisk(insertedDisk);
                }
                insertedDisk = null;
            }
        }

    }
}
