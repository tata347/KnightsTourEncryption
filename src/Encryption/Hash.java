package Encryption;

/**
 * A custom 256-bit hash function inspired by SHA-2.
 * Uses Merkle-Damgard construction: pad the input, split into 64-byte blocks,
 * compress each block into an 8-int state, then pack into 32 bytes.
 */
public class Hash {

    private static final int BLOCK_SIZE = 64;

    // initial vector - fractional parts of square roots of small primes (same trick as SHA-256)
    private static final int[] IV = {
            0x6a09e667, // sqrt2
            0xbb67ae85, // sqrt3
            0x3c6ef372, // sqrt5
            0xa54ff53a, // sqrt7
            0x510e527f, // sqrt11
            0x9b05688c, // sqrt13
            0x1f83d9ab, // sqrt17
            0x5be0cd19  // sqrt19
    };

    /** Converts bytes to a lowercase hex string for printing. */
    public static String stringToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** Hashes any byte array into a 32-byte digest. */
    public byte[] hash(byte[] input){
        int[] state = IV.clone();
        byte[] padded = pad(input);

        // process the padded input one 64-byte block at a time
        for (int i = 0; i < padded.length; i += BLOCK_SIZE) {
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(padded, i, block, 0, BLOCK_SIZE);
            state = compress(state, block);
        }

        return pack(state);
    }

    /**
     * Hashes two ints into a single int. Used where a full 32-byte digest
     * is overkill - typically as a quick mixing function inside other code.
     */
    public static int hash(int a, int b) {
        int[] state = IV.clone();

        // pack the two ints into an 8-byte input, big-endian
        byte[] input = new byte[8];
        input[0] = (byte) (a >>> 24);
        input[1] = (byte) (a >>> 16);
        input[2] = (byte) (a >>> 8);
        input[3] = (byte)  a;
        input[4] = (byte) (b >>> 24);
        input[5] = (byte) (b >>> 16);
        input[6] = (byte) (b >>> 8);
        input[7] = (byte)  b;

        // reuse the same pad + compress pipeline as the byte-array version
        Hash h = new Hash();
        byte[] padded = h.pad(input);
        for (int i = 0; i < padded.length; i += BLOCK_SIZE) {
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(padded, i, block, 0, BLOCK_SIZE);
            state = h.compress(state, block);
        }

        // fold the 8 state ints down into 1 via XOR
        int result = 0;
        for (int s : state) {
            result ^= s;
        }
        return result;
    }

    /** Turns the 8-int state into a 32-byte hash, big-endian. */
    private byte[] pack(int[] state) {
        byte[] digest = new byte[32];
        for (int i = 0; i < 8; i++) {
            digest[i * 4]     = (byte) (state[i] >>> 24); // most significant byte
            digest[i * 4 + 1] = (byte) (state[i] >>> 16);
            digest[i * 4 + 2] = (byte) (state[i] >>> 8);
            digest[i * 4 + 3] = (byte)  state[i];         // least significant byte
        }
        return digest;
    }

    /**
     * Applies Merkle-Damgard padding: appends a 0x80 delimiter, zero-pads,
     * and writes the original bit-length in the last 8 bytes. This makes
     * the input length a multiple of 64 and prevents length-extension ambiguity.
     */
    private byte[] pad(byte[] input) {

        // Calculate how many bytes are needed to fill the current block,
        // leaving 8 bytes at the end for the length.
        int remainder = input.length % BLOCK_SIZE;
        int paddedLen;
        if (remainder < 56) {
            // Enough room in current block — pad up to next multiple of 64
            paddedLen = input.length - remainder + BLOCK_SIZE;
        } else {
            // Not enough room — spill into a new block
            paddedLen = input.length - remainder + BLOCK_SIZE * 2;
        }

        byte[] padded = new byte[paddedLen];

        // Copy original input
        for (int i = 0; i < input.length; i++) {
            padded[i] = input[i];
        }

        // Append delimiter
        padded[input.length] = (byte) 0x80;

        // Zero fill is implicit in Java

        // Write original length in bits into last 8 bytes, big-endian
        long bitLen = (long) input.length * 8;
        padded[paddedLen - 8] = (byte) (bitLen >>> 56);
        padded[paddedLen - 7] = (byte) (bitLen >>> 48);
        padded[paddedLen - 6] = (byte) (bitLen >>> 40);
        padded[paddedLen - 5] = (byte) (bitLen >>> 32);
        padded[paddedLen - 4] = (byte) (bitLen >>> 24);
        padded[paddedLen - 3] = (byte) (bitLen >>> 16);
        padded[paddedLen - 2] = (byte) (bitLen >>> 8);
        padded[paddedLen - 1] = (byte) (bitLen);

        return padded;
    }

    /**
     * Mixes one 64-byte block into the state.
     * Runs 64 rounds of XOR + rotate + multiply for diffusion, then adds the
     * original state back in (Davies-Meyer) so the function is one-way.
     */
    private int[] compress(int[] state, byte[] block) {

        // transfer bytes to ints. & 0xFF is critical - bytes are signed in Java,
        // so without it negative values would sign-extend and corrupt the int
        int[] blockWords = new int[16];
        for (int i = 0; i < 16; i++) {
            int b = i * 4; // byte offset
            blockWords[i] = ((block[b]   & 0xFF) << 24)
                    | ((block[b+1] & 0xFF) << 16)
                    | ((block[b+2] & 0xFF) << 8)
                    |  (block[b+3] & 0xFF);
        }

        // save original state for Davies-Meyer feed-forward at the end
        int[] originalState = state.clone();

        // run mixing rounds
        int prime = 0x9e3779b9; // golden ratio constant - good for spreading bits
        for(int i = 0; i < 64; i++) {
            state[i % 8] ^= blockWords[i % 16];
            // rotate each time by 2, 3, 4, 5 to vary how bits move around
            int rotate = (i % 4) + 2;
            // rotation and multiplication
            state[i % 8] = Integer.rotateLeft(state[i % 8], rotate) * prime;
            // avalanche - feed each word into the next so a tiny change spreads everywhere
            state[(i + 1) % 8] ^= state[i % 8];
        }

        // Davies-Meyer feed-forward - makes the compression irreversible
        for (int i = 0; i < 8; i++) {
            state[i] += originalState[i];
        }
        return state;
    }
}