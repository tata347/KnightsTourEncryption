package ECC;

import java.math.BigInteger;

public class Curve {

    // field prime
    public static final BigInteger P = new BigInteger(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16
    );

    // curve coefficients:  y² = x³ + ax + b
    public static final BigInteger A = BigInteger.ZERO;  // 0
    public static final BigInteger B = new BigInteger("7");

    // generator point G
    public static final BigInteger GX = new BigInteger(
            "79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16
    );
    public static final BigInteger GY = new BigInteger(
            "483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16
    );

    // order n — private keys must be in [1, N-1]
    public static final BigInteger N = new BigInteger(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16
    );

    // verify a point (x, y) lies on the curve
    // checks:  y² ≡ x³ + 7  (mod p)
    public static boolean isOnCurve(BigInteger x, BigInteger y) {
        BigInteger left  = y.modPow(BigInteger.TWO, P);
        BigInteger right = x.modPow(new BigInteger("3"), P)
                .add(B)
                .mod(P);
        return left.equals(right);
    }
}