package com.ciphertechsolutions.io.applicationLogic;

import java.util.Set;

import com.ciphertechsolutions.io.applicationLogic.options.AdvancedOptions;
import com.ciphertechsolutions.io.device.Device;
import com.ciphertechsolutions.io.device.Disk;
import com.ciphertechsolutions.io.ui.BaseController;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.util.StringConverter;

/**
 * An interface specifying the responsibilities of the overall application controller.
 *
 */
public interface IProcessController extends IStoppable {

    /**
     * Gets the appropriate controller to display given the current state.
     * @return The appropriate controller.
     */
    public BaseController getController();

    /**
     * Get a set containing all available disks on the system.
     * @return The set of disks.
     */
    public Set<Disk> getAvailableDisks();

    /**
     * Gets a new disk that has been inserted to the system. May return null if none are present.
     * If multiple drives are new, only one new one will be returned.
     * @return The new disk. Null if there are no new disks.
     */
    public Disk findNewDisk();

    /**
     * Performs any necessary actions upon new disk insertion.
     */
    public void handleDiskInsertion(Disk insertedDisk);

    /**
     * Get a {@link StringConverter} for {@link com.ciphertechsolutions.io.device.Disk disks}.
     * @return A StringConverter for disks.
     */
    public StringConverter<Disk> getStringConverterForDisks();

    /**
     * Sets the given device as the selected one.
     * @param device The device to select.
     */
    public void selectDevice(Device device);

    /**
     * Prepares to image the previously selected device.
     * @return True if a device was selected and imaging successfully began, false otherwise.
     */
    public boolean setupImaging();

    /**
     * Begins imaging the previously setup device. Setup must have been called first.
     */
    public void beginImaging();

    /**
     * Stops imaging the device, then calls the callback. {@link #beginImaging()} must have been called first.
     */
    public void stopImaging(Runnable callback);

    /**
     * Returns whether or not there is a {@link Device device} currently selected for imaging.
     * @return True if there is a device, false otherwise.
     */
    public boolean hasDevice();

    /**
     * Returns whether or not the currently selected save location is valid. If it does not exist it will try to create it.
     * @return True if the save location is valid, false otherwise.
     */
    public boolean checkValidSaveDirectory();

    /**
     * Registers a {@link ListChangeListener listener} to obtain all logging output from this process controller.
     * @param listener The listener to register
     */
    public void registerOutputListener(ListChangeListener<String> listener);

    /**
     * Registers a {@link ChangeListener listener} to obtain progress information from this process controller.
     * @param listener The listener to register
     */
    public void registerProgressListener(ChangeListener<Number> listener);

    /**
     * Gets the maximum progress count of this process controller. A {@link Device device} must be set first.
     * Use {@link #hasDevice()} to check for a device first.
     * @return The maximum progress, as a long.
     */
    public long getMaxOutputCount();

    /**
     * Save the given {@link AdvancedOptions options} to disk.
     * @param options The {@link AdvancedOptions options} to save.
     */
    public void saveOptions(AdvancedOptions options);

    /**
     * Set the active {@link AdvancedOptions options} for this processing controller to the given {@link AdvancedOptions options}.
     * @param options The {@link AdvancedOptions options} to set.
     */
    public void setOptions(AdvancedOptions options);

    /**
     * Registers a listener for WriteBlock changes. If the Windows registry value of the WriteBlock value is changed,
     * any registered listeners will be notified.
     * @param listener The listener to notify.
     */
    public void registerWriteBlockListener(ChangeListener<Boolean> listener);

}
