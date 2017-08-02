package com.ciphertechsolutions.io.device;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Represents storage devices.
 */
public abstract class Device {

    /**
     *  The simple name of a device.
     */
    protected String name;

    /// The path to read the device with.
    protected String path;

    /**
     *  The size of the device in bytes.
     */
    protected long size;

    /**
     * Gives detailed information about this device as a printable string.
     * @return A printable string of device information.
     */
    public abstract String details();

    /**
     * Converts the device name to a format appropriate for outputting as a file name. Does not include file extension.
     * @return A valid file name containing device-relevant information.
     */
    public abstract String toFileNameString();

    /**
     *
     * @return The device path.
     */
    public Path getPath() {
    	return Paths.get(path);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the device serial number
     * @return The serial number.
     */
    public abstract String getSerialNumber();

    protected static String trimPropertyName(String s) {
        if (s.contains(":")) {
            return s.substring(s.indexOf(":") + 1).trim();
        }
        return s;
    }

    /**
     * @return the size
     */
    public long getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(long size) {
        this.size = size;
    }
}
