package domain.entity;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregate Entity that holds infromation about Bank Account - mainly it's balance and pending money transfers that block part of money.
 */
public class BankAccountEntity {

    private final UUID revisionId;

    public final BankAccountNumber bankAccountNumber;

    private Money accountBalance;

    private final Map<UUID, OutgoingMoneyTransfer> outgoingMoneyTransfers = new HashMap<>();

    private BloomFilter<String> previousTransfers = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 1000);

    public BankAccountEntity(UUID revisionId, BankAccountNumber bankAccountNumber, Currency accountCurrency) {
        this.revisionId = revisionId;
        this.bankAccountNumber = bankAccountNumber;
        this.accountBalance = new Money(accountCurrency, 0);
    }

    private void checkCurrency(Money money) {
        if (!accountBalance.sameCurrency(money)) {
            throw new IllegalArgumentException("Money exchange not supported");
        }
    }

    public Money getAccountBalance() {
        return accountBalance;
    }

    public Money getAvailableMoney() {
        return accountBalance.substract(getBlockedMoney());
    }

    public Money getBlockedMoney() {
        int blockedSum = outgoingMoneyTransfers.values().stream()
            .mapToInt(transfer -> transfer.moneyAmount.amount).sum();
        return new Money(getAccountCurrency(), blockedSum);
    }

    public Currency getAccountCurrency() {
        return accountBalance.currency;
    }


    public void startMoneyTransfer(UUID transferId, BankAccountNumber targetBankAccountNumber, Money moneyAmount) {
        checkCurrency(moneyAmount);
        if (previousTransfers.mightContain(transferId.toString())) {
            throw new IllegalArgumentException("Transfer already processed. " + transferId);
        }

        if (getAvailableMoney().amount < moneyAmount.amount) {
            throw new IllegalArgumentException("Not enough money to make transfer. " + bankAccountNumber);
        }

        OutgoingMoneyTransfer outgoingMoneyTransfer = new OutgoingMoneyTransfer(targetBankAccountNumber, moneyAmount);
        outgoingMoneyTransfers.put(transferId, outgoingMoneyTransfer);
    }

    public void confirmMoneyTransfer(UUID transferId) {
        OutgoingMoneyTransfer outgoingMoneyTransfer = outgoingMoneyTransfers.remove(transferId);
        if (outgoingMoneyTransfer == null) {
            // we assume it was already confirmed
            return;
        }

        previousTransfers.put(transferId.toString());
        accountBalance = accountBalance.substract(outgoingMoneyTransfer.moneyAmount);
    }

    public void receiveMoneyTransfer(Money receivedMoney) {
        checkCurrency(receivedMoney);
        accountBalance = accountBalance.add(receivedMoney);
    }

    public List<MoneyTransfer> getPendingTransfers() {
        return outgoingMoneyTransfers.entrySet().stream().map(entry ->
            new MoneyTransfer(entry.getKey(), bankAccountNumber, entry.getValue().targetBankAccountNumber, entry.getValue().moneyAmount))
            .collect(Collectors.toList());
    }


    public BankAccountEntity copy() {
        return newRevisionCopy(revisionId);
    }

    public BankAccountEntity newRevisionCopy(UUID newRevisionId) {
        BankAccountEntity bankAccount = new BankAccountEntity(newRevisionId, bankAccountNumber, getAccountCurrency());
        bankAccount.accountBalance = accountBalance;
        bankAccount.outgoingMoneyTransfers.putAll(outgoingMoneyTransfers);
        bankAccount.previousTransfers = previousTransfers.copy();
        return bankAccount;
    }


    @Override
    public String toString() {
        return bankAccountNumber + " balance: " + getAccountBalance() + " blockades: " + getBlockedMoney();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankAccountEntity that = (BankAccountEntity) o;
        return Objects.equals(revisionId, that.revisionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revisionId);
    }
}
