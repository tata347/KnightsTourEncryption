package ECC;

import java.math.BigInteger;

public class ECPoint {

    private final BigInteger x;
    private final BigInteger y;

    public static final ECPoint INFINITY = new ECPoint(null, null);

    public ECPoint(BigInteger x, BigInteger y) {
        this.x = x;
        this.y = y;

        // only validate real points, not infinity
        if (x != null && y != null) {
            if (!Curve.isOnCurve(x, y)) {
                throw new IllegalArgumentException("Point is not on curve");
            }
        }

    }

    public BigInteger getX() {
        return x;
    }

    public BigInteger getY() {
        return y;
    }

    public boolean isInfinity() {
        return x == null && y == null;
    }

    @Override
    public String toString() {
        if (isInfinity()) return "Point at Infinity";
        return "ECPoint(" + x.toString(16) + ", " + y.toString(16) + ")";
    }
}
