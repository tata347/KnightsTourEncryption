package ECC;

import java.math.BigInteger;

public class FieldMath {

    // modular inverse using Fermat's little theorem
    // b^(p-2) mod p
    public static BigInteger modInverse(BigInteger b) {
        return b.modPow(Curve.P.subtract(BigInteger.TWO), Curve.P);
    }

    // keep result inside the field
    public static BigInteger mod(BigInteger n) {
        return n.mod(Curve.P);
    }

    // modular addition
    public static BigInteger add(BigInteger a, BigInteger b) {
        return a.add(b).mod(Curve.P);
    }

    // modular subtraction
    public static BigInteger subtract(BigInteger a, BigInteger b) {
        return a.subtract(b).mod(Curve.P);
    }

    // modular multiplication
    public static BigInteger multiply(BigInteger a, BigInteger b) {
        return a.multiply(b).mod(Curve.P);
    }
}
