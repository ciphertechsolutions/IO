package com.ciphertechsolutions.io.ewf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Adler32;

import com.ciphertechsolutions.io.device.Device;

/**
 * A class to represent the Volume section in the Encase6 format.
 */
public class VolumeSection extends Section {

    static final int ADDITIONAL_SECTION_SIZE = 1052;

	//TODO: Enum?
    //0x00 => removable disk
    // 0x01 => fixed disk
    // 0x03 => optical disk
    // 0x0e => Logical evidence file (LEV or L01)
    // 0x10 => memory (RAM/process)
	MediaType mediaType;

    byte[] unknown = { 0x00, 0x00, 0x00 };

    int chunkCount;

    // TODO: Have the chunker set these, or be set from these.
    int sectorsPerChunk;

    int bytesPerSector;

    //TODO: Confirm long vs int
    long sectorCount;

    int cylinders;
    // Per cylinder, I think
    int heads;
    // Per head, I think
    int sectors;

    int mediaFlag;

    int IS_IMAGE_FILE_FLAG = 0x01;
    int IS_PHYSICAL_FLAG = 0x02;
    int FASTBLOC_USED_FLAG = 0x04;
    int TABLEAU_BLOCK_USED_FLAG = 0x08;


    // ???
    int palmVolumeStartSector;

    byte[] unknown2 = {0x00, 0x00, 0x00, 0x00};

    int smartLogStartSector;

    // I don't think this corresponds exactly to zlib levels, unsure though.
    int compressionLevel;


    //TODO: Tie this to our error handling in drive reader?
    int errorGranularity;

    byte[] unknown3 = {0x00, 0x00, 0x00, 0x00};

    byte[] fileSetGUID = new byte[16];

    // I am not typing this one out.
    byte[] unknown4 = new byte[963];

    //???
    byte[] signature = new byte[5];

    int secondaryAdler32 = 0;

    protected VolumeSection(long currentOffset, Device disk, byte[] guid) {
        this("volume", currentOffset, disk, guid);
    }

    protected VolumeSection(String typeString, long currentOffset, Device disk, byte[] guid) {
        super(currentOffset, typeString);
        this.sectionSize += ADDITIONAL_SECTION_SIZE;
        this.nextOffset += ADDITIONAL_SECTION_SIZE;

        // Maybe leave these as 0?
        this.cylinders = 0;
        this.heads = 0;
        this.sectors = 0;

        this.fileSetGUID = guid;
        // TODO: Set these more appropriately.
        this.mediaType = MediaType.FIXED_STORAGE_MEDIA; // Should match actual media type.
        this.compressionLevel = 0x01; //Make configurable maybe?
        this.errorGranularity = 1; // Should be tied to our reading granularity.
        this.mediaFlag = IS_IMAGE_FILE_FLAG | IS_PHYSICAL_FLAG; //Should match actual.
        this.sectorsPerChunk = 1024; //Must match compressed chunker
        this.smartLogStartSector = 0; // Measured from the end of media.
        this.palmVolumeStartSector = 0; //I have no idea what this is.
        this.bytesPerSector = 512; // Compressed chunker needs this too. Almost always 512.

        // These will get updated manually later.
        this.chunkCount = 0;
        this.sectorCount = 0;
    }

    void correctSizeInformation(int chunkCount, long sectorCount) {
        this.chunkCount = chunkCount;
        this.sectorCount = sectorCount;
        secondaryAdler32 = 0;
    }

    int getSecondaryAdler32(){
        if (secondaryAdler32 == 0) {
            Adler32 adlerCalc = new Adler32();
            ByteBuffer bytes = getPartialBytes();
            bytes.flip();
            adlerCalc.update(bytes);
            secondaryAdler32 = (int) adlerCalc.getValue();
        }
        return secondaryAdler32;
    }

    byte[] getAdditionalBytes() {
        ByteBuffer bytes = getPartialBytes();
        bytes.order(ByteOrder.LITTLE_ENDIAN);
        bytes.putInt(getSecondaryAdler32());
        return bytes.array();

    }

	private ByteBuffer getPartialBytes() {
		ByteBuffer bytes = ByteBuffer.allocate(ADDITIONAL_SECTION_SIZE);
        bytes.order(ByteOrder.LITTLE_ENDIAN);
        bytes.put(mediaType.value);
        bytes.put(unknown);
        bytes.putInt(chunkCount);
        bytes.putInt(sectorsPerChunk);
        bytes.putInt(bytesPerSector);
        bytes.putLong(sectorCount);
        bytes.putInt(cylinders);
        bytes.putInt(heads);
        bytes.putInt(sectors);
        bytes.putInt(mediaFlag);
        bytes.putInt(palmVolumeStartSector);
        bytes.put(unknown2);
        bytes.putInt(smartLogStartSector);
        bytes.putInt(compressionLevel);
        bytes.putInt(errorGranularity);
        bytes.put(unknown3);
        bytes.put(fileSetGUID);
        bytes.put(unknown4);
        bytes.put(signature);
		return bytes;
	}

    enum MediaType {
    	REMOVABLE_MEDIA(0x00), FIXED_STORAGE_MEDIA(0x01), OPTICAL_DISK(0x03), LOGICAL_EVIDENCE_FILE(0x0E), PHYSICAL_MEMORY(0x10);

    	final byte value;

    	MediaType(int value) {
    		this.value = (byte) value;
    	}
    }

}
