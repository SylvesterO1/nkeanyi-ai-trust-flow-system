package com.nkeanyi.payment.security;

public final class DataMaskingUtil {

    private DataMaskingUtil() {
    }

    public static String maskPan(String value) {
        if (value == null || value.length() < 10) {
            return "****";
        }
        return value.substring(0, 6) + "******" + value.substring(value.length() - 4);
    }

    public static String maskAccount(String value) {
        if (value == null || value.length() < 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }

    public static String maskEmail(String value) {
        if (value == null || value.isBlank() || !value.contains("@")) {
            return "****";
        }
        int atIndex = value.indexOf("@");
        if (atIndex <= 1) {
            return "****" + value.substring(atIndex);
        }
        return value.substring(0, 1) + "****" + value.substring(atIndex);
    }
}
