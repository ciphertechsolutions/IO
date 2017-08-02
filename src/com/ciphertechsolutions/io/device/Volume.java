package com.ciphertechsolutions.io.device;

import java.io.IOException;
import java.nio.file.FileStore;

/**
 * A class representing a volume on a disk.
 */
public class Volume extends Device {

    private static final String BLANK_SERIAL = "00000000";

    /**
     *  The volume's label, or "NO NAME" if none can be found.
     */
    public String label;

    /**
     *  Volume serial number.
     */
    public String volumeSerial;

    /**
     *  The type of volume.
     */
    public String type;

    /**
     *  The file system format of the volume.
     */
    public String format;

    /**
     * The free space in the volume.
     */
    public long free;

    /**
     * The {@link Disk disk} this volume is on.
     */
    public Disk underlyingDisk;

    /**
     * A constructor to create a Volume object from a FileStore object.
     *
     * @param root
     * @param fileStore
     */
    public Volume(String root, FileStore fileStore)
    {
        label = fileStore.name(); //TODO VolumeLabel;
        name = fileStore.name();
        type = fileStore.type();
        format = fileStore.type(); // TODO DriveFormat
        path = root; // ex. C:\

        try {
            size = fileStore.getTotalSpace();
            free = fileStore.getUsableSpace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The primary constructor for a Volume object
     * @param info The wmi query result to instantiate from
     */
    public Volume(String info)
    {
        String[] lines = info.split(System.lineSeparator());
        if (lines.length >= 8) {
            path = trimPropertyName(lines[0]);
            name = trimPropertyName(lines[1]);
            format =  trimPropertyName(lines[2]);
            label = trimPropertyName(lines[3]);
            free = safeParseLong(trimPropertyName(lines[4]));
            size = safeParseLong(trimPropertyName(lines[5]));
            type = getDriveTypeByNumber(trimPropertyName(lines[6]));
            volumeSerial = formatVolumeSerial(lines[7]); // logical disk query
        }
        else {
            throw new IllegalArgumentException("Invalid volume info");
        }

    }

    private static String getDriveTypeByNumber(String driveType) {
        switch (driveType) {
            case "1":
                return "Invalid";
            case "2":
                return "Removable";
            case "3":
                return "Fixed";
            case "4":
                return "Remote";
            case "5":
                return "CD-ROM";
            case "6":
                return "RAM disk";
            case "0":
            default:
                return "Unknown";
        }
    }

    protected String formatVolumeSerial(String line) {
        String volSerial = String.format("%08X", safeParseInt(trimPropertyName(line)));
        return volSerial.substring(0,4) + "-" + volSerial.substring(4);
    }

    private static int safeParseInt(String toParse) {
        if (toParse.length() == 0) {
            return 0;
        }
        else {
            try {
                return Integer.parseInt(toParse);
            }
            catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    private static long safeParseLong(String toParse) {
        if (toParse.length() == 0) {
            return 0;
        }
        else {
            try {
                return Long.parseLong(toParse);
            }
            catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    @Override
    public String details() {
        StringBuilder sb = new StringBuilder();
        sb.append("Volume: " + (label.equals("") ? getName() : label) + System.lineSeparator());
        sb.append("Root: " + path + System.lineSeparator());
        sb.append("Volume Serial: " + volumeSerial + System.lineSeparator());
        sb.append("Format: " + format + System.lineSeparator());
        sb.append(" Size: " + getSize() + System.lineSeparator());
        sb.append(" Free: " + free + System.lineSeparator());
        return sb.toString();
    }

    @Override
    public String toFileNameString() {
        return label.replaceAll("\\W+", "_");
    }

    @Override
    public int hashCode() {
        if (!BLANK_SERIAL.equals(volumeSerial)) {
            return this.volumeSerial.hashCode();
        }
        return this.path.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Volume) {
            return equals((Volume) o);
        }
        return false;
    }

    /**
     * Determines if this volume is equal to another volume using the serial number. If the serial is lacking compares instead via path.
     * @param o The volume to compare to.
     * @return True if they are equal, false otherwise.
     */
    public boolean equals(Volume o) {
    	if (o == null) {
    		return false;
    	}
        if (BLANK_SERIAL.equals(this.volumeSerial) && BLANK_SERIAL.equalsIgnoreCase(o.volumeSerial)) {
            return o.path.equals(this.path);
        }
        return o.volumeSerial.equals(this.volumeSerial);
    }

    @Override
    public String getSerialNumber() {
        return volumeSerial;
    }
}
