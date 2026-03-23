package ECC;

import ECC.ECPoint;
import java.math.BigInteger;

class Curve {

    // field prime
    static final BigInteger P = new BigInteger(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16
    );

    // curve coefficients:  y² = x³ + ax + b
    static final BigInteger A = BigInteger.ZERO;  // 0
    static final BigInteger B = new BigInteger("7");

    // generator point G
    static final BigInteger GX = new BigInteger(
            "79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16
    );
    static final BigInteger GY = new BigInteger(
            "483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16
    );

    static final ECPoint G = new ECPoint(GX, GY);


            // order n — private keys must be in [1, N-1]
    static final BigInteger N = new BigInteger(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16
    );

    // verify a point (x, y) lies on the curve
    // checks:  y² ≡ x³ + 7  (mod p)
    static boolean isOnCurve(BigInteger x, BigInteger y) {
        BigInteger left  = y.modPow(BigInteger.TWO, P);
        BigInteger right = x.modPow(new BigInteger("3"), P)
                .add(B)
                .mod(P);
        return left.equals(right);
    }
}