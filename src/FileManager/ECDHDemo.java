package FileManager;

import ECC.ECDH;
import ECC.ECPoint;
import Encryption.KDF;
import Encryption.KnightsTourEncryption;

import java.math.BigInteger;

public class ECDHDemo {

    public static void main(String[] args) {
        String base = System.getProperty("user.home") + "/ECDH_Demo";
        FileManager alice  = new FileManager(base + "/alice");
        FileManager bob    = new FileManager(base + "/bob");
        FileManager channel = new FileManager(base + "/public_channel");

        // Alice creates a sample file
        alice.createFile("secret.txt", "Hello Bob, this is a secret message from Alice!");

        // 1. Both generate keypairs
        BigInteger alicePriv = ECDH.generatePrivateKey();
        ECPoint    alicePub  = ECDH.generatePublicKey(alicePriv);
        BigInteger bobPriv   = ECDH.generatePrivateKey();
        ECPoint    bobPub    = ECDH.generatePublicKey(bobPriv);

        // 2. Public keys go to the channel
        channel.createFile("alice_pub.txt", alicePub.getX() + "\n" + alicePub.getY());
        channel.createFile("bob_pub.txt",   bobPub.getX()   + "\n" + bobPub.getY());

        // 3. Alice computes shared secret + key
        ECPoint aliceShared = ECDH.computeSharedSecret(alicePriv, bobPub);
        KDF kdf = new KDF();
        KDF.KDFResult aliceKdf = kdf.computeKDF(aliceShared);
        channel.createFile("salt.bin", aliceKdf.salt);

        // 4. Alice encrypts file and publishes
        KnightsTourEncryption cipher = new KnightsTourEncryption();
        byte[] plaintext  = alice.readFileAsBytes("secret.txt");
        byte[] ciphertext = cipher.encryptBytes(plaintext, aliceKdf.hash);
        channel.createFile("encrypted.bin", ciphertext);

        // 5. Bob computes the same key
        ECPoint bobShared = ECDH.computeSharedSecret(bobPriv, alicePub);
        byte[] bobKey = kdf.recomputeKDF(bobShared, aliceKdf.salt);

        // 6. Bob decrypts
        byte[] received  = channel.readFileAsBytes("encrypted.bin");
        byte[] decrypted = cipher.decryptBytes(received, bobKey);
        bob.createFile("secret.txt", decrypted);

        // Print result
        System.out.println("Original:  " + new String(plaintext));
        System.out.println("Decrypted: " + new String(decrypted));
        System.out.println("Match: " + new String(plaintext).equals(new String(decrypted)));
    }
}