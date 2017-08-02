package com.ciphertechsolutions.io.usb;

import com.ciphertechsolutions.io.logging.LogMessageType;
import com.ciphertechsolutions.io.logging.Logging;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

/**
 * A class for managing the USB Write Block registry key setting. <br>
 * Specifically, the key is: HKLM\SYSTEM\CurrentControlSet\Control\StorageDevicePolicies\WriteProtect <br>
 * Note: Write block status for a drive is determined at the time of drive insertion, not at the time of read/write.
 */
public class UsbWriteBlock {

    private static final String WRITE_BLOCK_KEY = "SYSTEM\\CurrentControlSet\\Control\\StorageDevicePolicies";

    private static boolean initial_status = getWriteBlockStatus();

    ///The associated registry key.
    private static void writeToKey(int value) {
        try {
            if (!Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, WRITE_BLOCK_KEY)) {
                Advapi32Util.registryCreateKey(WinReg.HKEY_LOCAL_MACHINE, WRITE_BLOCK_KEY);
            }
            Advapi32Util.registrySetIntValue(WinReg.HKEY_LOCAL_MACHINE, WRITE_BLOCK_KEY, "WriteProtect", value);
        }
        catch (IllegalArgumentException e) {
            Logging.log("Failed to " + (value == 0 ? "disable" : "enable") + " USB write block", LogMessageType.WARNING);
            Logging.log(e);
        }

    }

    /**
     * Gets the current setting of the write block key.
     * @return True if write block is turned on in the registry, false otherwise.
     */
    public static boolean getWriteBlockStatus() {
        if (!Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, WRITE_BLOCK_KEY)) {
            return false;
        }
        return Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, WRITE_BLOCK_KEY, "WriteProtect") == 1;

    }
    
    /**
     * Get the state of USB write block as of when ION launched.
     * @return true if write block was enabled, false if it was not.
     */
    public static boolean getInitialState() {
    	return initial_status;
    }

    /**
     * Sets the write block key back to what it was when ION launched.
     */
    public static void resetWriteBlockStatus() {
        if (!Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, WRITE_BLOCK_KEY)) {
            return;
        }
        Advapi32Util.registrySetIntValue(WinReg.HKEY_LOCAL_MACHINE, WRITE_BLOCK_KEY, "WriteProtect", initial_status ? 1 : 0);

    }

    /**
     * Enable the system write block on newly connected devices.
     */
    public static void enable()
    {
        if (getWriteBlockStatus())
        {
            return;
        }
        writeToKey(1);
    }

    /**
     *  Disable the system write block on any newly connected devices.
     */
    public static void disable()
    {
        if (!getWriteBlockStatus())
        {
            return;
        }
        writeToKey(0);
    }


}
