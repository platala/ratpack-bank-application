package domain.control;

import domain.entity.BankAccountEntity;
import domain.entity.BankAccountNumber;

import java.util.ConcurrentModificationException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class BankAccountRepository {

    private final AtomicInteger bankAccountNumberGenerator = new AtomicInteger();

    private final ConcurrentHashMap<BankAccountNumber, BankAccountEntity> bankAccounts = new ConcurrentHashMap<>();

    public BankAccountNumber generateNewBankAccountNumber() {
        return new BankAccountNumber(BankAccountNumber.revolutAccountPrefix + bankAccountNumberGenerator.incrementAndGet());
    }

    public BankAccountEntity getBankAccount(BankAccountNumber bankAccountNumber) {
        BankAccountEntity bankAccount = bankAccounts.get(bankAccountNumber);
        if (bankAccount == null) {
            throw new IllegalArgumentException("Bank account: " + bankAccountNumber + " not found");
        }

        return bankAccount.copy();
    }

    public void saveBankAccount(BankAccountEntity bankAccount) throws ConcurrentModificationException {
        BankAccountEntity previous = bankAccounts.putIfAbsent(bankAccount.bankAccountNumber, bankAccount);
        if (previous == null) {
            return;
        }

        BankAccountEntity newBankAccount = bankAccount.newRevisionCopy(UUID.randomUUID());
        boolean success = bankAccounts.replace(bankAccount.bankAccountNumber, bankAccount, newBankAccount);
        if (!success) {
            throw new ConcurrentModificationException();
        }
    }

}
