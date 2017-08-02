package com.ciphertechsolutions.io.applicationLogic;

import com.ciphertechsolutions.io.applicationLogic.options.AdvancedOptions;
import com.ciphertechsolutions.io.device.Device;

/**
 * A class representing the current state of the application, namely the currently active
 * {@link AdvancedOptions options configuration} and the active {@link Device device}.
 */
public class ApplicationState {
    private AdvancedOptions options;
    private Device selectedDevice;

    /**
     * The sole constructor.
     */
    public ApplicationState() {
        options = new AdvancedOptions();
    }

    /**
     * @return the options
     */
    public AdvancedOptions getOptions() {
        return options;
    }

    /**
     * @param options the options to set
     */
    public void setOptions(AdvancedOptions options) {
        this.options = options;
    }

    /**
     * @return the selectedDevice
     */
    public Device getSelectedDevice() {
        return selectedDevice;
    }

    /**
     * Sets the given {@link Device device} to be the active selected device.
     * @param device The {@link Device device} to select.
     */
    public void setSelectedDevice(Device device) {
        this.selectedDevice = device;
    }
}
