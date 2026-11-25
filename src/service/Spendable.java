package service;

import java.math.BigDecimal;

public interface Spendable {
    BigDecimal getSpendableBalance();
    void validateSpend(BigDecimal amount);
}
