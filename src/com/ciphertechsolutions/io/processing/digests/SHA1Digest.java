package com.ciphertechsolutions.io.processing.digests;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
/**
 * A class to compute the SHA1 Digest of an input.
 */
public class SHA1Digest extends DigestBase {

	/**
	 * Sole constructor.
	 */
    public SHA1Digest() {
        super("SHA1Digest");
    }

    @Override
    protected MessageDigest getDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA1");
    }

}
