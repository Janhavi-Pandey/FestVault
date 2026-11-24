package model;

import java.math.BigDecimal;

public class EventSubAccount extends ClubAccount {
    public EventSubAccount(String accountNo, String clubId, String accountName, BigDecimal balance) {
        super(accountNo, clubId, accountName, balance);
    }

    @Override
    public String getAccountType() {
        return "EVENT";
    }
}
