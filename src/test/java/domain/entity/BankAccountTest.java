package domain.entity;

import org.junit.Assert;
import org.junit.Test;

import java.util.Currency;
import java.util.UUID;

/**
 *
 */
public class BankAccountTest {

    static BankAccountNumber bankAccountNumber1 = new BankAccountNumber("1");
    static BankAccountNumber bankAccountNumber2 = new BankAccountNumber("2");

    @Test
    public void testBankAccountCreation() {
        BankAccountEntity bankAccount = createPolishEmptyBankAccount();

        Assert.assertEquals(bankAccount.getAccountCurrency(), Currency.getInstance("PLN"));
        Assert.assertEquals(bankAccount.getAccountBalance(), Money.ZERO_PLN);
        Assert.assertEquals(bankAccount.getBlockedMoney(), Money.ZERO_PLN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBankAccountAccountBalanceValidationNoFunds() {
        BankAccountEntity bankAccount = createPolishEmptyBankAccount();
        bankAccount.startMoneyTransfer(UUID.randomUUID(), bankAccountNumber2, Money.polish(100));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBankAccountAccountBalanceValidationWithFunds() {
        BankAccountEntity bankAccount = createPolishEmptyBankAccount();
        bankAccount.receiveMoneyTransfer(Money.polish(100));
        bankAccount.startMoneyTransfer(UUID.randomUUID(), bankAccountNumber2, Money.polish(101));
    }

    @Test
    public void testBankAccountAccountWillBeZero() {
        BankAccountEntity bankAccount = createPolishEmptyBankAccount();
        bankAccount.receiveMoneyTransfer(Money.polish(100));
        UUID transferId = UUID.randomUUID();
        bankAccount.startMoneyTransfer(transferId, bankAccountNumber2, Money.polish(100));
        Assert.assertEquals(bankAccount.getAvailableMoney(), Money.ZERO_PLN);
        Assert.assertEquals(bankAccount.getBlockedMoney(), Money.polish(100));

        bankAccount.confirmMoneyTransfer(transferId);
        Assert.assertEquals(bankAccount.getAccountBalance(), Money.ZERO_PLN);
    }

    private BankAccountEntity createPolishEmptyBankAccount() {
        return new BankAccountEntity(UUID.randomUUID(), bankAccountNumber1, Money.polishCurrency);
    }

}