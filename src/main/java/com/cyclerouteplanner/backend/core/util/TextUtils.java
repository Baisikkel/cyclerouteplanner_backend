package com.cyclerouteplanner.backend.core.util;

public final class TextUtils {

    private TextUtils() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
