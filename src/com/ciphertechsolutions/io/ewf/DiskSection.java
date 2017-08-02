package com.ciphertechsolutions.io.ewf;

import com.ciphertechsolutions.io.device.Device;

/**
 * TODO: Do not use?
 * Disk section is the same as the {@link com.ciphertechsolutions.io.ewf.VolumeSection}. Only found in FTK Imager 2.3.
 *
 */
public class DiskSection extends VolumeSection {

    public DiskSection(long currentOffset, Device disk, byte[] guid) {
        super("disk", currentOffset, disk, guid);
    }

}
