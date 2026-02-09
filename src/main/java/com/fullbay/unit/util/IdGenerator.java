package com.fullbay.unit.util;

import java.security.SecureRandom;

/** Utility for generating Unit IDs. */
public class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARSET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int ID_LENGTH = 7;
    private static final String PREFIX = "unt_";

    /**
     * Generate a unique Unit ID.
     *
     * @return Unit ID in format "unt_{7 random alphanumeric lowercase}"
     */
    public static String generateUnitId() {
        final StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < ID_LENGTH; i++) {
            sb.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }
}
