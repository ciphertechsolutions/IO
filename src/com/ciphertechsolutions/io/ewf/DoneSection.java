package com.ciphertechsolutions.io.ewf;

/**
 * Last section in the last segment file.
 * The offset of the next section in the section start of the done section point to itself.
 * Resides after {@link com.ciphertechsolutions.io.ewf.DataSection} in single segment, or after {@link com.ciphertechsolutions.io.ewf.Table2Section} in multiple segment files.
 * Size in section start is 0 instead of 76 for EnCase.
 */
public class DoneSection extends Section {

	/**
	 * Create a new "Done" Section with the given starting offset.
	 * @param currentOffset The offset, in bytes, from the start of the file that this section resides at.
	 */
    public DoneSection(long currentOffset) {
        super(currentOffset, "done");
        this.nextOffset = currentOffset; // This seems to just point to itself.
    }

}
