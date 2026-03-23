package ECC;

import java.math.BigInteger;

class PointMath {

    private static BigInteger slope(ECPoint p1, ECPoint p2) {
        BigInteger x1 = p1.getX(),  y1 = p1.getY();
        BigInteger x2 = p2.getX(),  y2 = p2.getY();

        // slope = (y2-y1) / (x2-x1)  mod p
        BigInteger numerator = y2.subtract(y1).mod(Curve.P);
        BigInteger denominator = x2.subtract(x1).mod(Curve.P);
        BigInteger slope = numerator.multiply(
                FieldMath.modInverse(denominator)
        ).mod(Curve.P);

        return slope;
    }

    private static BigInteger slope(ECPoint p) {

        BigInteger x = p.getX(),  y = p.getY();

        // numerator = 3x^2 + a
        BigInteger numerator = x.pow(2)
                .multiply(new BigInteger("3"))
                .add(Curve.A)
                .mod(Curve.P);

        // denominator = 2y
        BigInteger denominator = y
                .multiply(BigInteger.TWO)
                .mod(Curve.P);

        // slope = numerator / denominator  mod p
        return numerator.multiply(FieldMath.modInverse(denominator))
                .mod(Curve.P);
    }

    private static ECPoint pointAdd(ECPoint p1, ECPoint p2) {
        if (p1.isInfinity()) return p2;
        if (p2.isInfinity()) return p1;

        BigInteger x1 = p1.getX(), y1 = p1.getY();
        BigInteger x2 = p2.getX(), y2 = p2.getY();

        if (x1.equals(x2) && y1.equals(y2.negate().mod(Curve.P))) {
            return ECPoint.INFINITY;
        }

        // get slope
        BigInteger slope = slope(p1, p2);

        // x3 = slope^2 - x1 - x2  mod p
        BigInteger x3 = slope.pow(2)
                .subtract(x1)
                .subtract(x2)
                .mod(Curve.P);

        // y3 = slope(x1-x3) - y1  mod p
        BigInteger y3 = slope.multiply(x1.subtract(x3))
                .subtract(y1)
                .mod(Curve.P);

        return new ECPoint(x3, y3);
    }

    private static ECPoint pointDouble(ECPoint p) {

        if(p.isInfinity()){
            return ECPoint.INFINITY;
        }

        BigInteger x = p.getX(), y = p.getY();

        if(y.equals(BigInteger.ZERO)){
            return ECPoint.INFINITY;
        }

        //get slope
        BigInteger slope = slope(p);
        // x3 = slope^2 - 2x
        BigInteger x3 = slope.pow(2)
                             .subtract(x.multiply(BigInteger.TWO))
                             .mod(Curve.P);

        //y3
        BigInteger y3 = slope.multiply(x.subtract(x3))
                             .subtract(y)
                             .mod(Curve.P);

        return new ECPoint(x3, y3);
    }

    static ECPoint scalarMultiply(ECPoint p, BigInteger k) {

        k = k.mod(Curve.N);
        //like starting a sum at 0, it will double
        ECPoint result  = ECPoint.INFINITY;
        ECPoint current = p;

        //while k is bigger than zero
        while (k.signum() > 0) {
            //check least significant bit
            // if its 1 add current p, if not skip
            if (k.testBit(0)) {
                result = pointAdd(result, current);
            }
            //double p for the next point
            current = pointDouble(current);
            //shift k right, drop processed bit
            k = k.shiftRight(1);
        }
        return result;
    }

}
