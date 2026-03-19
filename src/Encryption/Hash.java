package Encryption;

public class Hash {

    private static final int BLOCK_SIZE = 64;

    //initial vector
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

    public static String stringToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public byte[] hash(byte[] input){
        int[] state = IV.clone();
        byte[] padded = pad(input);

        for (int i = 0; i < padded.length; i += BLOCK_SIZE) {
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(padded, i, block, 0, BLOCK_SIZE);
            state = compress(state, block);

        }

        return pack(state);
    }

    //turn state into 32 byte hash
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

    // Append Merkle-Damgard padding to input
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
    private int[] compress(int[] state, byte[] block) {

        //transfer bytes to ints, AND with &oxFF signed bit causes issue in negatives
        int[] blockWords = new int[16];
        for (int i = 0; i < 16; i++) {
            int b = i * 4; // byte offset
            blockWords[i] = ((block[b]   & 0xFF) << 24)
                    | ((block[b+1] & 0xFF) << 16)
                    | ((block[b+2] & 0xFF) << 8)
                    |  (block[b+3] & 0xFF);
        }

        //save original state for Davies-Meyer later
        int[] originalState = state.clone();

        //run mixing rounds
        int prime = 0x9e3779b9;
        for(int i = 0; i < 64; i++) {
            state[i % 8] ^= blockWords[i % 16];
            //rotate each time by 2, 3, 4, 5
            int rotate = (i % 4) + 2;
            //rotation and multiplication
            state[i % 8] = Integer.rotateLeft(state[i % 8], rotate) * prime;
            //avalanche
            state[(i + 1) % 8] ^= state[i % 8];

        }
        //Davies-Meyer feed-forward
        for (int i = 0; i < 8; i++) {
            state[i] += originalState[i];
        }
        return state;
    }
}