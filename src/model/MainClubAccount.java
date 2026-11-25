package model;

import java.math.BigDecimal;

public class MainClubAccount extends ClubAccount {
    public MainClubAccount(String accountNo, String clubId, String accountName, BigDecimal balance) {
        super(accountNo, clubId, accountName, balance);
    }
    @Override public String getAccountType() { return "MAIN"; }
}
