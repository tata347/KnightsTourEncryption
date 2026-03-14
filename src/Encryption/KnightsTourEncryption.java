package Encryption;
import Algorithms.KnightsTour;
import classes.point;

public class KnightsTourEncryption {
     public knightsTourEncryptSimple(byte[] plain, byte[] key)
     {

     }

    private point keyToPoint(byte[] key) {
        int x = 0, y = 0;
        for (int i = 0; i < 256; i++) {
            x ^= Byte.toUnsignedInt(key[i]);
            y ^= Byte.toUnsignedInt(key[255 - i]);
        }
        return new point(x % 8, y % 8);
    }
}
