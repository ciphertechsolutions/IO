package com.ciphertechsolutions.io.applicationLogic.options;

/**
 * An enumeration of the compression levels ION offers. These are used internally by zlib.
 */
public enum CompressionTypesEnum {
    /**
     * No compression
     */
    NONE("none", 0),
    /**
     * Fastest possible - zlib compression level 1.
     */
    FAST("fast", 1),
    /**
     * Balanced between speed and size - zlib compression level 4.
     */
    BALANCED("balanced", 4),
    /**
     * Maximal compression. zlib compression level 9.
     */
    BEST("best", 9);


    private final String displayName;
    private final int compressionLevel;

    CompressionTypesEnum(String value, int compressionLevel) {
        displayName = value;
        this.compressionLevel = compressionLevel;
    }

    /**
     * Get a user-friendly name for the compression level.
     * @return A user-friendly name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the zlib compression level, as an int.
     * @return The zlib compression level.
     */
    public int getLevel() {
        return compressionLevel;
    }

    /**
     * Gets ION's default compression level.
     * @return The default compression level.
     */
    public static CompressionTypesEnum getDefaultCompressionType() {
        return FAST;
    }

    /**
     * Get the zlib compression level by the user-friendly display name.
     * @param name The name to look up.
     * @return The compression level as an int.
     */
    public static int getLevelByName(String name) {
        for (CompressionTypesEnum value : CompressionTypesEnum.values()) {
            if (name.equals(value.getDisplayName())) {
                return value.getLevel();
            }
        }
        return 1;
    }
}
