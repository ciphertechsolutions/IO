package com.ciphertechsolutions.io.processing.triage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.ciphertechsolutions.io.logging.LogMessageType;
import com.ciphertechsolutions.io.logging.Logging;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDescriptor;
import com.drew.metadata.exif.GpsDirectory;

import javafx.util.Pair;

/**
 * A processor that guesses what types of files are present based on byte patterns found.
 */
public class MagicCarver extends TriageProcessorBase {

    private static final Map<String, byte[][]> DEFAULT_MAGICS = new TreeMap<>();

    static {
        DEFAULT_MAGICS.put("ASF header",
                new byte[][] {
                        new byte[] { 0x30, 0x26, (byte) 0xB2, 0x75, (byte) 0x8E, 0x66, (byte) 0xCF, 0x11, (byte) 0xA6, (byte) 0xD9, 0x00, (byte) 0xAA, 0x00,
                                0x62, (byte) 0xCE, 0x6C } });
        DEFAULT_MAGICS.put("BMP header",
                new byte[][] {
                        new byte[] { 0x42, 0x4D } });
        DEFAULT_MAGICS.put("BZip2 header",
                new byte[][] {
                        new byte[] { 0x42, 0x5A, 0x68 } });
        DEFAULT_MAGICS.put("ELF header",
                new byte[][] {
                        new byte[] { 0x7F, 0x45, 0x4C, 0x46 } });
        DEFAULT_MAGICS.put("GIF header",
                new byte[][] {
                        new byte[] { 0x47, 0x49, 0x46, 0x38, 0x37, 0x61 },
                        new byte[] { 0x47, 0x49, 0x46, 0x38, 0x39, 0x61 } });
        DEFAULT_MAGICS.put("GZIP header",
                new byte[][] {
                        new byte[] { 0x1F, (byte) 0x8B, 0x08, 0x00 },
                        new byte[] { 0x1F, (byte) 0x8B, 0x08, 0x08 } });
        DEFAULT_MAGICS.put("ISO header",
                new byte[][] {
                        new byte[] { 0x43, 0x44, 0x30, 0x30, 0x31 } });
        DEFAULT_MAGICS.put("Java class/Universal Mach-O header",
                new byte[][] {
                        new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE } });
        DEFAULT_MAGICS.put("JPEG header",
                new byte[][] {
                        new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDB },
                        new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0 },
                        new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1 } });
        DEFAULT_MAGICS.put("Mach-O header",
                new byte[][] {
                        new byte[] { (byte) 0xFE, (byte) 0xED, (byte) 0xFA, (byte) 0xCE },
                        new byte[] { (byte) 0xFE, (byte) 0xED, (byte) 0xFA, (byte) 0xCF } });
        DEFAULT_MAGICS.put("Matroska header",
                new byte[][] {
                        new byte[] { 0x1A, 0x45, (byte) 0xDF, (byte) 0xA3 } });
        DEFAULT_MAGICS.put("OGG header",
                new byte[][] {
                        new byte[] { 0x4F, 0x67, 0x67, 0x53 } });
        DEFAULT_MAGICS.put("OLE Compound File header",
                new byte[][] {
                        new byte[] { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1 },
                        new byte[] { 0x0E, 0x11, (byte) 0xFC, 0x0D, (byte) 0xD0, (byte) 0xCF, 0x11, 0x0E } });
        DEFAULT_MAGICS.put("OOXML Document header (ZIP)",
                new byte[][] {
                        new byte[] { 0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00 } });
        DEFAULT_MAGICS.put("PDF header",
                new byte[][] {
                        new byte[] { 0x25, 0x50, 0x44, 0x46 } });
        DEFAULT_MAGICS.put("PNG header",
                new byte[][] {
                        new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A } });
        DEFAULT_MAGICS.put("RAR header",
                new byte[][] {
                        new byte[] { 0x52, 0x61, 0x72, 0x21, 0x1A } });
        DEFAULT_MAGICS.put("RIFF header",
                new byte[][] {
                        new byte[] { 0x52, 0x49, 0x46, 0x46 } });
        DEFAULT_MAGICS.put("TIFF header",
                new byte[][] {
                        new byte[] { 0x49, 0x49, 0x2A, 0x00 },
                        new byte[] { 0x4D, 0x4D, 0x00, 0x2A } });
        DEFAULT_MAGICS.put("ZIP header",
                new byte[][] {
                        new byte[] { 0x50, 0x4B, 0x03, 0x04 },
                        new byte[] { 0x50, 0x4B, 0x05, 0x06 },
                        new byte[] { 0x50, 0x4B, 0x07, 0x08 } });
        DEFAULT_MAGICS.put("7-Zip header",
                new byte[][] {
                        new byte[] { 0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C } });
    }

    private Map<String, byte[][]> magics;
    private final MapAndOrStrings[] byteMapping = new MapAndOrStrings[256];
    private final boolean[] byteTest = new boolean[256];
    private final boolean[] firstByteTest = new boolean[256];
    private final Map<String, Integer> resultMap = Collections.synchronizedSortedMap(new TreeMap<>());
    private boolean wasGPSFound = false;
    private final String emptyGPS = "0\u00B0 0' 0\"";

    /**
     * Sole constructor.
     * 
     * @param readSize The default size of chunks that will be passed to this carver, used for memory management purposes.
     */
    public MagicCarver(int readSize) {
        super("MagicCarver", readSize);
    }

    @Override
    public void internalProcess() {
        try {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                Pair<byte[], Long> toRead;
                toRead = byteQueue.poll(5, TimeUnit.SECONDS);
                if (toRead == null) {
                    continue;
                }
                byte[] bytesToRead = toRead.getKey();
                if (bytesToRead.length == 0) {
                    isRunning = false;
                    return;
                }
                matchThings(bytesToRead, toRead.getValue());
            }
        }
        catch (InterruptedException e) {
            Logging.log(e);
            return;
        }
    }

    @Override
    public void initialize() {
        magics = DEFAULT_MAGICS;
        preprocess();
        startThreads();
    }

    private void matchThings(byte[] toMatch, long offset) {
        // TODO: Determine how much can be moved to helper functions without meaningful processing speed impact.
        List<MapAndOrStrings[]> potentialMatches = new ArrayList<>();
        List<MapAndOrStrings[]> nextMatches = new ArrayList<>();
        List<MapAndOrStrings[]> swap = new ArrayList<>();
        boolean clear = true;
        for (int i = 0; i < toMatch.length; i++) {
            int toFind = 0xff & toMatch[i];
            if (firstByteTest[toFind] || (!clear && byteTest[toFind])) {
                if (clear) {
                    potentialMatches.clear();
                    clear = false;
                }
                potentialMatches.add(byteMapping);
                for (MapAndOrStrings[] map : potentialMatches) {
                    MapAndOrStrings result = map[toFind];
                    if (result != null) {
                        if (!result.strings.isEmpty()) {
                            recordResults(toMatch, i, result);
                        }
                        MapAndOrStrings[] newMap = result.byteMapping;
                        if (newMap != null) {
                            nextMatches.add(newMap);
                        }
                    }
                }
                swap = potentialMatches;
                potentialMatches = nextMatches;
                nextMatches = swap;
                swap.clear();
            }
            else {
                clear = true;
            }
        }
    }

    private void recordResults(byte[] toMatch, int i, MapAndOrStrings result) {
        List<String> results = result.strings;
        List<Integer> offsets = result.stringOffsets;
        for (int resultIndex = 0; resultIndex < offsets.size(); resultIndex++) {
            String resultName = results.get(resultIndex);
            if (resultName.equals("JPEG header")) {
                getJpegData(toMatch, i, offsets, resultIndex);
            }
            if (resultName.equals("BMP header")) {
                if (toMatch.length >= i + 10 && toMatch[i + 5] == 0 && toMatch[i + 6] == 0 && toMatch[i + 7] == 0 &&
                        toMatch[i + 8] == 0 && toMatch[i + 9] != 0) {
                    resultMap.merge(results.get(resultIndex), new Integer(1), (A, B) -> A + 1);
                }
            }
            else {
                resultMap.merge(results.get(resultIndex), new Integer(1), (A, B) -> A + 1);
            }
        }
    }

    protected void getJpegData(byte[] toMatch, int i, List<Integer> offsets, int resultIndex) {
        try {
            Metadata reader = JpegMetadataReader
                    .readMetadata(new ByteArrayInputStream(Arrays.copyOfRange(toMatch, i - offsets.get(resultIndex), toMatch.length)));
            ExifIFD0Directory info = reader.getFirstDirectoryOfType(ExifIFD0Directory.class);
            String cameraMake = null;
            String cameraModel = null;
            if (info != null) {
                cameraMake = info.getString(ExifDirectoryBase.TAG_MAKE);
                cameraModel = info.getString(ExifDirectoryBase.TAG_MODEL);
            }
            for (Directory dir : reader.getDirectoriesOfType(GpsDirectory.class)) {
                GpsDescriptor descriptor = new GpsDescriptor((GpsDirectory) dir);

                Map<String, String> gpsInfoDict = new LinkedHashMap<>();
                gpsInfoDict.put("Camera Make: ", cameraMake);
                gpsInfoDict.put("Camera Model: ", cameraModel);
                gpsInfoDict.put("Latitude: ", tagOrEmpty(descriptor.getGpsLatitudeDescription()));
                gpsInfoDict.put("Longitude: ", tagOrEmpty(descriptor.getGpsLongitudeDescription()));
                gpsInfoDict.put("Altitude: ", descriptor.getGpsAltitudeDescription());
                gpsInfoDict.put("Altitude Reference: ", descriptor.getGpsAltitudeRefDescription());
                gpsInfoDict.put("Timestamp: ", descriptor.getGpsTimeStampDescription());

                if (notEmptyGPS(descriptor.getGpsLatitudeDescription(), descriptor.getGpsLongitudeDescription())) {
                    logGPS(gpsInfoDict);
                    wasGPSFound = true;
                }

            }

        }
        catch (IOException | ImageProcessingException e) {
            e.printStackTrace();
        }
    }

    private boolean notEmptyGPS(String lat, String longGPS) {
        if (lat == null || longGPS == null || lat.isEmpty() || (lat.equals(emptyGPS) && longGPS.equals(emptyGPS))) {
            return false;
        }
        return true;
    }

    /**
     * Avoids null Strings
     * 
     * @param tag the questionable String
     * @return Empty String or the non-null String
     */
    private String tagOrEmpty(String tag) {
        if (tag != null && !tag.isEmpty()) {
            return tag;
        }
        else {
            return "";
        }
    }

    /**
     * Logs GPS to CSV using logSimple()
     */
    private void logGPS(Map<String, String> gpsInfoDict) {
        StringBuilder gpsBuilder = new StringBuilder();
        for (Entry<String, String> info : gpsInfoDict.entrySet()) {
            logTagIfAvailable(info.getKey(), info.getValue());
            gpsBuilder.append(tagOrEmpty(info.getValue()));
            gpsBuilder.append(",");
        }
        gpsBuilder.deleteCharAt(gpsBuilder.length() - 1);
        Logging.logSimple(gpsBuilder.toString(), LogMessageType.GPS);
    }

    private void logTagIfAvailable(String baseString, String tag) {
        if (tag != null && !tag.isEmpty()) {
            Logging.log(baseString + tag, LogMessageType.REPORT, LogMessageType.USER);
            // TODO, second log of ALL tags onto a single line
        }
    }

    private void preprocess() {

        for (Map.Entry<String, byte[][]> magicsPair : magics.entrySet()) {
            for (byte[] magic : magicsPair.getValue()) {
                firstByteTest[0xff & magic[0]] = true;
                for (byte b : magic) {
                    byteTest[0xff & b] = true;
                }
                extendMapping(byteMapping, 0, magic, magicsPair.getKey());
            }
        }
    }

    private void extendMapping(MapAndOrStrings[] byteMapping, int depth, byte[] magic, String key) {
        if (byteMapping[0xff & magic[depth]] == null) {
            byteMapping[0xff & magic[depth]] = new MapAndOrStrings();
        }
        extendInnerMapping(byteMapping[0xff & magic[depth]], depth + 1, magic, key);
    }

    private void extendInnerMapping(MapAndOrStrings mapAndOrStrings, int depth, byte[] magic, String key) {
        if (depth == magic.length) {
            mapAndOrStrings.addResult(key, depth - 1);
        }
        else {
            extendMapping(mapAndOrStrings.byteMapping, depth, magic, key);
        }

    }

    @Override
    public void waitForExit() {
        super.waitForExit();
        Logging.log("File count totals: ", LogMessageType.REPORT, LogMessageType.USER);
        for (Map.Entry<String, Integer> entry : resultMap.entrySet()) {
            Logging.log(entry.getKey() + ": " + entry.getValue(), LogMessageType.REPORT, LogMessageType.USER);
        }
        if (wasGPSFound) {
            Logging.log("Geolocation Information Found", LogMessageType.REPORT, LogMessageType.USER);
        }

    }

    @Override
    protected int getThreadCount() {
        return 6;
    }
}

final class MapAndOrStrings {
    final List<String> strings = new ArrayList<>();
    final List<Integer> stringOffsets = new ArrayList<>();
    final MapAndOrStrings[] byteMapping = new MapAndOrStrings[256];

    public void addResult(String result, int depth) {
        this.strings.add(result);
        this.stringOffsets.add(depth);
    }

}