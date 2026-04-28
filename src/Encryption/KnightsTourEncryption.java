package Encryption;
import Algorithms.KnightsTour;
import Algorithms.Point;
import java.util.HashSet;
import java.util.Set;

/**
 * Encrypts and decrypts byte arrays by walking a Knights Tour over 8x8 chunks.
 * Each visited cell is transformed with two rings of neighbors
 * so a small change in input affects the whole chunk.
 */
public class KnightsTourEncryption {

    static final int size = 8;

    /** Splits data into 8x8 chunks, encrypts each chunk, returns the result as one byte array. */
    public byte[] encryptBytes(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        int totalChunks = (int) Math.ceil((double) data.length / (size * size));

        for (int c = 0; c < totalChunks; c++) {
            byte[][] chunk = new byte[size][size];

            // load chunk from data, padding with zeros if needed
            for (int i = 0; i < size * size; i++) {
                int dataIndex = c * size * size + i;
                chunk[i / size][i % size] = dataIndex < data.length ? data[dataIndex] : 0;
            }

            chunk = knightsTourEncryptChunkComplicated(chunk, key);

            // write chunk back, skipping any padding bytes
            for (int i = 0; i < size * size; i++) {
                int dataIndex = c * size * size + i;
                if (dataIndex < data.length)
                    result[dataIndex] = chunk[i / size][i % size];
            }
        }
        return result;
    }

    /** Reverse encryptBytes, same key produces the original data back. */
    public byte[] decryptBytes(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        int totalChunks = (int) Math.ceil((double) data.length / (size * size));

        for (int c = 0; c < totalChunks; c++) {
            byte[][] chunk = new byte[size][size];

            for (int i = 0; i < size * size; i++) {
                int dataIndex = c * size * size + i;
                chunk[i / size][i % size] = dataIndex < data.length ? data[dataIndex] : 0;
            }

            chunk = knightsTourDecryptChunkComplicated(chunk, key);

            for (int i = 0; i < size * size; i++) {
                int dataIndex = c * size * size + i;
                if (dataIndex < data.length)
                    result[dataIndex] = chunk[i / size][i % size];
            }
        }
        return result;
    }

    // simple variant, encrypts only the cells the knight visits, no neighbors

    public byte[][] knightsTourEncryptChunkSimple(byte[][] chunk, byte[] key) {
        Point start = keyToPoint(key);
        Point[] path = KnightsTour.knightsTourPath(start.row, start.col, size);
        for (int i = 0; i < path.length; i++) {
            Point cur = path[i];
            chunk[cur.col][cur.row] = EncryptionHelpers.encryptByte(chunk[cur.col][cur.row], key, cur);
        }
        return chunk;
    }

    public byte[][] knightsTourDecryptChunkSimple(byte[][] chunk, byte[] key) {
        Point start = keyToPoint(key);
        Point[] path = KnightsTour.knightsTourPath(start.row, start.col, size);
        for (int i = 0; i < path.length; i++) {
            Point cur = path[path.length - i - 1];
            chunk[cur.col][cur.row] = EncryptionHelpers.decryptByte(chunk[cur.col][cur.row], key, cur);
        }
        return chunk;
    }

    // complicated variant, each step also transforms the surrounding neighbors

    public byte[][] knightsTourEncryptChunkComplicated(byte[][] chunk, byte[] key){
        Point start = keyToPoint(key);
        Point[] path = KnightsTour.knightsTourPath(start.row, start.col, size);
        for (int i = 0; i < path.length; i++) {
            Point cur = path[i];
            chunk = encryptChunkWithNeighbors(chunk, key, cur, i);
        }
        return chunk;
    }

    public byte[][] knightsTourDecryptChunkComplicated(byte[][] chunk, byte[] key){
        Point start = keyToPoint(key);
        Point[] path = KnightsTour.knightsTourPath(start.row, start.col, size);
        for (int i = 0; i < path.length; i++) {
            Point cur = path[path.length - i - 1];
            int originalStep = path.length - i - 1;  // must reuse the same step value used during encryption
            chunk = decryptChunkWithNeighbors(chunk, key, cur, originalStep);
        }
        return chunk;
    }

    /**
     * Encrypts the current cell strongly, ring 1 (neighbors) medium,
     * and ring 2 (neighbors of neighbors) lightly. The step number is mixed
     * into the key byte so each visit produces a different transformation.
     */
    public byte[][] encryptChunkWithNeighbors(byte[][] chunk, byte[] key, Point cur, int step) {
        int[][] directions = {{0,1},{0,-1},{1,0},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};

        byte stepByte = (byte)(key[step % key.length] ^ step);

        // current cell - strongest transformation
        chunk[cur.col][cur.row] ^= stepByte;
        chunk[cur.col][cur.row] = EncryptionHelpers.encryptByteBig(chunk[cur.col][cur.row], key, cur);

        // ring 1 - direct neighbors, medium transformation
        Set<Point> ring1 = getNeighbors(cur, chunk, directions);
        for (Point p : ring1) {
            chunk[p.col][p.row] ^= (byte)(stepByte >>> 1);
            chunk[p.col][p.row] = EncryptionHelpers.encryptByteMed(chunk[p.col][p.row], key, p);
        }

        // ring 2 - neighbors of neighbors, lightest transformation
        Set<Point> ring2 = new HashSet<>();
        for (Point p : ring1) {
            ring2.addAll(getNeighbors(p, chunk, directions));
        }
        ring2.remove(cur);
        ring2.removeAll(ring1);
        for (Point p : ring2) {
            chunk[p.col][p.row] ^= (byte)(stepByte >>> 2);
            chunk[p.col][p.row] = EncryptionHelpers.encryptByteSmall(chunk[p.col][p.row], key, p);
        }

        return chunk;
    }

    /** Reverse of encryptChunkWithNeighbors, undoes ring 2, then ring 1, then the center. */
    public byte[][] decryptChunkWithNeighbors(byte[][] chunk, byte[] key, Point cur, int step) {
        int[][] directions = {{0,1},{0,-1},{1,0},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};

        byte stepByte = (byte)(key[step % key.length] ^ step);

        Set<Point> ring1 = getNeighbors(cur, chunk, directions);
        Set<Point> ring2 = new HashSet<>();
        for (Point p : ring1) {
            ring2.addAll(getNeighbors(p, chunk, directions));
        }
        ring2.remove(cur);
        ring2.removeAll(ring1);
        for (Point p : ring2) {
            chunk[p.col][p.row] = EncryptionHelpers.decryptByteSmall(chunk[p.col][p.row], key, p);
            chunk[p.col][p.row] ^= (byte)(stepByte >>> 2);
        }

        for (Point p : ring1) {
            chunk[p.col][p.row] = EncryptionHelpers.decryptByteMed(chunk[p.col][p.row], key, p);
            chunk[p.col][p.row] ^= (byte)(stepByte >>> 1);
        }

        chunk[cur.col][cur.row] = EncryptionHelpers.decryptByteBig(chunk[cur.col][cur.row], key, cur);
        chunk[cur.col][cur.row] ^= stepByte;

        return chunk;
    }

    /** Returns the up-to-8 valid neighbors of a point inside the chunk. */
    private Set<Point> getNeighbors(Point p, byte[][] chunk, int[][] directions) {
        Set<Point> neighbors = new HashSet<>();
        for (int[] d : directions) {
            int newCol = p.col + d[0];
            int newRow = p.row + d[1];
            if (newCol >= 0 && newCol < chunk.length &&
                    newRow >= 0 && newRow < chunk[0].length) {
                neighbors.add(new Point(newCol, newRow));
            }
        }
        return neighbors;
    }

    /** Derives a starting (col, row) on the 8x8 board by XOR-folding the 256-byte key. */
    private Point keyToPoint(byte[] key) {
        int x = 0, y = 0;
        for (int i = 0; i < 128; i++) {
            x ^= Byte.toUnsignedInt(key[i]);
            y ^= Byte.toUnsignedInt(key[255 - i]);
        }
        return new Point(x % 8, y % 8);
    }
}