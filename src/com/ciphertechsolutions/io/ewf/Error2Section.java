package com.ciphertechsolutions.io.ewf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.zip.Adler32;

/**
 * TODO: Optional? Since ION will specify errors as well.
 * Found in EnCase6 format.
 * It is only added to the last segment file when errors were encountered while reading the input.
 * It contains the sectors that have read errors. The sector where a read error
 * occurred are filled with zero's during EnCase acquiry.
 *
 */
public class Error2Section extends Section {

	public Error2Section(long currentOffset) {
		super(currentOffset, "error2");
		numberOfEntires = 0;
		error2SectorEntries = new ArrayList<>();
	}

	private int numberOfEntires;
	private static final byte[] padding = new byte[512];
	private Adler32 checksumPrevious; //Checksum of previous data

	private final ArrayList<Error2SectorEntry> error2SectorEntries;

	private Adler32 checksumSectors; //Checksum of remaining sectors data

	private static final int minimumError2SectionSize = padding.length + 12; // int + 2 alder;

	public void addEntry(int errorSector, int numSectors) {
		Error2SectorEntry errorEntry = new Error2SectorEntry(errorSector, numSectors);
		error2SectorEntries.add(errorEntry);
		numberOfEntires++;
	}

	public byte[] getFullBytes() {
		byte[] allEntries = getAllEntriesBytes();
	    ByteBuffer buffer = ByteBuffer.allocate(allEntries.length + minimumError2SectionSize);
	    buffer.order(ByteOrder.LITTLE_ENDIAN);
	    buffer.putInt(numberOfEntires);
	    buffer.put(padding);
	    buffer.putInt(getPreviousAdler32());
	    buffer.put(allEntries);
	    buffer.putInt(getSectorsAdler32());
	    return buffer.array();
	}

	private byte[] getAllEntriesBytes() {
		ByteBuffer buffer = ByteBuffer.allocate(Error2SectorEntry.entrySize * error2SectorEntries.size());
		for(Error2SectorEntry entry : error2SectorEntries) {
			buffer.put(entry.getFullBytes());
		}
		return buffer.array();
	}



    private int getPreviousAdler32() {
        Adler32 adlerCalc = new Adler32();
        adlerCalc.update(numberOfEntires);
        adlerCalc.update(padding);
        checksumPrevious = adlerCalc;
        return (int) adlerCalc.getValue();
    }

    private int getSectorsAdler32() {
        Adler32 adlerCalc = new Adler32();
        adlerCalc.update(getAllEntriesBytes());
        checksumSectors = adlerCalc;
        return (int) adlerCalc.getValue();
    }

	private class Error2SectorEntry {
		int firstErrorSector;
		int numberOfErrorSectors;
		static final int entrySize = 8;
		private Error2SectorEntry(int firstError, int numberofSectors) {
			firstErrorSector = firstError;
			numberOfErrorSectors = numberofSectors;
		}

		public byte[] getFullBytes() {
		    ByteBuffer buffer = ByteBuffer.allocate(entrySize);
		    buffer.order(ByteOrder.LITTLE_ENDIAN);
		    buffer.putInt(firstErrorSector);
		    buffer.putInt(numberOfErrorSectors);
		    return buffer.array();
		}
	}



}


