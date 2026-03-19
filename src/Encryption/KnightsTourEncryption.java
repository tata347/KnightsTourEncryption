package Encryption;
import Algorithms.KnightsTour;
import classes.point;

public class KnightsTourEncryption {

    static final int size = 8;

    public static void main(String[] args) {
        KnightsTourEncryption kte = new KnightsTourEncryption();

        byte[] key = new byte[256];
        for (int i = 0; i < 256; i++) key[i] = (byte)(i + 1);

        byte[] original = "Hello World".getBytes();
        System.out.println("Original:  " + new String(original));

        byte[] encrypted = kte.encryptBytes(original, key);
        byte[] decrypted = kte.decryptBytes(encrypted, key);
        System.out.println("Decrypted: " + new String(decrypted).trim());
        System.out.println("Match: " + java.util.Arrays.equals(original, decrypted));
    }

    public byte[] encryptBytes(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        int totalChunks = (int) Math.ceil((double) data.length / (size * size));

        for (int c = 0; c < totalChunks; c++) {
            byte[][] chunk = new byte[size][size];

            // fill chunk
            for (int i = 0; i < size * size; i++) {
                int dataIndex = c * size * size + i;
                chunk[i / size][i % size] = dataIndex < data.length ? data[dataIndex] : 0;
            }

            chunk = knightsTourEncryptChunkSimple(chunk, key);

            // flatten back
            for (int i = 0; i < size * size; i++) {
                int dataIndex = c * size * size + i;
                if (dataIndex < data.length)
                    result[dataIndex] = chunk[i / size][i % size];
            }
        }
        return result;
    }

    public byte[] decryptBytes(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        int totalChunks = (int) Math.ceil((double) data.length / (size * size));

        for (int c = 0; c < totalChunks; c++) {
            byte[][] chunk = new byte[size][size];

            for (int i = 0; i < size * size; i++) {
                int dataIndex = c * size * size + i;
                chunk[i / size][i % size] = dataIndex < data.length ? data[dataIndex] : 0;
            }

            chunk = knightsTourDecryptChunkSimple(chunk, key);

            for (int i = 0; i < size * size; i++) {
                int dataIndex = c * size * size + i;
                if (dataIndex < data.length)
                    result[dataIndex] = chunk[i / size][i % size];
            }
        }
        return result;
    }

    public byte[][] knightsTourEncryptChunkSimple(byte[][] chunk, byte[] key) {
        point start = keyToPoint(key);
        point[] path = KnightsTour.knightsTourPath(start.row, start.col, size);
        for (int i = 0; i < path.length; i++) {
            point cur = path[i];
            chunk[cur.col][cur.row] = EncryptionHelpers.encryptByte(chunk[cur.col][cur.row], key, cur);
        }
        return chunk;
    }

    public byte[][] knightsTourDecryptChunkSimple(byte[][] chunk, byte[] key) {
        point start = keyToPoint(key);
        point[] path = KnightsTour.knightsTourPath(start.row, start.col, size);
        for (int i = 0; i < path.length; i++) {
            point cur = path[i];
            chunk[cur.col][cur.row] = EncryptionHelpers.decryptByte(chunk[cur.col][cur.row], key, cur);
        }
        return chunk;
    }

    private point keyToPoint(byte[] key) {
        int x = 0, y = 0;
        for (int i = 0; i < 128; i++) {
            x ^= Byte.toUnsignedInt(key[i]);
            y ^= Byte.toUnsignedInt(key[255 - i]);
        }
        return new point(x % 8, y % 8);
    }
}