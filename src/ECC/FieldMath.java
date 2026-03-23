package ECC;

import java.math.BigInteger;

class FieldMath {

    // modular inverse using Fermat's little theorem
    // b^(p-2) mod p
    static BigInteger modInverse(BigInteger b) {
        return b.modPow(Curve.P.subtract(BigInteger.TWO), Curve.P);
    }

}
