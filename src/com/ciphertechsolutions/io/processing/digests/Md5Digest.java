package com.ciphertechsolutions.io.processing.digests;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A class to compute the MD5 Digest of an input.
 */
public class Md5Digest extends DigestBase {

	/**
	 * Sole constructor.
	 */
    public Md5Digest() {
        super("MD5Digest");
    }

    @Override
    protected MessageDigest getDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("md5");
    }
}
