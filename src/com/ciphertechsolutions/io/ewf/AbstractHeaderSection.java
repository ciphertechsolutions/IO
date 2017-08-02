package com.ciphertechsolutions.io.ewf;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;

abstract class AbstractHeaderSection extends Section {

	protected AbstractHeaderSection(long currentOffset, String typeString) {
		super(currentOffset, typeString);
	}

	protected final String TAB = "\t";
	protected final byte RETURN = 0x0D;
	protected final byte NEWLINE = 0x0A;
	protected byte[] convertedHeaderString = null;
    protected byte[] headerAsBytes = null;
	protected boolean isSet = false;

	protected abstract String generateDateTimeHeaderValue(LocalDateTime timestamp);

	protected abstract byte[] convertHeaderString();

	protected abstract byte[] getStringBytesByFormat(String str);

    protected abstract String getBytesAsString(byte[] bytes);


	/**
	 * Takes in Map containing user input, and sets those values in the header.
	 * @param identifierValues Map containing user input case num/evidence num/ description/examiner name/notes
	 */
	protected abstract void setHeaderSection(Map<String, String> identifierValues);

	/**
	 * Use this method for inputting user configured strings and setting the header values.
	 * @param uniqueDescription
	 * @param caseNumber
	 * @param evidenceNumber
	 * @param examinerName
	 * @param notes
	 */
	public void setHeaderSection(String uniqueDescription, String caseNumber, String evidenceNumber,
			String examinerName, String notes, String serialNumber) {
	    if (isSet) {
	        throw new IllegalStateException("Header info already set! This information cannot be set again.");
	    }
		Map<String, String> identifierValues = new HashMap<>();
		identifierValues.put("a", uniqueDescription);
		identifierValues.put("c", caseNumber);
		identifierValues.put("n", evidenceNumber);
		identifierValues.put("e", examinerName);
		identifierValues.put("t", notes);
        identifierValues.put("sn", serialNumber);
		//TODO: ALL: md (model # of media), sn (serial number of media) from jWMI disk info, not user input.
		//TODO: HEADER2: gu (GUID), ah (Acquire Hash), lo (logical offset), po (physical offset), tb (total bytes)
		setHeaderSection(identifierValues);
		isSet = true;
	}

	public byte[] getHeaderAsBytes() {
	    if (headerAsBytes == null) {
    	    if (convertedHeaderString == null) {
    	        convertedHeaderString = convertHeaderString();
    	    }
    	    nextOffset += convertedHeaderString.length;
    	    sectionSize += convertedHeaderString.length;
    	    headerAsBytes = convertedHeaderString;
    	    //TODO: if this is all we need, the extra member variable is redundant.
	    }
	    return headerAsBytes;
	}

	/**
	 * Takes in Map containing user input, and generates the resulting Header Section
	 */
	//public abstract HeaderSection generateHeaderSection(Map<String, String> identifierValues);


	public byte[] getEOLBytes() {
		return new byte[]{ RETURN, NEWLINE };
	}

    public String getEOLString() {
        return "\r\n";
    }

	public String tabifyStringArrayLine(String[] line) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for(String value : line) {
			if(i == (line.length -1)) {
				sb.append(value); // last value doesn't need to be tabified, will instead have EOL
			} else {
				sb.append(value + TAB);
			}
			i++;
		}

		return sb.toString();
	}
	/**
	 * Returns header value for current date time.
	 */
	protected String generateCurrentDateTimeString() {
		return generateDateTimeHeaderValue(LocalDateTime.now());
	}

	protected static byte[] deflate(byte[] toCompress) {
        Deflater deflater = new Deflater();
        deflater.setInput(toCompress);
        deflater.finish();
        byte[] output = new byte[toCompress.length];
        int compressedSize = deflater.deflate(output);
        //TODO: Determine if we need to append the adler-32 hash.
        return Arrays.copyOf(output, compressedSize);
	}

	/**
	 * Gets the correct index of the identifier and sets the key and value at the index for the given lines.
	 * @param identifiers List of identifier keys
	 * @param identifier Identifier to be added
	 * @param identifierLine to set identifier at index
	 * @param valueLine to set value at index
	 * @param value to be set
	 */
	private static void setHeaderValueByIdentifier(List<String> identifiers, String identifier, String[] valueLine, String value) {
		int index = identifiers.indexOf(identifier);
		if (index >= 0) { // is a correct identifier
			valueLine[index] = value;
		}
	}


	/**
	 * Takes a map of identifiers and values and assigns them to proper lines to include in header.
	 * @param identifiersForLine List of identifier keys
	 * @param identifierLine Line to set identifiers of
	 * @param valueLine Line to set values of
	 * @param identifierValues Identifier/Value map
	 */
	protected void setLineValues(List<String> identifiersForLine, String[] valueLine, Map<String, String> identifierValues) {
		for (String key : identifierValues.keySet()) {
			setHeaderValueByIdentifier(identifiersForLine, key, valueLine, identifierValues.get(key));
		}
	}

	/**
	 * IdentifierValues contains larger set for Header2, but both Headers share core identifiers.
	 * @param identifierValues map of user/system configured identifier/value
	 */
	protected void setupLinesThreeFour(Map<String, String> identifierValues) {
		String acquireDate = generateCurrentDateTimeString();
		identifierValues.put("av", "ION");
		identifierValues.put("ov", System.getProperty("os.name"));
		identifierValues.put("m", acquireDate);
		identifierValues.put("u", acquireDate);
		identifierValues.put("p", "0");

	}




}
