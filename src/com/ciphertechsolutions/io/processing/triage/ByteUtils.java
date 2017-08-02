package com.ciphertechsolutions.io.processing.triage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of byte manipulation utility functions.
 */
public class ByteUtils {

	/**
	 * Converts a long to a little endian byte array.
	 * @param l The long to convert
	 * @return A little endian array.
	 */
    public static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 0; i <= 7; i++) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    /**
     * Converts an int to a little endian byte array.
     * @param _int The int to convert.
     * @return A little endian array.
     */
    public static byte[] intToBytes(int _int) {
        byte[] result = new byte[4];
        for (int i = 0; i <= 3; i++) {
            result[i] = (byte)(_int & 0xFF);
            _int >>= 8;
        }
        return result;
    }

    public static Map<Integer, String> printableSpansWithIndexes(byte[] bytes, int minLength, boolean filterRandom, int randomThreshold) {

        Map<Integer, String> spanPairs = new HashMap<>();
        int[] table = new int[bytes.length];
        int tableSize = 0;
        int strStart, strSize;
        byte[] byteSpan;
        String span;


        for (int i = 0; i < bytes.length; i++)
        {
            if (!isPrintable(bytes[i]))
            {
                table[tableSize++] = i;
            }
        }

        for (int j = 1; j < tableSize; j++)
        {
            if (table[j] == 0)
            {
                continue;
            }

            strStart = table[j - 1] + 1;
            strSize = table[j] - strStart;

            if (strSize < minLength)
            {
                continue;
            }


            byteSpan = Arrays.copyOfRange(bytes, strStart, strStart + strSize);
            span = new String(byteSpan, StandardCharsets.US_ASCII);
            spanPairs.put(strStart, span);
        }

        if (filterRandom)
        {
            //TODO: Bother with this?
            return spanPairs;
        }
        else
        {
            return spanPairs;
        }
    }

    private static boolean isPrintable(byte b)
    {
        return (b >= 0x20 && b < 0x7F) || b == 0x0a || b == 0x0d || b == 0x09;
    }
}
