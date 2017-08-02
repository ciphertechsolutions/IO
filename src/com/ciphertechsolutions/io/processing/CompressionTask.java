package com.ciphertechsolutions.io.processing;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.xml.bind.DatatypeConverter;

import com.ciphertechsolutions.io.ewf.DataChunk;
import com.ciphertechsolutions.io.processing.triage.ByteUtils;

class CompressionTask implements Callable<DataChunk> {

    private final byte[] input;
    private final int compressionLevel;

    CompressionTask(byte[] toCompress, int compressionLevel) {
        input = toCompress;
        this.compressionLevel = compressionLevel;
    }

    @Override
    public DataChunk call() throws Exception {
        Deflater deflater = new Deflater(compressionLevel);
        deflater.setInput(input);
        deflater.finish();
        byte[] output = new byte[input.length + 4];
        int compressedSize = deflater.deflate(output);
        //Unlikely, but possible.
        if (compressedSize >= input.length) {
            System.arraycopy(input, 0, output, 0, input.length);
            System.arraycopy(ByteUtils.intToBytes(deflater.getAdler()), 0, output, input.length, 4);
            return new DataChunk(input.length, output, false);
        }
        return new DataChunk(input.length, Arrays.copyOf(output, compressedSize), true);
    }

    /**
     * Testing method for decompression of ewf file hex bytes
     * @param ewfHexStr any zlib compressed hex
     * @return decompressed string
     */
    protected static String decompress(String ewfHexStr) {
		Inflater inflater = new Inflater();
		byte[] input = DatatypeConverter.parseHexBinary(ewfHexStr);
		inflater.setInput(input, 0, input.length);
		String outputString = "empty";

		byte[] result = new byte[input.length];
		int resultLength;
		try {
			resultLength = inflater.inflate(result);
			outputString = new String(result, 0, resultLength, "UTF-8");
		} catch (DataFormatException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		inflater.end();

		return outputString;
    }
}
