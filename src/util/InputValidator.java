package util;

import java.math.BigDecimal;

public final class InputValidator {
    private InputValidator() { }

    public static boolean isValidClubId(String value) {
        return value != null && value.trim().matches("[A-Za-z0-9_-]{2,20}");
    }
    public static boolean isValidUsername(String value) {
        return value != null && value.trim().matches("[A-Za-z0-9._-]{3,50}");
    }
    public static boolean isValidPin(String value) {
        return value != null && value.trim().matches(".{4,100}");
    }
    public static boolean isValidBankRef(String value) {
        return value != null && value.trim().matches("[A-Za-z0-9_-]{4,80}");
    }
    public static boolean isPositiveAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
    public static boolean isValidText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
