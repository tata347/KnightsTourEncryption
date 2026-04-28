package ECC;

import ECC.ECPoint;
import java.math.BigInteger;

/**
 * Constants defining the secp256k1 elliptic curve - the same curve used by
 * Bitcoin and many other crypto systems. Equation: y² = x³ + 7 (mod p).
 */
class Curve {

    /** Field prime - all coordinate arithmetic is done modulo this value. */
    static final BigInteger P = new BigInteger(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16
    );

    // curve coefficients:  y² = x³ + ax + b
    static final BigInteger A = BigInteger.ZERO;
    static final BigInteger B = new BigInteger("7");

    // generator point G - every public key is some scalar multiple of this point
    static final BigInteger GX = new BigInteger(
            "79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16
    );
    static final BigInteger GY = new BigInteger(
            "483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16
    );

    static final ECPoint G = new ECPoint(GX, GY);

    /**
     * Order of G - the number of distinct points you can reach by repeatedly
     * adding G to itself before cycling back. Private keys must be in [1, N-1]
     * so they map to a non-identity public key.
     */
    static final BigInteger N = new BigInteger(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16
    );

    /**
     * Checks whether (x, y) is actually a point on the curve - i.e. whether
     * y² ≡ x³ + 7 (mod p). Used to reject malformed or attacker-crafted points
     * before doing any scalar multiplication on them.
     */
    static boolean isOnCurve(BigInteger x, BigInteger y) {
        BigInteger left  = y.modPow(BigInteger.TWO, P);
        BigInteger right = x.modPow(new BigInteger("3"), P)
                .add(B)
                .mod(P);
        return left.equals(right);
    }
}