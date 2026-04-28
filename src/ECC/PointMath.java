package ECC;

import java.math.BigInteger;

/**
 * Elliptic curve point arithmetic over secp256k1.
 * All operations are done mod P, using the standard chord-and-tangent rules.
 *
 * The three building blocks are:
 *   slope     - tangent (point doubling) or secant (point addition)
 *   pointAdd  - adds two distinct points
 *   pointDouble - doubles a point (adds it to itself)
 * These combine into scalarMultiply, which is the only public method and the
 * one everything else in the project actually calls.
 */
class PointMath {

    /** Secant slope between two distinct points: (y2-y1) / (x2-x1) mod p */
    private static BigInteger slope(ECPoint p1, ECPoint p2) {
        BigInteger x1 = p1.getX(),  y1 = p1.getY();
        BigInteger x2 = p2.getX(),  y2 = p2.getY();

        BigInteger numerator   = y2.subtract(y1).mod(Curve.P);
        BigInteger denominator = x2.subtract(x1).mod(Curve.P);
        return numerator.multiply(FieldMath.modInverse(denominator)).mod(Curve.P);
    }

    /** Tangent slope at a point (used when doubling): (3x² + a) / (2y) mod p */
    private static BigInteger slope(ECPoint p) {
        BigInteger x = p.getX(),  y = p.getY();

        // numerator = 3x² + a
        BigInteger numerator = x.pow(2)
                .multiply(new BigInteger("3"))
                .add(Curve.A)
                .mod(Curve.P);

        // denominator = 2y
        BigInteger denominator = y.multiply(BigInteger.TWO).mod(Curve.P);

        return numerator.multiply(FieldMath.modInverse(denominator)).mod(Curve.P);
    }

    /**
     * Adds two distinct curve points using the chord rule.
     * Returns INFINITY if the points are additive inverses (same x, opposite y).
     */
    private static ECPoint pointAdd(ECPoint p1, ECPoint p2) {
        if (p1.isInfinity()) return p2;
        if (p2.isInfinity()) return p1;

        BigInteger x1 = p1.getX(), y1 = p1.getY();
        BigInteger x2 = p2.getX(), y2 = p2.getY();

        // p1 and p2 are mirror images across the x-axis - their sum is the identity
        if (x1.equals(x2) && y1.equals(y2.negate().mod(Curve.P))) {
            return ECPoint.INFINITY;
        }

        BigInteger slope = slope(p1, p2);

        // x3 = slope² - x1 - x2  mod p
        BigInteger x3 = slope.pow(2).subtract(x1).subtract(x2).mod(Curve.P);

        // y3 = slope(x1 - x3) - y1  mod p
        BigInteger y3 = slope.multiply(x1.subtract(x3)).subtract(y1).mod(Curve.P);

        return new ECPoint(x3, y3);
    }

    /**
     * Doubles a point using the tangent rule.
     * Returns INFINITY if the tangent is vertical (y == 0), meaning the
     * point has no finite result when added to itself.
     */
    private static ECPoint pointDouble(ECPoint p) {
        if (p.isInfinity()) return ECPoint.INFINITY;

        BigInteger x = p.getX(), y = p.getY();

        // vertical tangent - doubling sends this to infinity
        if (y.equals(BigInteger.ZERO)) return ECPoint.INFINITY;

        BigInteger slope = slope(p);

        // x3 = slope² - 2x  mod p
        BigInteger x3 = slope.pow(2).subtract(x.multiply(BigInteger.TWO)).mod(Curve.P);

        // y3 = slope(x - x3) - y  mod p
        BigInteger y3 = slope.multiply(x.subtract(x3)).subtract(y).mod(Curve.P);

        return new ECPoint(x3, y3);
    }

    /**
     * Computes k*P using the double-and-add algorithm - the elliptic curve
     * equivalent of repeated addition, done in O(log k) steps.
     *
     * Works by scanning k bit by bit from LSB to MSB: if the current bit is 1,
     * add the current power of P into the result; then double P for the next bit.
     * This is the core operation behind both key generation (k*G) and ECDH
     * (privateKey * theirPublicKey).
     */
    static ECPoint scalarMultiply(ECPoint p, BigInteger k) {
        k = k.mod(Curve.N);

        ECPoint result  = ECPoint.INFINITY; // identity element, like starting at 0
        ECPoint current = p;

        while (k.signum() > 0) {
            if (k.testBit(0)) {              // if LSB is 1, include current power of P
                result = pointAdd(result, current);
            }
            current = pointDouble(current);  // advance to next power of P
            k = k.shiftRight(1);             // drop the bit we just processed
        }
        return result;
    }
}