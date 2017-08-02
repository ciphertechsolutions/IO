package com.ciphertechsolutions.io.ewf;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Found in EnCase6. Resides at the start of the first segment file. Nowhere else.
 *
 * Encase 6 contains the same header2 section twice, directly after each other. Contains UTF16 format text compressed via zlib.
 *
 *In EnCase 6 the header2 information consist of 18 lines
 * The remainder of the string contains the following information:
 * Line number 	Meaning 			Value
 * 1 The number of categories provided 3
 * 2 The name/type of the category provided main
 * 3 Identifier for the values in the 4th line
 * 4 The data for the different identifiers in the 3rd line
 * 5 (an empty line)
 * 6 The name/type of the category provided srce
 * 7
 * 8 Identifier for the values in the category
 * 9
 * 10
 * 11 (an empty line)
 * 12 The name/type of the category provided sub
 * 13
 * 14 Identifier for the values in the category
 * 15
 * 16
 * 17 (an empty line)
 * The end of line character(s) is a newline (0x10)
 *
 * The 3rd and the 4th line consist of the following tab (0x09) separated values.
 *  Identifier number Character in 3rd line Value in 4th line
 *  1 a Unique description
 *  2 c Case number
 *  3 n Evidence number
 *  4 e Examiner name
 *  5 t Notes
 *  6 md The model of the media, i.e. hard disk model
 *  7 sn The serial number of media
 *  8 av Version The EnCase version used to acquire the media EnCase limits this value to 12 characters
 *  9 ov Platform The platform/operating system used to acquire the media
 *  10 m Acquired date
 *  11 u System date
 *  12 p pwhash
 *  13 dc Unknown
 * Case number, evidence number, unique description, examiner name, notes, version and platform are freeform
 * “Acquired date”, and “System date” are in the form of: “1142163845”, which is a Unix epoch time stamp and represents the date: March 12 2006, 11:44:05.
 * DIFFERENT THAN {@link com.ciphertechsolutions.io.ewf.HeaderSection}
 * pwhash is 0 if no pw is set
 *
 * The 8th line consist of the following tab (0x09) separated values.
 * Identifier number Character in 8th line Meaning
 * 1 p
 * 2 n
 * 3 id Identifier, unique name
 * 4 ev Evidence number
 * 5 tb Total bytes
 * 6 lo Logical offset
 * 7 po Physical offset
 * 8 ah Acquire hash
 * 9 gu GUID
 * 10 aq Acquire date
 * sub probably are subjects within EnCase
 *
 * The 14th line consist of the following tab (0x09) separated values.
 * Identifier number Character in 14th line Meaning
 * 1 p
 * 2 n
 * 3 id Identifier, unique name
 * 4 nu Number
 * 5 co Comment
 * 6 gu GUID

 */
public class Header2Section extends AbstractHeaderSection {
	private static final String[] IDENTIFIER3_CHARS = new String[] { "c", "n", "a", "e", "t", "md", "sn", "av", "ov", "m", "u", "p", "dc"};
	private static final List<String> IDENTIFIER3_CHARS_LIST = Arrays.asList(IDENTIFIER3_CHARS);

	private static final String[] IDENTIFIER8_CHARS = new String[] { "p", "n", "id", "ev", "tb", "lo", "po", "ah", "gu", "aq"};
	private static final List<String> IDENTIFIER8_CHARS_LIST = Arrays.asList(IDENTIFIER8_CHARS);

	private static final String[] IDENTIFIER14_CHARS = new String[] { "p", "n", "id", "nu", "co", "gu"};
	private static final List<String> IDENTIFIER14_CHARS_LIST = Arrays.asList(IDENTIFIER14_CHARS);

    public Header2Section(long currentOffset) {
        super(currentOffset, "header2");
    }

	byte[] byteOrder = new byte[] { (byte) 0xff, (byte) 0xfe }; // little-endian, reverse for big-endian
	byte[] lineOne = new byte[] { 0x03 };
	String lineTwo = "main";
	String[] lineThree = IDENTIFIER3_CHARS; // identifiers
	String[] lineFour = new String[13]; // identifier values
	byte[] lineFive = new byte[] { 0x00, 0x00 };
	String lineSix = "srce";
	byte[] lineSeven = new byte[] { 0x00, 0x01 };
	String[] lineEight = IDENTIFIER8_CHARS;
	byte[] lineNine = new byte[] { 0x00, 0x00 };
	String[] lineTen = new String[10];
	byte[] lineEleven = new byte[] { 0x00 };
	String lineTwelve = "sub";
	byte[] lineThirteen = new byte[] { 0x00, 0x01 };
	String[] lineFourteen = IDENTIFIER14_CHARS;
	byte[] lineFifteen = new byte[] { 0x00, 0x00 };
	String[] lineSixteen = new String[6];
	byte[] lineSeventeen = new byte[] { 0x00 };


	/**
	 * IdentifierValues contains larger set for Header2, but both Headers share core identifiers.
	 * @param identifierValues map of user/system configured identifier/value
	 */
	private void setupRemainingLineIdentifiers(Map<String, String> identifierValues) {
		String acquireDate = generateCurrentDateTimeString();
		identifierValues.put("aq", acquireDate);
		identifierValues.put("id", identifierValues.get("a")); // a -> id for later lines
		identifierValues.put("co", identifierValues.get("t")); // t -> co for later lines
		identifierValues.put("nu", identifierValues.get("c")); // c -> nu for later lines
		identifierValues.put("ev", identifierValues.get("n")); // n- > ev for later lines


	}

	private void setLineFourValues(Map<String, String> identifierValues) {
		setLineValues(IDENTIFIER3_CHARS_LIST, lineFour, identifierValues);
	}

	private void setLineTenValues(Map<String, String> identifierValues) {
		// remove p, n, make copy

		setLineValues(IDENTIFIER8_CHARS_LIST, lineTen, identifierValues);
	}

	private void setLineSixteenValues(Map<String, String> identifierValues) {

		setLineValues(IDENTIFIER14_CHARS_LIST, lineSixteen, identifierValues);
	}


	/**
	 * “Acquired date”, and “System date” are in the form of: “1142163845”, which is a Unix epoch time stamp and represents the date: March 12 2006, 11:44:05.
	 * DIFFERENT THAN {@link com.ciphertechsolutions.io.ewf.HeaderSection}
	 */
	@Override
    protected String generateDateTimeHeaderValue(LocalDateTime timestamp) {
		return String.valueOf(timestamp.toEpochSecond(ZoneOffset.UTC));

	}

	/**
	 * Takes in Map containing user input, and generates the resulting Header Section
	 * @param identifierValues Map containing user input case num/evidence num/ description/examiner name/notes
	 */
	@Override
	protected void setHeaderSection(Map<String, String> identifierValues) {
		setupLinesThreeFour(identifierValues);
		setLineFourValues(identifierValues);
		setupRemainingLineIdentifiers(identifierValues);
		setLineTenValues(identifierValues);
		setLineSixteenValues(identifierValues);
	}

	/**
	 * Use after {@link com.ciphertechsolutions.io.ewf.Header2Section#generateHeaderSection()}
	 */
	@Override
	protected byte[] convertHeaderString() {
		// All lines, separated by new line
		// All arrays within line separated by tab
		StringBuilder sb = new StringBuilder();

		sb.append(getBytesAsString(byteOrder));
		sb.append(getEOLString());
		sb.append(getBytesAsString(lineOne));
		sb.append(getEOLString());
		sb.append(lineTwo);
        sb.append(getEOLString());
        sb.append(tabifyStringArrayLine(lineThree));
        sb.append(getEOLString());
        sb.append(tabifyStringArrayLine(lineFour));
        sb.append(getEOLString());
        sb.append(getBytesAsString(lineFive));
        sb.append(getEOLString());
        sb.append(lineSix);
        sb.append(getEOLString());
        sb.append(getBytesAsString(lineSeven));
        sb.append(getEOLString());
        sb.append(tabifyStringArrayLine(lineEight));
        sb.append(getEOLString());
        sb.append(getBytesAsString(lineNine));
        sb.append(getEOLString());
        sb.append(tabifyStringArrayLine(lineTen));
        sb.append(getEOLString());
        sb.append(getBytesAsString(lineEleven));
        sb.append(getEOLString());
        sb.append(lineTwelve);
        sb.append(getEOLString());
        sb.append(getBytesAsString(lineThirteen));
        sb.append(getEOLString());
        sb.append(tabifyStringArrayLine(lineFourteen));
        sb.append(getEOLString());
        sb.append(getBytesAsString(lineFifteen));
        sb.append(getEOLString());
        sb.append(tabifyStringArrayLine(lineSixteen));
        sb.append(getEOLString());
        sb.append(getBytesAsString(lineSeventeen));
        sb.append(getEOLString());

		return deflate(getStringBytesByFormat(sb.toString()));
	}

	/**
	 * Returns UTF-16 text in byte[] form.
	 */
	@Override
	protected byte[] getStringBytesByFormat(String str) {
		return str.getBytes(StandardCharsets.UTF_16LE);
	}

	@Override
	protected String getBytesAsString(byte[] bytes) {
    	return new String(bytes, StandardCharsets.UTF_16LE);
    }



}
