package com.ciphertechsolutions.io.device;

import java.util.HashSet;
import java.util.Set;

/**
 * A class representing Disks.
 *
 */
public class Disk extends Device implements Comparable<Device> {

    /**
     * Hardware serial number.
     */
    public String serial;

    /**
     * The model of the physical disk.
     */
    public String model;

    /**
     * The number of heads on the disk.
     */
    public int heads;

    /**
     * The number of cylinders on the disk.
     */
    public long cylinders;

    /**
     * The number of tracks on the disk.
     */
    public long tracks;

    /**
     * The number of sectors on the disk.
     */
    public long sectors;

    /**
     * The number of tracks per disk cylinder.
     */
    public int tracksPerCylinder;

    /**
     * The number of sectors per disk track.
     */
    public int sectorsPerTrack;

    /**
     * The number of bytes per disk sector.
     */
    public int bytesPerSector;

    /**
     * PNP device ID for correlating to USB connectors.
     */
    public String pnpDeviceID;

    /**
     * Physical Device ID
     */
    public String physicalSerial;

    /**
     * VID From USB Device
     */
    public String VID;

    /**
     * Vendor name
     */
    public String vendorName;

    /**
     * PID From USB Device
     */
    public String PID;

    /**
     * The system name for the drive
     */
    public String systemName;

    /**
     * The disk type
     */
    public String mediaType;

    private final Set<Volume> volumes = new HashSet<>();

    /**
     * Create a new disk object from a management object.
     *
     * @param info
     *            The DiskDrive management object.
     */
    public Disk(String info) {
        String[] lines = info.split(System.lineSeparator());
        if (lines.length >= 14) {
            try {
                name = trimPropertyName(lines[0]);
                systemName = trimPropertyName(lines[1]);
                serial = trimPropertyName(lines[2]).replace("\\x1b", "").trim();
                model = trimPropertyName(lines[3]);
                path = trimPropertyName(lines[0]); // DeviceID
                size = Long.parseLong(trimPropertyName(lines[4]));
                heads = Integer.parseInt(trimPropertyName(lines[5]));
                cylinders = Long.parseLong(trimPropertyName(lines[6]));
                tracks = Long.parseLong(trimPropertyName(lines[7])); // TotalTracks
                sectors = Long.parseLong(trimPropertyName(lines[8])); // TotalSectors
                tracksPerCylinder = Integer.parseInt(trimPropertyName(lines[9]));
                sectorsPerTrack = Integer.parseInt(trimPropertyName(lines[10]));
                bytesPerSector = Integer.parseInt(trimPropertyName(lines[11]));
                pnpDeviceID = trimPropertyName(lines[12]);
                initializePhysicalSerialNumber();
                mediaType = trimPropertyName(lines[13]);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid disk info");
            }
        }
        else {
            throw new IllegalArgumentException("Invalid disk info");
        }
    }

    private void initializePhysicalSerialNumber() {
        String[] pnpPathArray = pnpDeviceID.split("\\\\");
        String physSerial = pnpPathArray[pnpPathArray.length - 1];
        if (physSerial.startsWith("0&")) {
            physicalSerial = physSerial.substring(2);
        }
        else if (physSerial.endsWith("&0")) {
            physicalSerial = physSerial.substring(0, physSerial.length() - 2);
        }
        else {
            physicalSerial = physSerial;
        }
    }

    /**
     * Provides a detailed report on the disk.
     */
    @Override
    public String details() {
        StringBuilder sb = new StringBuilder();
        sb.append("Disk: " + getName() + System.lineSeparator());
        sb.append("Type: " + mediaType + System.lineSeparator());
        sb.append("Serial: " + serial + System.lineSeparator());
        sb.append("Model: " + model + System.lineSeparator());
        sb.append("Size: " + getSize() + System.lineSeparator());
        sb.append("PNPDeviceID: " + pnpDeviceID + System.lineSeparator());
        sb.append("Physical Serial: " + physicalSerial + System.lineSeparator());
        if (VID != null) {
            sb.append("Vendor ID: " + VID + System.lineSeparator());
            String vendor = VIDLookup.getVendorByVID(VID);
            if (vendor == null) {
                vendor = vendorName;
            }
            sb.append("Vendor: " + vendor + System.lineSeparator());
        }
        if (PID != null) {
            sb.append("Product ID: " + PID + System.lineSeparator());
        }
        sb.append("Geometry:" + System.lineSeparator());
        sb.append(" Heads: " + heads + System.lineSeparator());
        sb.append(" Cylinders: " + cylinders + System.lineSeparator());
        sb.append(" Tracks: " + tracks + System.lineSeparator());
        sb.append(" Sectors: " + getSize() / bytesPerSector + System.lineSeparator());
        if (this.volumes.size() > 0) {
            sb.append("Volumes: " + System.lineSeparator());
            for (Volume vol : volumes) {
                sb.append(vol.details());
            }
        }
        return sb.toString();
    }

    /**
     * Associate a volume with this disk.
     *
     * @param toAdd
     *            The volume to add.
     */
    public void addVolume(Volume toAdd) {
        this.volumes.add(toAdd);
    }

    /**
     * Get the volumes associated with this disk.
     *
     * @return The set of volumes associated with this disk.
     */
    public Set<Volume> getVolumes() {
        return this.volumes;
    }

    @Override
    public String toString() {
        return model;
    }

    @Override
    public String toFileNameString() {
        String cleanedSerial = serial.replace("\\W+", "");
        return model.replaceAll("\\W+", "_") + "_" + cleanedSerial.substring(Math.max(0, cleanedSerial.length() - 5));
    }

    /**
     * Note: this class has a natural ordering that is inconsistent with equals.
     */
    @Override
    public int compareTo(Device o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public int hashCode() {
        return this.pnpDeviceID.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Disk) {
            return equals((Disk) o);
        }
        return false;
    }

    /**
     * Determines if this disk is equal to the given disk. Performs a simple
     * name check.
     *
     * @param d
     *            The disk to compare to
     * @return true if the disks are equal, false otherwise.
     */
    public boolean equals(Disk d) {
        return d.pnpDeviceID.equals(this.pnpDeviceID);
    }

    @Override
    public String getSerialNumber() {
        return serial;
    }
}
