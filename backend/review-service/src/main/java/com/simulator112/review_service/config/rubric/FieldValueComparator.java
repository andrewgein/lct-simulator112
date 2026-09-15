package com.simulator112.review_service.config.rubric;

import java.util.Locale;

public final class FieldValueComparator {

    private FieldValueComparator() {
    }

    public static boolean hasSameContent(String expected, String actual) {

        return normalize(expected).equals(normalize(actual));
    }

    private static String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value.replaceAll("[\\s\\p{Z}]+", "")
                    .toLowerCase(Locale.ROOT);
    }
}
