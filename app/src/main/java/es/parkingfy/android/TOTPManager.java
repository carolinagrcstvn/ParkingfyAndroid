package es.parkingfy.android;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

public class TOTPManager {
    static public long getTOTP (String encodedSecret) {
        TOTP totpInstance = TOTP.getInstance(encodedSecret);

        final long time = (System.currentTimeMillis() / 1000) / 30;

        return totpInstance.generateTOTP(time, 8);
    }
}

class TOTP {

    private final int window;
    private final byte[] secret;
    private final String secretEncoded;

    private static final int secret_size_bits = 160;

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final String HMAC_PROVIDER = "SunJCE";

    private TOTP(
            final int window,
            final byte[] secret,
            final String secretEncoded
    ) {
        this.window = window;
        this.secret = secret;
        this.secretEncoded = secretEncoded;
    }

    public static final TOTP getInstance(
            final int window,
            final String encodedSecret
    )
            throws IllegalWindowSizeException
    {
        if (window >= 1 && window <= 10) {

            Base32 base32 = new Base32();
            final byte[] secret = base32.decode(encodedSecret);
            return new TOTP(window, secret, encodedSecret);

        } else {
            throw new IllegalWindowSizeException("Window size provided not allowed: " + window);
        }
    }

    public static final TOTP getInstance(
            final String encodedSecret
    ) {
        Base32 base32 = new Base32();
        final byte[] secret = base32.decode(encodedSecret);
        return new TOTP(3, secret, encodedSecret);
    }

    public static final String generateSecret() {
        Base32 base32 = new Base32();
        SecureRandom random = null;
        final byte[] secret = new byte[secret_size_bits / 8];

        try {
            random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        } catch (
                NoSuchAlgorithmException |
                        NoSuchProviderException e
        ) {
            return null;
        }

        random.nextBytes(secret);

        return new String (base32.encode(secret));
    }

    public final String getQRCodeURL(
            String issuer,
            String user
    ) {
        if (issuer.contains(":") || user.contains(":")) {
            System.err.println("Issuer name or username contains an illegal character.");
            return null;
        }

        //Remove all whitespace
        issuer = issuer.trim().replaceAll("\\s", "");
        user = user.trim().replaceAll("\\s", "");

        final String url_format = "https://chart.googleapis.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=otpauth://totp/%s:%s%%3Fsecret=%s";
        return String.format(url_format, issuer, user, secretEncoded);
    }

    public final boolean verifyCode(
            final long code
    ) {
        final long time = (System.currentTimeMillis() / 1000) / 30;

        for (int i = -window; i <= window; ++i) {
            if (generateTOTP(time, i) == code) {
                return true;
            }
        }

        return false;
    }

    final long generateTOTP(
            final long time,
            final long count
    ) {
        long truncatedResult = 0L;
        long val = time + count;
        final byte[] buf = new byte[8];

        for (int i = 8; i-- > 0; val >>>= 8) {
            buf[i] = (byte) val;
        }

        final byte[] result = hmac(buf);

        final int offset = result[result.length - 1] & 0xF;

        for (int i = 0; i < 4; ++i) {
            truncatedResult = (truncatedResult << 8) | (result[offset + i] & 0xFF);
        }

        return ((truncatedResult & 0x7FFFFFFF) % 1000000);
    }

    private final byte[] hmac(
            final byte[] data
    ) {
        final Mac mac;

        try {
            mac = Mac.getInstance(HMAC_ALGORITHM, HMAC_PROVIDER);
        } catch (
                NoSuchAlgorithmException |
                        NoSuchProviderException e
        ) {
            return null;
        }

        try {
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
        } catch (
                InvalidKeyException e
        ) {
            return null;
        }

        return mac.doFinal(data);
    }


}

class IllegalWindowSizeException extends Exception {

    private static final long serialVersionUID = 1L;

    public IllegalWindowSizeException(
            final String message
    ) {
        super(message);
    }
}
