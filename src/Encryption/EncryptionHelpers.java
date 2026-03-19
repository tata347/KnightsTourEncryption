package Encryption;

import java.awt.*;
import classes.point;

public class EncryptionHelpers {
    public static byte encryptByte(byte b, byte[] key, point p) {
        int pos = p.row ^ p.col;
        return (byte) (b ^ key[pos % 256]);
    }
    // Strongest effect — uses 3 key bytes + full bit rotation
    public static byte encryptByteBig(byte b, byte[] key, point p) {
        int pos = p.row ^ p.col;
        byte xored = (byte) (b ^ key[pos % 256]);
        xored = (byte) (xored ^ key[(pos + 128) % 256]);
        xored = (byte) (xored ^ key[(pos + 64) % 256]);
        return (byte) xored; // rotate bits
    }

    // Medium effect — uses 2 key bytes + mild scramble
    public static byte encryptByteMed(byte b, byte[] key, point p) {
        int pos = p.row ^ p.col;
        byte xored = (byte) (b ^ key[(pos + 32) % 256]);
        xored = (byte) (xored ^ key[(pos + 96) % 256]);
        return (byte) xored;
    }

    // Weak effect — uses 1 key byte
    public static byte encryptByteSmall(byte b, byte[] key, point p) {
        int pos = p.row ^ p.col;
        return (byte) (b ^ (key[(pos + 16) % 256]));
    }


    public static byte decryptByte(byte b, byte[] key, point p) {
        int pos = p.row ^ p.col;
        return (byte) (b ^ key[pos % 256]);
    }

    public static byte decryptByteBig(byte b, byte[] key, point p) {
        int pos = p.row ^ p.col;
        byte xored = (byte) (b ^ key[(pos + 64) % 256]);
        xored = (byte) (xored ^ key[(pos + 128) % 256]);
        xored = (byte) (xored ^ key[pos % 256]);
        return (byte) xored;
    }

    public static byte decryptByteMed(byte b, byte[] key, point p) {
        int pos = p.row ^ p.col;
        byte xored = (byte) (b ^ key[(pos + 96) % 256]);
        xored = (byte) (xored ^ key[(pos + 32) % 256]);
        return (byte) xored;
    }

    public static byte decryptByteSmall(byte b, byte[] key, point p) {
        int pos = p.row ^ p.col;
        return (byte) (b ^ key[(pos + 16) % 256]);
    }
}
