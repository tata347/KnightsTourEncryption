package Encryption;

import ECC.ECPoint;

import java.security.SecureRandom;

/**
 * Derives a 256-byte key from either a password or an ECDH shared point,
 * paired with a random 16-byte salt. Same input + same salt always produces
 * the same key.
 */
public class KDF {
    Hash h = new Hash();

    /** Holds the derived key and the salt that produced it. */
    public static class KDFResult {
        public final byte[] hash;
        public final byte[] salt;

        public KDFResult(byte[] hash, byte[] salt) {
            this.hash = hash;
            this.salt = salt;
        }
    }

    /** Derives a key from an ECDH shared point with a fresh random salt. */
    public KDFResult computeKDF(ECPoint point){
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        byte[] PRK  = h.hash(concatenate(salt, point.getX().toByteArray()));
        byte[] hash = expand(PRK, 1000);
        return new KDFResult(hash, salt);
    }

    /** Hashes a new password with a fresh random salt. */
    public KDFResult computeKDF(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        byte[] PRK  = h.hash(concatenate(salt, password.getBytes()));
        byte[] hash = expand(PRK, 1000);

        return new KDFResult(hash, salt);
    }

    /** Recomputes the hash from a password using a previously stored salt. */
    public byte[] recomputeKDF(String password, byte[] salt) {
        byte[] PRK = h.hash(concatenate(salt, password.getBytes()));
        return expand(PRK, 1000);
    }

    /** Recomputes the hash from a shared point using the salt the other party generated. */
    public byte[] recomputeKDF(ECPoint point, byte[] salt) {
        byte[] PRK  = h.hash(concatenate(salt, point.getX().toByteArray()));
        return expand(PRK, 1000);
    }

    /** Checks a password against a stored hash + salt in constant time. */
    public boolean verify(String password, byte[] storedHash, byte[] storedSalt) {
        byte[] recomputed = recomputeKDF(password, storedSalt);
        return constantTimeEquals(recomputed, storedHash);
    }

    // helpers

    /**
     * Stretches the 32-byte PRK into a 256-byte output by hashing repeatedly
     * and XORing each result into one of 8 rotating 32-byte slots.
     * The 1000 rounds make brute-force attacks slower.
     */
    private byte[] expand(byte[] PRK, int rounds) {
        byte[] current = PRK.clone();
        byte[] output  = new byte[256];

        for (int i = 0; i < rounds; i++) {
            current = h.hash(current);
            int offset = (i % 8) * 32;
            for (int j = 0; j < 32; j++) {
                output[offset + j] ^= current[j];
            }
        }

        return output;
    }

    private byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    /** Compares byte arrays in constant time to avoid timing-based password guessing. */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }

    // test
    private static void main(String[] args) {
        KDF kdf = new KDF();

        KDFResult result = kdf.computeKDF("mypassword");

        System.out.println("Hash length : " + result.hash.length + " bytes"); // 32
        System.out.println("Salt length : " + result.salt.length + " bytes"); // 16

        System.out.println("Correct  : " + kdf.verify("mypassword",  result.hash, result.salt)); // true
        System.out.println("Wrong    : " + kdf.verify("wrongpassword", result.hash, result.salt)); // false
    }
}