package com.wallet.resource;

import com.wallet.dto.CreateWalletRequest;
import com.wallet.dto.TransactionRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class WalletResourceTest {

    @Test
    public void testCreateWallet() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId("user123");
        request.setCurrency("USD");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/wallets")
            .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("userId", is("user123"))
            .body("currency", is("USD"))
            .body("balance", is(0))
            .body("status", is("ACTIVE"));
    }

    @Test
    public void testCreateWalletInvalidCurrency() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId("user123");
        request.setCurrency("INVALID");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/wallets")
            .then()
            .statusCode(400);
    }

    @Test
    public void testDepositFlow() {
        // First create a wallet
        CreateWalletRequest createRequest = new CreateWalletRequest();
        createRequest.setUserId("user123");
        createRequest.setCurrency("USD");

        String walletId = given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .when()
            .post("/api/v1/wallets")
            .then()
            .statusCode(200)
            .extract()
            .path("id");

        // Then make a deposit
        TransactionRequest depositRequest = new TransactionRequest();
        depositRequest.setAmount(new BigDecimal("100.00"));
        depositRequest.setReferenceId("DEP123");
        depositRequest.setDescription("Test deposit");

        given()
            .contentType(ContentType.JSON)
            .body(depositRequest)
            .when()
            .post("/api/v1/wallets/{walletId}/deposit", walletId)
            .then()
            .statusCode(200)
            .body("amount", is(100.00f))
            .body("type", is("DEPOSIT"))
            .body("status", is("COMPLETED"));

        // Verify the balance
        given()
            .when()
            .get("/api/v1/wallets/{walletId}", walletId)
            .then()
            .statusCode(200)
            .body("balance", is(100.00f));
    }

    @Test
    public void testWithdrawFlow() {
        // First create a wallet and deposit funds
        CreateWalletRequest createRequest = new CreateWalletRequest();
        createRequest.setUserId("user123");
        createRequest.setCurrency("USD");

        String walletId = given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .when()
            .post("/api/v1/wallets")
            .then()
            .statusCode(200)
            .extract()
            .path("id");

        // Make initial deposit
        TransactionRequest depositRequest = new TransactionRequest();
        depositRequest.setAmount(new BigDecimal("100.00"));
        depositRequest.setReferenceId("DEP123");

        given()
            .contentType(ContentType.JSON)
            .body(depositRequest)
            .when()
            .post("/api/v1/wallets/{walletId}/deposit", walletId)
            .then()
            .statusCode(200);

        // Then make a withdrawal
        TransactionRequest withdrawRequest = new TransactionRequest();
        withdrawRequest.setAmount(new BigDecimal("50.00"));
        withdrawRequest.setReferenceId("WIT123");
        withdrawRequest.setDescription("Test withdrawal");

        given()
            .contentType(ContentType.JSON)
            .body(withdrawRequest)
            .when()
            .post("/api/v1/wallets/{walletId}/withdraw", walletId)
            .then()
            .statusCode(200)
            .body("amount", is(50.00f))
            .body("type", is("WITHDRAWAL"))
            .body("status", is("COMPLETED"));

        // Verify the balance
        given()
            .when()
            .get("/api/v1/wallets/{walletId}", walletId)
            .then()
            .statusCode(200)
            .body("balance", is(50.00f));
    }

    @Test
    public void testTransferFlow() {
        // Create source wallet
        CreateWalletRequest sourceRequest = new CreateWalletRequest();
        sourceRequest.setUserId("user123");
        sourceRequest.setCurrency("USD");

        String sourceWalletId = given()
            .contentType(ContentType.JSON)
            .body(sourceRequest)
            .when()
            .post("/api/v1/wallets")
            .then()
            .statusCode(200)
            .extract()
            .path("id");

        // Create destination wallet
        CreateWalletRequest destRequest = new CreateWalletRequest();
        destRequest.setUserId("user456");
        destRequest.setCurrency("USD");

        String destWalletId = given()
            .contentType(ContentType.JSON)
            .body(destRequest)
            .when()
            .post("/api/v1/wallets")
            .then()
            .statusCode(200)
            .extract()
            .path("id");

        // Make initial deposit to source wallet
        TransactionRequest depositRequest = new TransactionRequest();
        depositRequest.setAmount(new BigDecimal("100.00"));
        depositRequest.setReferenceId("DEP123");

        given()
            .contentType(ContentType.JSON)
            .body(depositRequest)
            .when()
            .post("/api/v1/wallets/{walletId}/deposit", sourceWalletId)
            .then()
            .statusCode(200);

        // Transfer funds
        TransactionRequest transferRequest = new TransactionRequest();
        transferRequest.setAmount(new BigDecimal("50.00"));
        transferRequest.setReferenceId("TRF123");
        transferRequest.setDescription("Test transfer");
        transferRequest.setDestinationWalletId(destWalletId);

        given()
            .contentType(ContentType.JSON)
            .body(transferRequest)
            .when()
            .post("/api/v1/wallets/{walletId}/transfer", sourceWalletId)
            .then()
            .statusCode(200)
            .body("amount", is(50.00f))
            .body("type", is("TRANSFER"))
            .body("status", is("COMPLETED"));

        // Verify source wallet balance
        given()
            .when()
            .get("/api/v1/wallets/{walletId}", sourceWalletId)
            .then()
            .statusCode(200)
            .body("balance", is(50.00f));

        // Verify destination wallet balance
        given()
            .when()
            .get("/api/v1/wallets/{walletId}", destWalletId)
            .then()
            .statusCode(200)
            .body("balance", is(50.00f));
    }
}
