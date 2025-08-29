package com.wallet.api;

import com.wallet.api.request.CreateWalletRequest;
import com.wallet.application.handler.CreateWalletCommandHandler;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class WalletResourceTest {

    @InjectMock
    CreateWalletCommandHandler createWalletHandler;

    @Test
    void shouldCreateWalletSuccessfully() {
        // Given
        String walletId = "test-wallet-id-123";
        when(createWalletHandler.handle(any()))
            .thenReturn(Uni.createFrom().item(walletId));

        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId("user-123");
        request.setCurrency("USD");

        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(201)
            .header("Location", containsString("/api/v1/wallets/" + walletId));
    }

    @Test
    void shouldReturnBadRequestForInvalidRequest() {
        // Given - Invalid request with missing userId
        CreateWalletRequest request = new CreateWalletRequest();
        request.setCurrency("USD");
        // userId is null

        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(400);
    }

    @Test
    void shouldReturnBadRequestForInvalidCurrency() {
        // Given - Invalid request with missing currency
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId("user-123");
        // currency is null

        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(400);
    }

    @Test
    void shouldHandleHandlerFailure() {
        // Given
        when(createWalletHandler.handle(any()))
            .thenReturn(Uni.createFrom().failure(new RuntimeException("Database error")));

        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId("user-123");
        request.setCurrency("USD");

        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(500);
    }

    @Test
    void shouldAcceptDifferentCurrencies() {
        // Given
        String[] currencies = {"USD", "EUR", "GBP", "JPY", "BRL"};
        
        for (String currency : currencies) {
            String walletId = "wallet-" + currency.toLowerCase();
            when(createWalletHandler.handle(any()))
                .thenReturn(Uni.createFrom().item(walletId));

            CreateWalletRequest request = new CreateWalletRequest();
            request.setUserId("user-test");
            request.setCurrency(currency);

            // When & Then
            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/api/v1/wallets")
            .then()
                .statusCode(201)
                .header("Location", containsString("/api/v1/wallets/" + walletId));
        }
    }

    @Test
    void shouldReturnUnsupportedMediaTypeForInvalidContentType() {
        // Given
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId("user-123");
        request.setCurrency("USD");

        // When & Then
        given()
            .contentType(ContentType.XML)
            .body(request)
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(415); // Unsupported Media Type
    }

    @Test
    void shouldReturnBadRequestForEmptyBody() {
        // When & Then
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(400);
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() {
        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body("{invalid json}")
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(400);
    }
}
