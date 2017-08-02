package com.ciphertechsolutions.io.ewf;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Header section is defined only once. It resides after the {@link com.ciphertechsolutions.io.ewf.Header2Section} of the first segment file. It is not found in other segment files.
 * The header data itself is ASCII text compressed using zlib.
 *
 * Offset 76 (0x4c), Size: variable, contains information about the acquire media, below.
 *
 * For Encase 6 the information consists of at least the four lines:
 * Line number  Meaning 									Value
 * 1 			The number of sections provided 			1
 * 2 			Probably the type of information provided 	main
 * 3 			Identifiers for the values in the 4th line
 * 4 			The data for the different identifiers in the 3rd line
 * 5 			Empty										Empty
 *
 * The 3rd and the 4th line consist of the following tab (0x09) separated values.
 * Identifier number|Character in 3rd line|Value in 4th line
 * 1 				c 						Case number
 * 2 				n 						Evidence number
 * 3 				a 						Unique description
 * 4 				e 						Examiner name
 * 5 				t 						Notes
 * 6 				av 						Version The EnCase version used to acquire the media EnCase limits this value to 12 characters
 * 7 				ov 						Platform The platform/operating system used to acquire the media
 * 8 				m 						Acquired date
 * 9 				u 						System date
 * 10 				p 						pwhas
 * Case number, evidence number, unique description, examiner name, notes, version and platform are freeform
 * EOL char is return (0x13) followed by newline (0x10)
 * Acquired date and System date are 2002 3 4 10 19 59 which represents March 4, 2002 10:19:59 DIFFERENT THAN {@link com.ciphertechsolutions.io.ewf.Header2Section}
 * pwhash is 0
 *
 */
public class HeaderSection extends AbstractHeaderSection {
	private static final String[] IDENTIFIER3_CHARS = new String[] { "c", "n", "a", "e", "t", "sn", "av", "ov", "m", "u", "p"};
	private static final List<String> IDENTIFIER3_CHARS_LIST = Arrays.asList(IDENTIFIER3_CHARS);

	public HeaderSection(long currentOffset) {
	    super(currentOffset, "header");
	}

	String lineOne = "1";
	String lineTwo = "main";
	String[] lineThree = IDENTIFIER3_CHARS;
	String[] lineFour = new String[11];

	private void setLineFourValues(Map<String, String> identifierValues) {
		setLineValues(IDENTIFIER3_CHARS_LIST, lineFour, identifierValues);
	}

	/**
	 *  Acquired date and System date are 2002 3 4 10 19 59 which represents March 4, 2002 10:19:59 DIFFERENT THAN {@link com.ciphertechsolutions.io.ewf.Header2Section}
	 */
	@Override
    protected String generateDateTimeHeaderValue(LocalDateTime timestamp) {
		StringBuilder sb = new StringBuilder();
		sb.append(timestamp.getYear() + " ");
		sb.append(timestamp.getMonthValue() + " ");
		sb.append(timestamp.getDayOfMonth() + " ");
		sb.append(timestamp.getHour() + " ");
		sb.append(timestamp.getMinute() + " ");
		sb.append(timestamp.getSecond());

		return sb.toString();

	}

	/**
	 * Takes in Map containing user input, and set the Header Section
	 * @param identifierValues Map containing user input case num/evidence num/ description/examiner name/notes
	 */
	@Override
    protected void setHeaderSection(Map<String, String> identifierValues) {
		setupLinesThreeFour(identifierValues);
		setLineFourValues(identifierValues);
	}

	/**
	 * Use after {@link com.ciphertechsolutions.io.ewf.HeaderSection#generateHeaderSection()}
	 */
	@Override
	protected byte[] convertHeaderString() {
		// TODO: Use a straight byte array instead if efficiency is an issue.
		// All lines, separated by new line
		// All arrays within line separated by tab
		StringBuilder sb = new StringBuilder();
		sb.append(lineOne);
		sb.append(getEOLString());
		sb.append(lineTwo);
		sb.append(getEOLString());
		sb.append(tabifyStringArrayLine(lineThree));
		sb.append(getEOLString());
		sb.append(tabifyStringArrayLine(lineFour));
		sb.append(getEOLString());

		return deflate(getStringBytesByFormat(sb.toString()));
	}

	/**
	 * Returns ASCII text in byte[] form.
	 */
	@Override
	protected byte[] getStringBytesByFormat(String str) {
		return str.getBytes(StandardCharsets.US_ASCII);
	}

	@Override
	protected String getBytesAsString(byte[] bytes) {
    	return new String(bytes, StandardCharsets.US_ASCII);
    }


}
