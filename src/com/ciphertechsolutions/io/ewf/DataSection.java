package com.ciphertechsolutions.io.ewf;

import com.ciphertechsolutions.io.device.Device;

/**
 * If the data section has data it should contain the same information
 * as the volume section. For multiple segment files it is not in the first segment.
 * Resides after {@link com.ciphertechsolutions.io.ewf.Table2Section} in a single segment file,
 * or at the start of the segment files, other than the first segment file.
 *
 *
 */
public class DataSection extends VolumeSection {

    public DataSection(long currentOffset, Device disk, byte[] guid) {
        super("data", currentOffset, disk, guid);
    }
}
