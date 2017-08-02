package com.ciphertechsolutions.io.device;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ciphertechsolutions.io.logging.LogMessageType;
import com.ciphertechsolutions.io.logging.Logging;
import com.profesorfalken.wmi4java.WMI4Java;
import com.profesorfalken.wmi4java.WMIClass;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.LARGE_INTEGER;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.Winioctl;
import com.sun.jna.platform.win32.WinioctlUtil;
import com.sun.jna.ptr.IntByReference;

/**
 * A class to help manage disks and other devices.
 */
public class DeviceManager {

    private static final Pattern VOLUME_PATTERN = Pattern.compile("\"([A-Z]+?:)\"");
    private static final Pattern PARTITION_PATTERN = Pattern.compile("(Disk\\s#\\d+,\\sPartition\\s#\\d+)");
    private static final Pattern DRIVE_PATTERN = Pattern.compile("(\\\\\\\\\\\\\\\\\\.\\\\\\\\PHYSICALDRIVE\\d+)");
    private static final String DISK_SPLIT = "Name: \\\\\\\\.\\\\";
    private static final String DISK_PATH = "\\\\.\\";
    private static final String TWOLINE_SPLIT_PATTERN = "(\\R)";

    private Map<Volume, Disk> volumeToDiskDictionary = new HashMap<>();
    private final Map<String, Disk> currentDisks = new HashMap<>();

    /**
     * Get all currently connected {@link Disk disks}.
     *
     * @return The currently connected disks.
     */
    public Set<Disk> getCurrentDisks() {
        findAndMapVolumesToDisks();
        return new TreeSet<>(currentDisks.values());
    }

    /**
     * Get the default {@link Disk}.
     *
     * @return The default {@link Disk}.
     */
    public Device getDefaultDisk() {
        return getCurrentDisks().iterator().next();
    }

    /**
     * Get a {@link Disk} by name
     *
     * @param deviceName
     *            The name to look for.
     * @return The disk, if found, otherwise null.
     */
    public Disk getDeviceByName(String deviceName) {
        if (currentDisks.isEmpty()) {
            scanForDisks();
        }
        return currentDisks.get(deviceName);
    }

    private long getDiskSize(Disk disk) {
        long result = -1l;
        Kernel32 kernel32 = Kernel32.INSTANCE;
        HANDLE diskHandle = kernel32.CreateFile(disk.path, WinNT.GENERIC_READ, WinNT.FILE_SHARE_READ, null,
                WinNT.OPEN_EXISTING, WinNT.FILE_ATTRIBUTE_NORMAL, null);
        if (WinBase.INVALID_HANDLE_VALUE.equals(diskHandle)) {
            return result;
        }
        try {
            Memory output = new Memory(Native.getNativeSize(LARGE_INTEGER.class));
            IntByReference lpBytes = new IntByReference();
            boolean success = kernel32.DeviceIoControl(diskHandle,
                    WinioctlUtil.CTL_CODE(Winioctl.FILE_DEVICE_DISK, 0x17, Winioctl.METHOD_BUFFERED,
                            Winioctl.FILE_READ_ACCESS),
                    null, 0, output, Native.getNativeSize(LARGE_INTEGER.class), lpBytes, null);
            // TODO: Check success?
            result = output.getLong(0);
        }
        finally {
            Kernel32Util.closeHandle(diskHandle);
        }
        return result;
    }

    /**
     * Scan for a list of disks connected to the system.
     *
     * @return a list of populated disk objects.
     */
    public List<Disk> scanForDisks() {
        return scanForDisks(true);
    }

    /**
     * Scan for a list of disks connected to the system.
     *
     * @param update
     *            forcibly update if disk information has previously been
     *            retrieved.
     * @return a list of populated disk objects.
     */
    public List<Disk> scanForDisks(boolean update) {
        List<Disk> disks = new ArrayList<>();
        if (update || currentDisks.isEmpty()) {
            String queryResult;
            queryResult = getWin32DiskDrives();
            for (String result : queryResult.split(DISK_SPLIT)) {
                try {
                    if (result.length() > 0) {
                        Disk disk = new Disk(DISK_PATH + result);
                        tryUpdateDisk(disk);
                        disks.add(disk);
                        currentDisks.put(disk.getName(), disk);
                    }
                }
                catch (Exception e) {
                    Logging.log(e);
                }
            }
        }
        else {
            disks.addAll(currentDisks.values());
        }
        return disks;
    }

    protected String getWin32DiskDrives() {
        String queryResult;
        queryResult = WMI4Java.get().VBSEngine()
                .properties(Arrays.asList("Name", "SystemName", "SerialNumber", "Model", "Size", "TotalHeads",
                        "TotalCylinders", "TotalTracks", "TotalSectors", "TracksPerCylinder", "SectorsPerTrack",
                        "BytesPerSector", "PNPDeviceID", "MediaType"))
                .getRawWMIObjectOutput(WMIClass.WIN32_DISKDRIVE);
        return queryResult;
    }

    protected void tryUpdateDisk(Disk disk) {
        tryUpdateSize(disk);
        tryUpdateVIDPID(disk);
        tryUpdateVendor(disk);
    }

    private void tryUpdateSize(Disk disk) {
        long size = getDiskSize(disk);
        if (size > 0) {
            disk.setSize(size); // A more accurate disk size.
        }
    }

    private void tryUpdateVIDPID(Disk disk) {
        try {
            final String USB_DEVICES_KEY = "SYSTEM\\CurrentControlSet\\Enum\\USB";
            String[] deviceKeys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, USB_DEVICES_KEY);
            for (String deviceKey : deviceKeys) {
                if (deviceKey.startsWith("VID")) {
                    if (Arrays.asList(
                            Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, USB_DEVICES_KEY + "\\" + deviceKey))
                            .contains(disk.physicalSerial)) {
                        disk.VID = deviceKey.substring(4, 8);
                        disk.PID = deviceKey.substring(13, 17);
                    }
                }
            }
        }
        catch (Exception e) {
            Logging.log("Unable to retrieve VID/PID for disk " + disk.model, LogMessageType.INFO);
            Logging.log(e);
        }
    }

    private void tryUpdateVendor(Disk disk) {
        try {
            final String USB_DEVICES_KEY = "SYSTEM\\CurrentControlSet\\Enum\\USBSTOR";
            String[] deviceKeys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, USB_DEVICES_KEY);
            for (String deviceKey : deviceKeys) {
                if (deviceKey.startsWith("Disk")) {
                    if (Arrays
                            .asList(Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE,
                                    USB_DEVICES_KEY + "\\" + deviceKey))
                            .stream().anyMatch((x) -> x.contains(disk.physicalSerial))) {
                        int startIndex = deviceKey.indexOf("_");
                        if (startIndex != -1) {
                            disk.vendorName = deviceKey.substring(startIndex + 1, deviceKey.indexOf("&", startIndex));
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            Logging.log("Unable to retrieve vendor name for disk " + disk.model, LogMessageType.DEBUG);
            Logging.log(e);
        }
    }

    /**
     * Scan for a newly inserted disk connected to the system.
     *
     * @return null if no new disks or found, otherwise it will return one of
     *         the newly connected disks.
     */
    public Disk scanForInsertedDisk() {
        String queryResult;
        Disk newDisk = null;
        Set<String> priorDisks = new TreeSet<>(currentDisks.keySet());
        queryResult = getWin32DiskDrives();
        Disk disk = null;
        for (String result : queryResult.split(DISK_SPLIT)) {
            try {
                if (result.length() > 0) {
                    disk = new Disk(DISK_PATH + result);
                    tryUpdateDisk(disk);
                    if (!currentDisks.containsKey(disk.getName())) {
                        map(scanForVolumes(), Arrays.asList(disk));
                        currentDisks.put(disk.getName(), disk);
                        newDisk = disk;
                    }
                    else {
                        if (!currentDisks.get(disk.getName()).equals(disk)) {
                            map(scanForVolumes(), Arrays.asList(disk));
                            currentDisks.put(disk.getName(), disk);
                            newDisk = disk;
                        }
                        priorDisks.remove(disk.getName());
                    }
                }

            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        for (String removedDisk : priorDisks) {
            currentDisks.remove(removedDisk);
        }
        return newDisk;
    }

    /**
     * Scan for a list of volumes connected to the system.
     *
     * @return list of populated volume objects.
     */
    private List<Volume> scanForVolumes() {
        List<Volume> volumes = new ArrayList<>();
        String queryResult = WMI4Java.get().VBSEngine().properties(Arrays.asList("DriveLetter", "Name", "FileSystem",
                "Label", "FreeSpace", "Capacity", "DriveType", "SerialNumber")).getRawWMIObjectOutput("Win32_Volume");

        for (String result : queryResult.split("DriveLetter")) {
            if (result.length() > 0) {
                Volume volume = new Volume(result);
                volumes.add(volume);
            }

        }
        return volumes;
    }

    /**
     * Finds and maps all connected volumes and all connected disks.
     *
     * @return
     */
    private Map<Volume, Disk> findAndMapVolumesToDisks() {
        List<Volume> volumes = scanForVolumes();
        List<Disk> disks = scanForDisks();
        volumeToDiskDictionary = map(volumes, disks);
        return volumeToDiskDictionary;
    }

    /**
     * Maps the given volumes to the appropriate disk.
     *
     * @param volumes
     *            The volumes to map.
     * @param drives
     *            The disks to map the volumes to.
     * @return The mapping of volumes to disks.
     */
    private static Map<Volume, Disk> map(List<Volume> volumes, List<Disk> drives) {
        Map<Volume, Disk> volumeToDrive = new HashMap<>(); // TODO, volumes hold their own Drive reference
        String queryResult;
        List<String> partitionDrivePairs;
        Map<String, Disk> partitionToDrive = mapPartitions(drives);
        queryResult = WMI4Java.get().VBSEngine().properties(Arrays.asList("Antecedent", "Dependent"))
                .getRawWMIObjectOutput(WMIClass.WIN32_LOGICALDISKTOPARTITION);
        partitionDrivePairs = joinDriveVolumeString(queryResult);
        for (String path : partitionDrivePairs) {

            String volumeName = matchPattern(VOLUME_PATTERN, path);
            String partitionName = matchPattern(PARTITION_PATTERN, path);
            Volume volume = null;
            for (Volume v : volumes) {
                if (v.path.startsWith(volumeName)) {
                    volume = v;
                    break;
                }
            }

            if (volume != null && volume.underlyingDisk != null) {
                volume.underlyingDisk = partitionToDrive.get(partitionName);
                volume.underlyingDisk.addVolume(volume);
                volumeToDrive.put(volume, volume.underlyingDisk);
            }
        }

        return volumeToDrive;
    }

    protected static Map<String, Disk> mapPartitions(List<Disk> drives) {
        Map<String, Disk> partitionToDrive = new HashMap<>();
        String queryResult = WMI4Java.get().VBSEngine().properties(Arrays.asList("Antecedent", "Dependent"))
                .getRawWMIObjectOutput(WMIClass.WIN32_DISKDRIVETODISKPARTITION);
        List<String> partitionDrivePairs = joinDriveVolumeString(queryResult);
        for (String path : partitionDrivePairs) {
            String driveName = matchPattern(DRIVE_PATTERN, path).replaceAll("\\\\\\\\", "\\\\");
            String partitionName = matchPattern(PARTITION_PATTERN, path);

            Disk drive = null;
            for (Disk d : drives) {
                if (d.getName().equals(driveName)) {
                    drive = d;
                    break;
                }
            }

            if (drive != null) {
                partitionToDrive.put(partitionName, drive);
            }
        }
        return partitionToDrive;
    }

    private static String matchPattern(Pattern pattern, String path) {
        Matcher matcher = pattern.matcher(path);
        String matchGroup = "";
        if (matcher.find()) {
            matchGroup = matcher.group(1);
        }
        return matchGroup;
    }

    private static List<String> joinDriveVolumeString(String queryResult) {
        String[] volumeInfoSingle = queryResult.split(TWOLINE_SPLIT_PATTERN);
        List<String> partitionDrivePairs = new ArrayList<>();
        String temp = "";
        for (String volume : volumeInfoSingle) {
            if (temp.length() == 0) {
                temp = volume;
            }
            else {
                partitionDrivePairs.add(temp + volume);
                temp = "";
            }
        }
        return partitionDrivePairs;
    }

    /**
     * A main method for testing disk and volume scanning and mapping.
     *
     * @param argv
     *            No arguments are used.
     */
    public static void main(String[] argv) {
        try {
            DeviceManager dm = new DeviceManager();
            dm.findAndMapVolumesToDisks();
            for (Disk disk : dm.currentDisks.values()) {
                System.out.println(disk.details());
            }

            for (Volume vol : dm.volumeToDiskDictionary.keySet()) {
                System.out.println(vol.details());
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
