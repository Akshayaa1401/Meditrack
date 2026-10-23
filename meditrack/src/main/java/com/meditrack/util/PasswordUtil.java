package com.meditrack.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PasswordUtil
 * ------------
 * Provides salted SHA-256 password hashing.
 *
 * NOTE (production guidance): For a real production deployment, prefer a
 * dedicated password hashing algorithm such as bcrypt or Argon2 (e.g. via
 * the jBCrypt library) which include built-in work-factor / cost tuning
 * and are more resistant to brute-force attacks than a generic digest.
 * Salted SHA-256 is used here to keep the dependency footprint minimal for
 * this educational/reference build.
 */
public class PasswordUtil {

    private static final int SALT_LENGTH = 16;

    public static String hashPassword(String plainPassword) {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashed = md.digest(plainPassword.getBytes("UTF-8"));

            // store as salt:hash, both base64 encoded
            return Base64.getEncoder().encodeToString(salt) + ":" +
                    Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    public static boolean verifyPassword(String plainPassword, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 2) return false;

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] actualHash = md.digest(plainPassword.getBytes("UTF-8"));

            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (Exception e) {
            return false;
        }
    }
}
