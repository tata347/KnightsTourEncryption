package Encryption;

import Algorithms.Point;

/**
 * Per-byte encryption primitives used by the Knight's Tour cipher.
 * Each function XORs a byte with one or more key bytes, picked by the
 * point's position so the same key produces different transformations
 * across the 8x8 board.
 *
 * The Big/Med/Small variants exist so the Knight's Tour can apply a
 * stronger transformation at the center of each step and weaker ones
 * on the surrounding rings.
 */
public class EncryptionHelpers {

    /** Basic XOR with a single key byte - used by the simple Knight's Tour variant. */
    public static byte encryptByte(byte b, byte[] key, Point p) {
        int pos = p.row ^ p.col;
        return (byte) (b ^ key[pos % 256]);
    }

    /** Strongest effect - XORs with 3 different key bytes spread across the key. */
    public static byte encryptByteBig(byte b, byte[] key, Point p) {
        int pos = p.row ^ p.col;
        byte xored = (byte) (b ^ key[pos % 256]);
        xored = (byte) (xored ^ key[(pos + 128) % 256]);
        xored = (byte) (xored ^ key[(pos + 64) % 256]);
        return (byte) xored;
    }

    /** Medium effect - XORs with 2 key bytes. */
    public static byte encryptByteMed(byte b, byte[] key, Point p) {
        int pos = p.row ^ p.col;
        byte xored = (byte) (b ^ key[(pos + 32) % 256]);
        xored = (byte) (xored ^ key[(pos + 96) % 256]);
        return (byte) xored;
    }

    /** Weakest effect - single XOR, used on the outermost ring of neighbors. */
    public static byte encryptByteSmall(byte b, byte[] key, Point p) {
        int pos = p.row ^ p.col;
        return (byte) (b ^ (key[(pos + 16) % 256]));
    }

    // decrypt counterparts - XOR is its own inverse, so each one undoes its
    // matching encrypt by applying the exact same key bytes (order doesn't
    // matter for XOR, but the Big variant reverses it anyway for symmetry)

    public static byte decryptByte(byte b, byte[] key, Point p) {
        int pos = p.row ^ p.col;
        return (byte) (b ^ key[pos % 256]);
    }

    public static byte decryptByteBig(byte b, byte[] key, Point p) {
        int pos = p.row ^ p.col;
        byte xored = (byte) (b ^ key[(pos + 64) % 256]);
        xored = (byte) (xored ^ key[(pos + 128) % 256]);
        xored = (byte) (xored ^ key[pos % 256]);
        return (byte) xored;
    }

    public static byte decryptByteMed(byte b, byte[] key, Point p) {
        int pos = p.row ^ p.col;
        byte xored = (byte) (b ^ key[(pos + 96) % 256]);
        xored = (byte) (xored ^ key[(pos + 32) % 256]);
        return (byte) xored;
    }

    public static byte decryptByteSmall(byte b, byte[] key, Point p) {
        int pos = p.row ^ p.col;
        return (byte) (b ^ key[(pos + 16) % 256]);
    }
}