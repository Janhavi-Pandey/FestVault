package util;

import java.math.BigDecimal;

public class InputValidator {
    private InputValidator() {}

    public static boolean isValidPin(String pin) {
        if (pin == null) return false;
        return pin.trim().matches("\\d{4}");
    }

    public static boolean isValidBankRef(String ref) {
        if (ref == null) return false;
        String cleaned = ref.trim();
        return !cleaned.isEmpty() && cleaned.matches("[A-Za-z0-9_-]{4,40}");
    }

    public static boolean isPositiveAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
