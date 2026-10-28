package com.campuslab.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** PBKDF2 password hashing using only the JDK. */
public final class PasswordUtil {
    private static final int ITERATIONS = 160_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}

    public record PasswordRecord(String salt, String hash) {}

    public static PasswordRecord hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = derive(password.toCharArray(), salt, ITERATIONS);
        return new PasswordRecord(Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(key));
    }

    public static boolean verify(String password, String encodedSalt, String expectedHash) {
        try {
            byte[] salt = Base64.getDecoder().decode(encodedSalt);
            byte[] expected = Base64.getDecoder().decode(expectedHash);
            byte[] actual = derive(password.toCharArray(), salt, ITERATIONS);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("Secure password hashing is unavailable in this Java runtime.", ex);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(password, '\0');
        }
    }
}
