package domain.boundary;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import domain.control.BankAccountRepository;
import domain.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ConcurrentModificationException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Handles all operations on Bank Accounts - both coming from API and based on Domain Events.
 */
public class SimpleBankService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final BankAccountRepository bankAccountRepository = new BankAccountRepository();

    private final EventBus eventBus = new AsyncEventBus(Executors.newCachedThreadPool());

    private volatile boolean isSuspended = false;
    private final Queue<Object> pendingEvents = new ConcurrentLinkedQueue<>();

    private final Queue<Object> failedEvents = new ConcurrentLinkedQueue<>();

    public SimpleBankService() {
        eventBus.register(this);
    }

    public BankAccountEntity bankAccount(BankAccountNumber bankAccountNumber) {
        return bankAccountRepository.getBankAccount(bankAccountNumber);
    }

    public BankAccountNumber newBankAccount(Money initialMoney) {
        BankAccountNumber bankAccountNumber = bankAccountRepository.generateNewBankAccountNumber();
        BankAccountEntity bankAccount = new BankAccountEntity(UUID.randomUUID(), bankAccountNumber, initialMoney.currency);
        bankAccount.receiveMoneyTransfer(initialMoney);
        bankAccountRepository.saveBankAccount(bankAccount);

        return bankAccountNumber;
    }

    public MoneyBlockedEvent startMoneyTransfer(MoneyTransfer moneyTransfer) {
        BankAccountEntity bankAccount = bankAccountRepository.getBankAccount(moneyTransfer.accountNumber);
        bankAccount.startMoneyTransfer(moneyTransfer.transferId, moneyTransfer.targetBankAccountNumber, moneyTransfer.money);
        bankAccountRepository.saveBankAccount(bankAccount);

        MoneyBlockedEvent moneyBlockedEvent = new MoneyBlockedEvent(moneyTransfer);
        publishEventOrQueue(moneyBlockedEvent);

        return moneyBlockedEvent;
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleMoneyBlockedEvent(MoneyBlockedEvent moneyBlockedEvent) {
        MoneyTransferedEvent moneyTransferedEvent = new MoneyTransferedEvent(moneyBlockedEvent.moneyTransfer);

        if (moneyBlockedEvent.moneyTransfer.targetBankAccountNumber.isMyBank()) {
            operationOnBankAccountWithRetriesOnConcurrentModification(
                moneyBlockedEvent.moneyTransfer.targetBankAccountNumber,
                bankAccount -> bankAccount.receiveMoneyTransfer(moneyBlockedEvent.moneyTransfer.money),
                moneyTransferedEvent);
        } else {
            // TODO: external bank - I assume it succeeds
            publishEventOrQueue(moneyTransferedEvent);
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleMoneyTransferedEvent(MoneyTransferedEvent moneyTransferedEvent) {
        operationOnBankAccountWithRetriesOnConcurrentModification(
            moneyTransferedEvent.moneyTransfer.accountNumber,
            bankAccount -> bankAccount.confirmMoneyTransfer(moneyTransferedEvent.moneyTransfer.transferId),
            moneyTransferedEvent.moneyTransfer);
    }


    void operationOnBankAccountWithRetriesOnConcurrentModification(BankAccountNumber accountNumber,
                                                                   Consumer<BankAccountEntity> operationOnBankAccount,
                                                                   Object event) {
        int retryCount = 10;
        while(true) {
            try {
                operationOnBankAccount(accountNumber, operationOnBankAccount, event);
                return;
            } catch (ConcurrentModificationException ex) {
                logger.debug("Retrying operation, retry count: " + retryCount);
                if (retryCount-- == 0) {
                    throw new RuntimeException("10 retries failed", ex);
                }
            } catch (RuntimeException e) {
                logger.info("Failure in handling MoneyBlockedEvent event", e);
                failedEvents.add(event);
                return;
            }
        }
    }

    private void operationOnBankAccount(BankAccountNumber accountNumber, Consumer<BankAccountEntity> operationOnBankAccount, Object event) {
        BankAccountEntity bankAccount = bankAccountRepository.getBankAccount(accountNumber);
        operationOnBankAccount.accept(bankAccount);
        bankAccountRepository.saveBankAccount(bankAccount);

        publishEventOrQueue(event);
    }


    public void suspendEventSending(boolean isSuspended) {
        this.isSuspended = isSuspended;
        while (!isSuspended && !pendingEvents.isEmpty()) {
            eventBus.post(pendingEvents.remove());
        }
    }

    private void publishEventOrQueue(Object event) {
        if (isSuspended) {
            pendingEvents.add(event);
        } else {
            eventBus.post(event);
        }
    }
}
