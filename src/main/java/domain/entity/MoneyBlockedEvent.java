package domain.entity;

/**
 *
 */
public class MoneyBlockedEvent {
    public final MoneyTransfer moneyTransfer;

    public MoneyBlockedEvent(MoneyTransfer moneyTransfer) {
        this.moneyTransfer = moneyTransfer;
    }
}
