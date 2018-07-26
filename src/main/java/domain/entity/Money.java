package domain.entity;

import java.util.Currency;
import java.util.Objects;

/**
 *
 */
public class Money {

    public static final Currency polishCurrency = Currency.getInstance("PLN");
    public static final Money ZERO_PLN = polish(0);

    public static Money polish(int amount) {
        return new Money(polishCurrency, amount);
    }

    public final Currency currency;
    public final int amount;

    public Money(Currency currency, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("only positive money amount supported");
        }
        this.currency = currency;
        this.amount = amount;
    }

    public boolean sameCurrency(Money other) {
        return currency.equals(other.currency);
    }

    public Money add(Money other) {
        if (!sameCurrency(other)) {
            throw new IllegalArgumentException("Cannot add Money with different Currency");
        }
        return new Money(currency, amount + other.amount);
    }

    public Money substract(Money other) {
        if (!sameCurrency(other)) {
            throw new IllegalArgumentException("Cannot substract Money with different Currency");
        }
        return new Money(currency, amount - other.amount);
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount == money.amount &&
            Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, amount);
    }
}
