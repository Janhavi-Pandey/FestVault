package service;

import exception.UnauthorizedTransferException;
import model.AccountStatus;
import model.ClubAccount;
import model.EventSubAccount;
import model.MainClubAccount;
import model.TransactionRecord;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class InMemoryLedgerStore {
    private final Map<String, ClubAccount> accountsByNo = new HashMap<>();
    private final Set<String> usedBankRefs = new HashSet<>();
    private final Set<String> uniqueSponsors = new HashSet<>();
    private final List<TransactionRecord> transactions = new ArrayList<>();

    public void registerAccount(ClubAccount account) {
        accountsByNo.put(account.getAccountNo(), account);
    }

    public ClubAccount getAccount(String accountNo) {
        return accountsByNo.get(accountNo);
    }

    public boolean registerBankRef(String bankRef, String sponsorName) {
        if (!usedBankRefs.add(bankRef)) {
            return false;
        }
        if (sponsorName != null && !sponsorName.isBlank()) {
            uniqueSponsors.add(sponsorName.trim());
        }
        return true;
    }

    public void addTransaction(TransactionRecord record) {
        transactions.add(record);
    }

    public List<TransactionRecord> getTopExpensesDescending() {
        List<TransactionRecord> copy = new ArrayList<>(transactions);
        copy.sort(Comparator.comparing(TransactionRecord::getAmount).reversed());
        return copy;
    }

    public Map<String, BigDecimal> getMonthlySpendTotals() {
        Map<String, BigDecimal> monthlyTotals = new TreeMap<>();
        DateTimeFormatter ym = DateTimeFormatter.ofPattern("yyyy-MM");
        for (TransactionRecord t : transactions) {
            if ("SUCCESS".equalsIgnoreCase(t.getStatus()) && "VENDOR_EXPENSE".equalsIgnoreCase(t.getTxnType())) {
                String key = t.getTxnDate().format(ym);
                monthlyTotals.put(key, monthlyTotals.getOrDefault(key, BigDecimal.ZERO).add(t.getAmount()));
            }
        }
        return monthlyTotals;
    }

    public Set<String> getUniqueSponsors() {
        return Set.copyOf(uniqueSponsors);
    }

    public void validateOwnClubTransfer(ClubAccount source, ClubAccount target) {
        if (!(source instanceof MainClubAccount) || !(target instanceof EventSubAccount)) {
            throw new UnauthorizedTransferException("Transfers must be from a MAIN club account to an EVENT sub-account.");
        }
        if (!source.getClubId().equals(target.getClubId())) {
            throw new UnauthorizedTransferException(
                    "Cross-club transfer blocked: " + source.getAccountNo() + " (" + source.getClubId() +
                    ") cannot transfer to " + target.getAccountNo() + " (" + target.getClubId() + ")."
            );
        }
        if (source.getStatus() != AccountStatus.ACTIVE || target.getStatus() != AccountStatus.ACTIVE) {
            throw new UnauthorizedTransferException("Both source and target accounts must be ACTIVE for transfer.");
        }
    }
}
