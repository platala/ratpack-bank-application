package domain.entity;

import java.util.Objects;

/**
 *
 */
public class BankAccountNumber {

    public static final String revolutAccountPrefix = "REV";


    private final String accountNumber;

    public BankAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public boolean isMyBank() {
        return accountNumber.startsWith(revolutAccountPrefix);
    }

    @Override
    public String toString() {
        return accountNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankAccountNumber that = (BankAccountNumber) o;
        return Objects.equals(accountNumber, that.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }
}
