import domain.boundary.SimpleBankService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import domain.entity.BankAccountEntity;
import domain.entity.BankAccountNumber;
import domain.entity.Money;
import domain.entity.MoneyTransfer;
import ratpack.error.ServerErrorHandler;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.jackson.Jackson;
import ratpack.server.RatpackServer;

import java.time.Duration;
import java.util.ConcurrentModificationException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 *
 */
public class RatpackBankApplication {

    SimpleBankService simpleBankService = new SimpleBankService();

    {
        simpleBankService.newBankAccount(Money.polish(1000));
        simpleBankService.newBankAccount(Money.polish(1000));
    }

    Action<Chain> chain() {
        return chain -> chain
            .register(r -> r.add(ServerErrorHandler.class, (ctx, throwable) -> ctx.getResponse().status(400).send(throwable.getMessage())))
            .put("admin/suspension", this::suspendEventSending)
            .get("accounts/:accountNumber", this::accountDetails)
            .get("accounts/:accountNumber/transfers", this::accountTransfers)
            .put("accounts/:accountNumber/transfers/:transferId", this::moneyTransfer)
            ;
    }

    void suspendEventSending(Context ctx) {
        Promise.value(Boolean.parseBoolean(ctx.getRequest().getQueryParams().getOrDefault("suspend", "false")))
            .blockingOp(simpleBankService::suspendEventSending).map(x -> "SUSPEND: " + x).then(ctx::render);
    }

    void accountDetails(Context ctx) {
        Promise.value(accountNumber(ctx))
            .blockingMap(simpleBankService::bankAccount)
            .map(bankAccount -> Jackson.json(JsonNodeFactory.instance.objectNode()
                .put("balance", bankAccount.getAccountBalance().toString())
                .put("blockedMoney", bankAccount.getBlockedMoney().toString())
                ))
            .then(ctx::render);
    }

    void accountTransfers(Context ctx) {
        Promise.value(accountNumber(ctx))
            .blockingMap(simpleBankService::bankAccount)
            .map(BankAccountEntity::getPendingTransfers)
            .map(pendingTransfers -> Jackson.json(JsonNodeFactory.instance.arrayNode().addAll(
                pendingTransfers.stream().map(
                    transfer -> JsonNodeFactory.instance.objectNode()
                        .put("transferId", transfer.transferId.toString())
                        .put("blockedMoney", transfer.money.toString()))
                    .collect(Collectors.toList()))))
            .then(ctx::render);
    }

    void moneyTransfer(Context ctx) {
        UUID transferId = UUID.fromString(ctx.getPathTokens().get("transferId"));
        BankAccountNumber accountNumber = accountNumber(ctx);

        ctx.parse(Jackson.jsonNode())
            .map(jsonNode -> new MoneyTransfer(transferId, accountNumber,
                new BankAccountNumber(jsonNode.get("targetAccount").asText()),
                Money.polish(jsonNode.get("money").asInt())))
            .fork()
            .blockingMap(simpleBankService::startMoneyTransfer)
            .onError(e -> !(e instanceof ConcurrentModificationException), e -> ctx.error(e))
            .retry(10, Duration.ZERO, BiAction.noop())
            .then(moneyBlockedEvent -> ctx.render("transfer from " + moneyBlockedEvent.moneyTransfer.accountNumber + " money: " + moneyBlockedEvent.moneyTransfer.money));
    }

    private BankAccountNumber accountNumber(Context ctx) {
        return new BankAccountNumber(ctx.getPathTokens().get("accountNumber"));
    }


    public static void main(String[] args) throws Exception {
        RatpackBankApplication application = new RatpackBankApplication();
        RatpackServer.start(server -> server
            .handlers(application.chain())
        );
    }

}
