package Encryption;
import Algorithms.KnightsTour;
import classes.Point;
import java.util.HashSet;
import java.util.Set;

public class KnightsTourEncryption {

    static final int size = 8;

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

            chunk = knightsTourEncryptChunkComplicated(chunk, key);

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

            chunk = knightsTourDecryptChunkComplicated(chunk, key);

            for (int i = 0; i < size * size; i++) {
                int dataIndex = c * size * size + i;
                if (dataIndex < data.length)
                    result[dataIndex] = chunk[i / size][i % size];
            }
        }
        return result;
    }

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

    public byte[][] knightsTourEncryptChunkComplicated(byte[][] chunk, byte[] key){
        Point start = keyToPoint(key);
        Point[] path = KnightsTour.knightsTourPath(start.row, start.col, size);
        for (int i = 0; i < path.length; i++) {
            Point cur = path[i];
            chunk = encryptChunkWithNeighbors(chunk, key, cur);
        }
        return chunk;
    }

    public byte[][] knightsTourDecryptChunkComplicated(byte[][] chunk, byte[] key){
        Point start = keyToPoint(key);
        Point[] path = KnightsTour.knightsTourPath(start.row, start.col, size);
        for (int i = 0; i < path.length; i++) {
            //you have to walk backwards for decryption
            Point cur = path[path.length - i - 1];
            chunk = decryptChunkWithNeighbors(chunk, key, cur);
        }
        return chunk;
    }


    // set is o(n) to build here instead of array, removing duplicates is o(n^2), n is small but is computed many times
    public byte[][] encryptChunkWithNeighbors(byte[][] chunk, byte[] key, Point cur) {
        int[][] directions = {{0,1},{0,-1},{1,0},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};

        // Encrypt cur with Big
        chunk[cur.col][cur.row] = EncryptionHelpers.encryptByteBig(chunk[cur.col][cur.row], key, cur);

        // Encrypt ring 1 (direct neighbors) with Med
        Set<Point> ring1 = getNeighbors(cur, chunk, directions);
        for (Point p : ring1) {
            chunk[p.col][p.row] = EncryptionHelpers.encryptByteMed(chunk[p.col][p.row], key, p);
        }

        // Encrypt ring 2 (neighbors of neighbors, excluding already-processed) with Small
        Set<Point> ring2 = new HashSet<>();
        for (Point p : ring1) {
            ring2.addAll(getNeighbors(p, chunk, directions));
        }
        ring2.remove(cur);
        ring2.removeAll(ring1);

        for (Point p : ring2) {
            chunk[p.col][p.row] = EncryptionHelpers.encryptByteSmall(chunk[p.col][p.row], key, p);
        }

        return chunk;
    }

    public byte[][] decryptChunkWithNeighbors(byte[][] chunk, byte[] key, Point cur) {
        int[][] directions = {{0,1},{0,-1},{1,0},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};

        // Decrypt cur with Big
        chunk[cur.col][cur.row] = EncryptionHelpers.decryptByteBig(chunk[cur.col][cur.row], key, cur);

        // Decrypt ring 1 (direct neighbors) with Med
        Set<Point> ring1 = getNeighbors(cur, chunk, directions);
        for (Point p : ring1) {
            chunk[p.col][p.row] = EncryptionHelpers.decryptByteMed(chunk[p.col][p.row], key, p);
        }

        // Decrypt ring 2 (neighbors of neighbors, excluding already-processed) with Small
        Set<Point> ring2 = new HashSet<>();
        for (Point p : ring1) {
            ring2.addAll(getNeighbors(p, chunk, directions));
        }
        ring2.remove(cur);
        ring2.removeAll(ring1);

        for (Point p : ring2) {
            chunk[p.col][p.row] = EncryptionHelpers.decryptByteSmall(chunk[p.col][p.row], key, p);
        }

        return chunk;
    }

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

    private Point keyToPoint(byte[] key) {
        int x = 0, y = 0;
        for (int i = 0; i < 128; i++) {
            x ^= Byte.toUnsignedInt(key[i]);
            y ^= Byte.toUnsignedInt(key[255 - i]);
        }
        return new Point(x % 8, y % 8);
    }
}