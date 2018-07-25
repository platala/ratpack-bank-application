package domain.entity;

/**
 *
 */
public class MoneyTransferedEvent {
    public final MoneyTransfer moneyTransfer;

    public MoneyTransferedEvent(MoneyTransfer moneyTransfer) {
        this.moneyTransfer = moneyTransfer;
    }
}
