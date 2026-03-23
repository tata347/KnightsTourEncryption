package ECC;
import Encryption.Hash;
import Encryption.KDF;

import java.math.BigInteger;
import java.security.SecureRandom;

public class ECDH {

    public static BigInteger generatePrivateKey(){
        SecureRandom secureRandom = new SecureRandom();
        BigInteger privateKey;

        do {
            privateKey = new BigInteger(256, secureRandom);
        } while (privateKey.compareTo(Curve.N) >= 0 || privateKey.compareTo(BigInteger.ZERO) <= 0);
        return privateKey;
    }

    public static ECPoint generatePublicKey(BigInteger privateKey){
        return PointMath.scalarMultiply(Curve.G, privateKey);
    }

    public static ECPoint computeSharedSecret(BigInteger myPrivateKey, ECPoint theirPublicKey){
        ECPoint sharedPoint = PointMath.scalarMultiply(theirPublicKey, myPrivateKey);
        return sharedPoint;
    }


    public static void main(String[] args) {
        System.out.println("=== ECDH + KDF Test ===");

        // --- Alice ---
        BigInteger alicePrivateKey = generatePrivateKey();
        ECPoint alicePublicKey = generatePublicKey(alicePrivateKey);

        System.out.println("\n--- Alice ---");
        System.out.println("Private Key: " + alicePrivateKey);
        System.out.println("Public Key:  " + alicePublicKey);

        // --- Bob ---
        BigInteger bobPrivateKey = generatePrivateKey();
        ECPoint bobPublicKey = generatePublicKey(bobPrivateKey);

        System.out.println("\n--- Bob ---");
        System.out.println("Private Key: " + bobPrivateKey);
        System.out.println("Public Key:  " + bobPublicKey);

        // --- Shared Secret ---
        ECPoint aliceSharedKey = computeSharedSecret(alicePrivateKey, bobPublicKey);
        ECPoint bobSharedKey = computeSharedSecret(bobPrivateKey, alicePublicKey);

        System.out.println("\n--- Shared Secret (EC Point) ---");
        System.out.println("Alice: " + aliceSharedKey);
        System.out.println("Bob:   " + bobSharedKey);

        boolean sameShared = aliceSharedKey.equals(bobSharedKey);
        System.out.println("\nShared EC Point equal? " + sameShared);

        // --- KDF ---
        KDF kdf = new KDF();

        // Alice generates salt + key
        KDF.KDFResult aliceResult = kdf.computeKDF(aliceSharedKey);

        System.out.println("\n--- Alice KDF ---");
        System.out.println("Hash: " + bytesToHex(aliceResult.hash));
        System.out.println("Salt: " + bytesToHex(aliceResult.salt));

        // Bob uses Alice's salt
        byte[] bobResult = kdf.recomputeKDF(bobSharedKey, aliceResult.salt);

        System.out.println("\n--- Bob KDF (using Alice's salt) ---");
        System.out.println("Hash: " + bytesToHex(bobResult));

        // Compare final keys
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
