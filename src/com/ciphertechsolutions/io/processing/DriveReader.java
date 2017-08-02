package com.ciphertechsolutions.io.processing;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;

import com.ciphertechsolutions.io.device.DeviceManager;
import com.ciphertechsolutions.io.device.Disk;
import com.ciphertechsolutions.io.logging.LogMessageType;
import com.ciphertechsolutions.io.logging.Logging;

/**
 * An implementation of {@link IMediaReader} for reading entire drives. Capable of reading physical drives only.
 */
public class DriveReader implements IMediaReader {

    private static final int SECTORS_PER_CHUNK = 256;
    private static int SECTOR_SIZE = 512; // TODO: does this need to be variable based on the device?
    private static byte[] NULL_SECTOR = new byte[SECTOR_SIZE];
    private static final int READ_CHUNK_SIZE = SECTOR_SIZE * SECTORS_PER_CHUNK;
    private int currentReadSize = READ_CHUNK_SIZE;
    private final FileChannel channel;
    private ByteBuffer buffer;
    private long bytesRead;
    private final long lastSector;
    private final long lastSectorBytes; // For faster comparisons
    private long resumeNormalRead = 0l;
    private static ArrayList<Long> badSectorList;
    private static final int MAX_PRINTED_WARNINGS = 100;
    private boolean hasWarned = false;

    /**
     * Constructs a DriveReader to read the drive specified by the given path. The path should be to a physical
     * drive (e.g. \\\\.\\PhysicalDrive3).
     * @param toRead The path to the drive to read.
     * @throws IOException An irrecoverable exception occurred during IO.
     */
    @SuppressWarnings("resource")
    public DriveReader(Path toRead) throws IOException {
        channel = new RandomAccessFile(toRead.toFile(), "r").getChannel();
        channel.position(0);
        buffer = ByteBuffer.allocate(READ_CHUNK_SIZE);
        DeviceManager dManager = new DeviceManager();
        String diskName = toRead.toString().substring(0, toRead.toString().length() - 1);
        Disk disk = dManager.getDeviceByName(diskName);
        lastSector = (disk.getSize() / 512);
        lastSectorBytes = disk.getSize();
        badSectorList = new ArrayList<>();
    }

    @Override
    public int read() throws IOException {
        buffer.clear();
        int read = 0;
        try {
            if (currentReadSize < READ_CHUNK_SIZE && bytesRead >= resumeNormalRead) { // once past bad sector(s), ramp back up
                currentReadSize = READ_CHUNK_SIZE; // don't slowly ramp up.
                changeReadSize();
            }
            //Never read past last expected sector, TODO check this doesn't impact speeds
            if (channel.position() >= lastSectorBytes) {
                return -1;
            }
            read = channel.read(buffer);
        }
        catch (IOException e) {
            read = buffer.position();
            if (read == 0) {
                long currentPosition = channel.position();
                long currentSector = currentPosition / SECTOR_SIZE;
                // Ensure never past last sector
                if (currentSector >= lastSector) {
                    return -1;
                }
                else if (currentReadSize > SECTOR_SIZE) {
                    resumeNormalRead = currentPosition + READ_CHUNK_SIZE;
                    currentReadSize = SECTOR_SIZE;
                    changeReadSize();
                    return 0;
                }
                else if (currentReadSize == SECTOR_SIZE) {
                    if (currentSector < lastSector && e.getMessage().contains("The drive cannot find the sector requested")) {
                        logIfNeeded("The drive cannot find the sector #" + currentSector + ". Nulling chunk and advancing to next sector.");
                        Logging.log("Current Read size: " + currentReadSize, LogMessageType.DEBUG);
                        read = markBadSectorAndAdvance(currentSector);
                    }
                    else if (e.getMessage().contains("Data error")) {
                        logIfNeeded("Bad sector at sector #" + currentSector + ". Nulling chunk and advancing to next sector.");
                        read = markBadSectorAndAdvance(currentSector);
                    }
                    else if (currentSector < lastSector) {
                        logIfNeeded("Unknown error at sector #" + currentSector + ". Nulling chunk and advancing to next sector.");
                        read = markBadSectorAndAdvance(currentSector);
                    }
                    else {
                        close();
                        return -1;
                    }
                }
                else {
                    // We should never get here.
                    close();
                    return -1;
                }
            }
        }
        bytesRead += read;
        buffer.flip();
        return read;
    }

    private void logIfNeeded(String warning) {
        if (badSectorList.size() > MAX_PRINTED_WARNINGS) {
            if (!hasWarned) {
                hasWarned = true;
                Logging.log("A high number of bad sectors has been detected, individual sector errors will no longer be printed.", LogMessageType.WARNING);
            }
        }
        else {
            Logging.log(warning, LogMessageType.WARNING);
        }

    }

    protected int markBadSectorAndAdvance(long currentSector) throws IOException {
        badSectorList.add(currentSector);
        channel.position(channel.position() + SECTOR_SIZE);
        buffer.put(NULL_SECTOR);
        return SECTOR_SIZE;
    }

    @Override
    public void close() {
        try {
            channel.close();
        }
        catch (IOException e) {
            // Well, we tried
            Logging.log(e);
        }
    }

    private void changeReadSize() {
        buffer = ByteBuffer.allocate(currentReadSize);
        buffer.limit(0);
    }

    @Override
    public ByteBuffer getBuffer() {
        return this.buffer;
    }

    @Override
    public byte[] getBytes() {
        int length = buffer.remaining();
        byte[] dest = new byte[length];
        System.arraycopy(buffer.array(), 0, dest, 0, length);
        return dest;
    }

    @Override
    public byte[] getUnsafeBytes() {
        return buffer.array();
    }

    @Override
    public long getBytesRead() {
        return bytesRead;
    }

    // TODO: This is bad code and should not be static.
    /**
     * Get a list of bad sectors read. This method should be replaced with one that is not static.
     * @return The bad sectors.
     */
    public static ArrayList<Long> getBadSectorsList() {
        return badSectorList;
    }

    @Override
    public int getReadSize() {
        return READ_CHUNK_SIZE;
    }
}
