package Encryption;

import ECC.ECPoint;

import java.security.SecureRandom;

public class KDF {
    Hash h = new Hash();

    // Return type holding both hash and salt
    public static class KDFResult {
        public final byte[] hash;
        public final byte[] salt;

        public KDFResult(byte[] hash, byte[] salt) {
            this.hash = hash;
            this.salt = salt;
        }
    }
    public KDFResult computeKDF(ECPoint point){
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        byte[] PRK  = h.hash(concatenate(salt, point.getX().toByteArray()));
        byte[] hash = expand(PRK, 1000);
        return new KDFResult(hash, salt);

    }

    // Hash a new password — generates fresh salt
    public KDFResult computeKDF(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        byte[] PRK  = h.hash(concatenate(salt, password.getBytes()));
        byte[] hash = expand(PRK, 1000);

        return new KDFResult(hash, salt);
    }

    // Recompute hash using a stored salt
    public byte[] recomputeKDF(String password, byte[] salt) {
        byte[] PRK = h.hash(concatenate(salt, password.getBytes()));
        return expand(PRK, 1000);
    }

    public byte[] recomputeKDF(ECPoint point, byte[] salt) {
        byte[] PRK  = h.hash(concatenate(salt, point.getX().toByteArray()));
        return expand(PRK, 1000);
    }


    // Verify password against hash and salt
    public boolean verify(String password, byte[] storedHash, byte[] storedSalt) {
        byte[] recomputed = recomputeKDF(password, storedSalt);
        return constantTimeEquals(recomputed, storedHash);
    }

    // Helpers

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

        // Verify correct password
        System.out.println("Correct  : " + kdf.verify("mypassword",  result.hash, result.salt)); // true
        System.out.println("Wrong    : " + kdf.verify("wrongpassword", result.hash, result.salt)); // false
    }
}