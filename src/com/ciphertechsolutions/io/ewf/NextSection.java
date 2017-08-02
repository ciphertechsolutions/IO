package com.ciphertechsolutions.io.ewf;

/**
 * Next section has a section data type field of 'next'.
 * The next section is the last section within a segment other than the last segment file. The offset to the next section
 * in the start of the nextion section point to itself (the start of the next section). It should be the last section
 * in a segment file, other than the last segment file.
 * Resides after {@link com.ciphertechsolutions.io.ewf.DataSection} in single segment, or after {@link com.ciphertechsolutions.io.ewf.Table2Section} in multiple segment files.
 * Size in section start is 0 instead of 76 for EnCase.
 */
public class NextSection extends Section {

    public NextSection(long currentOffset) {
        super(currentOffset, "next");
        this.nextOffset = currentOffset; // This seems to just point to itself.
    }

}
