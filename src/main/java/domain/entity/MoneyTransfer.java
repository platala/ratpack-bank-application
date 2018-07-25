package domain.entity;

import java.util.UUID;

/**
 *
 */
public class MoneyTransfer {

    public final UUID transferId;
    public final BankAccountNumber accountNumber;
    public final BankAccountNumber targetBankAccountNumber;
    public final Money money;

    public MoneyTransfer(UUID transferId, BankAccountNumber accountNumber, BankAccountNumber targetBankAccountNumber, Money money) {
        if (accountNumber.equals(targetBankAccountNumber)) {
            throw new IllegalArgumentException("Cannot make transfer between same account");
        }
        this.transferId = transferId;
        this.accountNumber = accountNumber;
        this.targetBankAccountNumber = targetBankAccountNumber;
        this.money = money;
    }
}
