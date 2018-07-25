package domain.entity;

/**
 *
 */
public class OutgoingMoneyTransfer {

    public final BankAccountNumber targetBankAccountNumber;

    public final Money moneyAmount;

    public OutgoingMoneyTransfer(BankAccountNumber targetBankAccountNumber, Money moneyAmount) {
        this.targetBankAccountNumber = targetBankAccountNumber;
        this.moneyAmount = moneyAmount;
    }

    @Override
    public String toString() {
        return "Outgoing transfer to: " + targetBankAccountNumber + ", amount: " + moneyAmount;
    }
}
