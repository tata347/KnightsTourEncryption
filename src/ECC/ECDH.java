package ECC;
import Encryption.Hash;
import Encryption.KDF;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Elliptic Curve Diffie-Hellman key exchange over secp256k1.
 *
 * Lets two parties agree on a shared secret over an open channel without
 * ever transmitting it. Each side generates a private key, derives a public
 * key from it, and exchanges public keys. Both then compute the same shared
 * point - one that an eavesdropper watching the public keys can't recover
 * (this is the elliptic curve discrete log problem, ECDLP).
 */
public class ECDH {

    /**
     * Picks a random 256-bit private key in the valid range [1, N-1].
     * Uses SecureRandom because predictability here would compromise the
     * entire key exchange. Loops until it gets a value in range - rejects
     * 0 and anything >= curve order N.
     */
    public static BigInteger generatePrivateKey(){
        SecureRandom secureRandom = new SecureRandom();
        BigInteger privateKey;

        do {
            privateKey = new BigInteger(256, secureRandom);
        } while (privateKey.compareTo(Curve.N) >= 0 || privateKey.compareTo(BigInteger.ZERO) <= 0);
        return privateKey;
    }

    /** Derives the public key by computing privateKey * G on the curve. */
    public static ECPoint generatePublicKey(BigInteger privateKey){
        return PointMath.scalarMultiply(Curve.G, privateKey);
    }

    /**
     * Computes the shared secret point: my private key * their public key.
     * Both sides arrive at the same point because (a*b)*G = (b*a)*G.
     * The X coordinate of this point is what gets fed into the KDF.
     */
    public static ECPoint computeSharedSecret(BigInteger myPrivateKey, ECPoint theirPublicKey){
        ECPoint sharedPoint = PointMath.scalarMultiply(theirPublicKey, myPrivateKey);
        return sharedPoint;
    }

    // simulates a full Alice-Bob key exchange end to end
    public static void main(String[] args) {
        System.out.println("=== ECDH + KDF Test ===");

        // Alice generates her keypair
        BigInteger alicePrivateKey = generatePrivateKey();
        ECPoint alicePublicKey = generatePublicKey(alicePrivateKey);

        System.out.println("\n--- Alice ---");
        System.out.println("Private Key: " + alicePrivateKey);
        System.out.println("Public Key:  " + alicePublicKey);

        // Bob generates his keypair
        BigInteger bobPrivateKey = generatePrivateKey();
        ECPoint bobPublicKey = generatePublicKey(bobPrivateKey);

        System.out.println("\n--- Bob ---");
        System.out.println("Private Key: " + bobPrivateKey);
        System.out.println("Public Key:  " + bobPublicKey);

        // each side computes the shared secret using their own private key
        // and the other's public key - the math guarantees they match
        ECPoint aliceSharedKey = computeSharedSecret(alicePrivateKey, bobPublicKey);
        ECPoint bobSharedKey = computeSharedSecret(bobPrivateKey, alicePublicKey);

        System.out.println("\n--- Shared Secret (EC Point) ---");
        System.out.println("Alice: " + aliceSharedKey);
        System.out.println("Bob:   " + bobSharedKey);

        boolean sameShared = aliceSharedKey.equals(bobSharedKey);
        System.out.println("\nShared EC Point equal? " + sameShared);

        // run the shared point through the KDF to get a usable symmetric key
        KDF kdf = new KDF();

        // Alice generates a fresh salt and derives her key
        KDF.KDFResult aliceResult = kdf.computeKDF(aliceSharedKey);

        System.out.println("\n--- Alice KDF ---");
        System.out.println("Hash: " + bytesToHex(aliceResult.hash));
        System.out.println("Salt: " + bytesToHex(aliceResult.salt));

        // Bob recomputes using Alice's salt - this is what would travel
        // through the public channel along with the public keys
        byte[] bobResult = kdf.recomputeKDF(bobSharedKey, aliceResult.salt);

        System.out.println("\n--- Bob KDF (using Alice's salt) ---");
        System.out.println("Hash: " + bytesToHex(bobResult));

        boolean sameKdf = java.util.Arrays.equals(aliceResult.hash, bobResult);
        System.out.println("\nFinal keys equal? " + sameKdf);

        if (sameShared && sameKdf) {
            System.out.println("\nSUCCESS: Full key exchange works correctly!");
        } else {
            System.out.println("\nERROR: Something is wrong in the flow!");
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}