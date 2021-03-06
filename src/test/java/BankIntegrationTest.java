import org.assertj.core.api.Assertions;
import org.junit.Test;
import ratpack.http.client.ReceivedResponse;
import ratpack.server.RatpackServer;
import ratpack.test.embed.EmbeddedApp;

import java.util.UUID;

/**
 *
 */
public class BankIntegrationTest {

    private final RatpackBankApplication application = new RatpackBankApplication();

    @Test
    public void simpleTest() throws Exception {
        application.createTwoBankAccounts();

        EmbeddedApp.fromServer(RatpackServer.of(server -> server
            .handlers(application.chain())
        )).test(client -> {
            // initial check that there are two accounts (REV1 and REV2) with 1000 PLN
            Assertions.assertThat(client.getText("/accounts/REV1")).contains("\"balance\":\"1000 PLN\"");
            Assertions.assertThat(client.getText("/accounts/REV2")).contains("\"balance\":\"1000 PLN\"");

            // no pending transfers
            Assertions.assertThat(client.getText("/accounts/REV1/transfers")).isEqualTo("[]");

            ReceivedResponse sameAccountTransferResponse = client.request("/accounts/REV1/transfers/" + UUID.randomUUID(),
                spec -> spec.body(body -> body.type("application/json").text("{\"targetAccount\" : \"REV1\", \"money\" : 100  }")).put());
            Assertions.assertThat(sameAccountTransferResponse.getStatusCode()).isEqualTo(400);
            Assertions.assertThat(sameAccountTransferResponse.getBody().getText()).containsIgnoringCase("same account");

            ReceivedResponse firstTransferResponse = client.request("/accounts/REV1/transfers/" + UUID.randomUUID(),
                spec -> spec.body(body -> body.type("application/json").text("{\"targetAccount\" : \"REV2\", \"money\" : 100  }")).put());
            Assertions.assertThat(firstTransferResponse.getStatusCode()).isEqualTo(200);

            Assertions.assertThat(client.getText("/accounts/REV1/transfers")).isEqualTo("[]");
            Assertions.assertThat(client.getText("/accounts/REV1")).contains("\"balance\":\"900 PLN\"");

            // switch our bank to suspend mode so Events are queued
            client.put("admin/suspension?suspend=true");

            for (int i = 0; i < 10; i++) {
                client.request("/accounts/REV1/transfers/" + UUID.randomUUID(),
                    spec -> spec.body(body -> body.type("application/json").text("{\"targetAccount\" : \"REV2\", \"money\" : 100  }")).put());
            }
            Assertions.assertThat(client.getText("/accounts/REV1/transfers")).isNotEqualTo("[]");
            Assertions.assertThat(client.getText("/accounts/REV1")).contains("\"blockedMoney\":\"900 PLN\"");

            ReceivedResponse notEnoughFundsWhenBlockades = client.request("/accounts/REV1/transfers/" + UUID.randomUUID(),
                spec -> spec.body(body -> body.type("application/json").text("{\"targetAccount\" : \"REV2\", \"money\" : 200  }")).put());
            Assertions.assertThat(notEnoughFundsWhenBlockades.getStatusCode()).isEqualTo(400);
            Assertions.assertThat(notEnoughFundsWhenBlockades.getBody().getText()).containsIgnoringCase("not enough money");

            // all pending Events are send and processed
            client.put("admin/suspension?suspend=false");
            Thread.sleep(20);

            Assertions.assertThat(client.getText("/accounts/REV1/transfers")).isEqualTo("[]");
            Assertions.assertThat(client.getText("/accounts/REV1")).contains("\"balance\":\"0 PLN\"");

            ReceivedResponse notEnoughFundsWhenNoMoney = client.request("/accounts/REV1/transfers/" + UUID.randomUUID(),
                spec -> spec.body(body -> body.type("application/json").text("{\"targetAccount\" : \"REV2\", \"money\" : 100  }")).put());
            Assertions.assertThat(notEnoughFundsWhenNoMoney.getStatusCode()).isEqualTo(400);
            Assertions.assertThat(notEnoughFundsWhenNoMoney.getBody().getText()).containsIgnoringCase("not enough money");
        });
    }

}