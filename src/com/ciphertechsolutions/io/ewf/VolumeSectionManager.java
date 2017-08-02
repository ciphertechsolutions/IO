package com.ciphertechsolutions.io.ewf;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.ciphertechsolutions.io.device.Device;

public class VolumeSectionManager {
    private final Map<RandomAccessFile, List<VolumeSection>> volumeSections;
    private final byte[] EMPTY_VOLUME_SECTION = new byte[VolumeSection.ADDITIONAL_SECTION_SIZE + Section.SECTION_HEADER_SIZE];
    private final Device imagedDisk;
    private int chunks;
    private long sectors;
    private byte[] fileSetGUID = null;

    public VolumeSectionManager(Device imagedDisk) {
        this.volumeSections = new ConcurrentHashMap<>();
        this.imagedDisk = imagedDisk;
    }

    public void put(RandomAccessFile key, VolumeSection value) {
        if (volumeSections.containsKey(key)) {
            volumeSections.get(key).add(value);
        }
        else {
            List<VolumeSection> sections = Collections.synchronizedList(new ArrayList<>());
            sections.add(value);
            volumeSections.put(key, sections);
        }
    }

    public void writePreliminaryVolumeSection(RandomAccessFile file) throws IOException {
        put(file, new VolumeSection(file.getFilePointer(), imagedDisk, getFileSetGuid()));
        file.write(EMPTY_VOLUME_SECTION);
    }

    public void writePreliminaryDataSection(RandomAccessFile file) throws IOException {
        put(file, new DataSection(file.getFilePointer(), imagedDisk, getFileSetGuid()));
        file.write(EMPTY_VOLUME_SECTION);
    }

    public void writePreliminaryDiskSection(RandomAccessFile file) throws IOException {
        put(file, new DiskSection(file.getFilePointer(), imagedDisk, getFileSetGuid()));
        file.write(EMPTY_VOLUME_SECTION);
    }

    public void setVolumeSize(int chunks, long sectors) {
        this.chunks = chunks;
        this.sectors = sectors;
    }

    public void writeProperVolumeSections(boolean closeFiles) throws IOException{
        for (RandomAccessFile file : volumeSections.keySet()) {
            for (VolumeSection toWrite : volumeSections.get(file)) {
                toWrite.correctSizeInformation(chunks, sectors);
                file.seek(toWrite.getCurrentOffset());
                file.write(toWrite.getFullHeader());
                file.write(toWrite.getAdditionalBytes());
            }
            if (closeFiles) {
                file.close();
            }
        }
    }

    private byte[] getFileSetGuid() {
        if (fileSetGUID == null) {
            UUID acquiryID = UUID.randomUUID();
            ByteBuffer guidBuffer = ByteBuffer.allocate(16);
            guidBuffer.order(ByteOrder.LITTLE_ENDIAN);
            guidBuffer.putLong(acquiryID.getLeastSignificantBits());
            guidBuffer.putLong(acquiryID.getMostSignificantBits());
            fileSetGUID = guidBuffer.array();
        }
        return fileSetGUID;
    }

}
