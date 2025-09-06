package com.wallet.api;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.wallet.api.request.CreateWalletRequest;
import com.wallet.api.request.DepositFundsRequest;
import com.wallet.api.request.WithdrawFundsRequest;
import com.wallet.api.request.TransferFundsRequest;
import com.wallet.application.command.CreateWalletCommand;
import com.wallet.application.command.DepositFundsCommand;
import com.wallet.application.command.WithdrawFundsCommand;
import com.wallet.application.command.TransferFundsCommand;
import com.wallet.application.handler.CreateWalletCommandHandler;
import com.wallet.application.handler.DepositFundsCommandHandler;
import com.wallet.application.handler.GetWalletQueryHandler;
import com.wallet.application.handler.WithdrawFundsCommandHandler;
import com.wallet.application.handler.TransferFundsCommandHandler;
import com.wallet.application.query.GetWalletQuery;
import com.wallet.application.query.GetHistoricalBalanceQuery;
import com.wallet.application.handler.GetHistoricalBalanceQueryHandler;
import com.wallet.core.command.CommandBus;
import com.wallet.core.query.QueryBus;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/wallets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Wallet Operations", description = "Digital wallet management operations")
public class WalletResource {
    @Inject
    CommandBus commandBus;

    @Inject
    QueryBus queryBus;

    // Temporary direct injection for debugging
    @Inject
    CreateWalletCommandHandler createWalletHandler;

    @Inject
    GetWalletQueryHandler getWalletQueryHandler;

    @Inject
    DepositFundsCommandHandler depositFundsHandler;

    @Inject
    WithdrawFundsCommandHandler withdrawFundsHandler;

    @Inject
    TransferFundsCommandHandler transferFundsHandler;

    @Inject
    GetHistoricalBalanceQueryHandler historicalBalanceQueryHandler;

    @POST
    @WithTransaction
    @Operation(
        summary = "Create a new wallet",
        description = "Creates a new digital wallet for a user with zero initial balance"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "201",
            description = "Wallet created successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"Location\": \"/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000\" }"
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"error\": \"INVALID_REQUEST\", \"message\": \"User ID is required\", \"timestamp\": \"2025-01-01T12:00:00\" }"
                )
            )
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"error\": \"INTERNAL_ERROR\", \"message\": \"An unexpected error occurred\", \"errorId\": \"abc-123\" }"
                )
            )
        )
    })
    @WithSpan("api.wallet.create")
    public Uni<Response> createWallet(@Valid CreateWalletRequest request) {
        CreateWalletCommand command = new CreateWalletCommand(
            request.getUserId()
        );

        // Using CommandBus for proper CQRS architecture
        return commandBus.dispatch(command)
            .map(walletId -> Response
                .created(URI.create("/api/v1/wallets/" + walletId))
                .build()
            );
    }

    @GET
    @Path("/{walletId}")
    @WithTransaction
    @Operation(
        summary = "Get wallet details",
        description = "Retrieves wallet information including current balance and status"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Wallet found successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"userId\": \"user123\", \"balance\": 150.75, \"status\": \"ACTIVE\", \"createdAt\": \"2025-01-01T12:00:00\", \"updatedAt\": \"2025-01-01T12:30:00\" }"
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid wallet ID format",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"error\": \"VALIDATION_ERROR\", \"message\": \"Wallet ID must be a valid UUID\" }"
                )
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Wallet not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"error\": \"WALLET_NOT_FOUND\", \"message\": \"Wallet not found: 550e8400-e29b-41d4-a716-446655440000\" }"
                )
            )
        )
    })
    public Uni<Response> getWallet(
            @Parameter(
                description = "Unique wallet identifier (UUID format)",
                example = "550e8400-e29b-41d4-a716-446655440000",
                required = true
            )
            @PathParam("walletId") 
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
                     message = "Wallet ID must be a valid UUID") 
            String walletId) {
        GetWalletQuery query = new GetWalletQuery(walletId);

        // Using QueryBus for proper CQRS architecture
        return queryBus.dispatch(query)
            .map(wallet -> Response.ok(wallet).build())
            .onFailure(NotFoundException.class)
            .recoverWithItem(e -> Response.status(Status.NOT_FOUND).build());
    }

    @POST
    @Path("/{walletId}/deposit")
    @WithTransaction
    @Operation(
        summary = "Deposit funds to wallet",
        description = "Adds money to the specified wallet with transaction tracking"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Deposit successful",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"transactionId\": \"txn-550e8400-e29b-41d4-a716-446655440000\", \"status\": \"COMPLETED\" }"
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request or negative amount",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"error\": \"INVALID_TRANSFER\", \"message\": \"Transfer amount must be positive\" }"
                )
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Wallet not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"error\": \"WALLET_NOT_FOUND\", \"message\": \"Wallet not found: 550e8400-e29b-41d4-a716-446655440000\" }"
                )
            )
        )
    })
    @WithSpan("api.wallet.deposit")
    public Uni<Response> depositFunds(
            @Parameter(
                description = "Unique wallet identifier (UUID format)",
                example = "550e8400-e29b-41d4-a716-446655440000",
                required = true
            )
            @PathParam("walletId") 
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
                     message = "Wallet ID must be a valid UUID") 
            String walletId,
            @Valid DepositFundsRequest request) {
        DepositFundsCommand command = new DepositFundsCommand(
            walletId,
            request.getAmount(),
            request.getReferenceId()
        );

        // Using CommandBus for proper CQRS architecture
        return commandBus.dispatch(command)
            .map(transactionId -> Response.ok()
                .location(URI.create("/api/v1/transactions/" + transactionId))
                .build()
            );
    }

    @POST
    @Path("/{walletId}/withdraw")
    @WithTransaction
    @Operation(
        summary = "Withdraw funds from wallet",
        description = "Removes money from the specified wallet with balance validation"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Withdrawal successful",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"transactionId\": \"txn-550e8400-e29b-41d4-a716-446655440000\", \"status\": \"COMPLETED\" }"
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Insufficient funds or invalid amount",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"error\": \"INSUFFICIENT_FUNDS\", \"message\": \"Insufficient funds. Available: 50.00, Requested: 100.00\", \"details\": { \"availableAmount\": 50.00, \"requestedAmount\": 100.00 } }"
                )
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Wallet not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"error\": \"WALLET_NOT_FOUND\", \"message\": \"Wallet not found: 550e8400-e29b-41d4-a716-446655440000\" }"
                )
            )
        )
    })
    public Uni<Response> withdrawFunds(
            @Parameter(
                description = "Unique wallet identifier (UUID format)",
                example = "550e8400-e29b-41d4-a716-446655440000",
                required = true
            )
            @PathParam("walletId") 
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
                     message = "Wallet ID must be a valid UUID") 
            String walletId,
            @Valid WithdrawFundsRequest request) {
        WithdrawFundsCommand command = new WithdrawFundsCommand(
            walletId,
            request.getAmount(),
            request.getReferenceId()
        );

        // Using CommandBus for proper CQRS architecture
        return commandBus.dispatch(command)
            .map(transactionId -> Response.ok()
                .location(URI.create("/api/v1/transactions/" + transactionId))
                .build()
            );
    }

    @POST
    @Path("/{sourceWalletId}/transfer")
    @WithTransaction
    @Operation(
        summary = "Transfer funds between wallets",
        description = "Transfers money from source wallet to destination wallet atomically"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Transfer successful",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"transactionId\": \"txn-550e8400-e29b-41d4-a716-446655440000\", \"status\": \"COMPLETED\" }"
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid transfer (insufficient funds, same wallet, invalid amount)",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"error\": \"INVALID_TRANSFER\", \"message\": \"Source and destination wallets cannot be the same\" }"
                )
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Source or destination wallet not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"error\": \"WALLET_NOT_FOUND\", \"message\": \"Wallet not found: 550e8400-e29b-41d4-a716-446655440000\" }"
                )
            )
        )
    })
    public Uni<Response> transferFunds(
            @Parameter(
                description = "Source wallet identifier (UUID format)",
                example = "550e8400-e29b-41d4-a716-446655440000",
                required = true
            )
            @PathParam("sourceWalletId") 
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
                     message = "Source wallet ID must be a valid UUID") 
            String sourceWalletId,
            @Valid TransferFundsRequest request) {
        TransferFundsCommand command = new TransferFundsCommand(
            sourceWalletId,
            request.getDestinationWalletId(),
            request.getAmount(),
            request.getReferenceId()
        );

        // Using CommandBus for proper CQRS architecture
        return commandBus.dispatch(command)
            .map(transactionId -> Response.ok()
                .location(URI.create("/api/v1/transactions/" + transactionId))
                .build()
            );
    }

    @GET
    @Path("/{walletId}/balance/historical")
    @WithTransaction
    @Operation(
        summary = "Get historical wallet balance",
        description = "Retrieves the wallet balance at a specific point in time using transaction replay"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Historical balance retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"walletId\": \"550e8400-e29b-41d4-a716-446655440000\", \"balance\": 125.50, \"timestamp\": \"2025-12-31T23:59:59\", \"transactionCount\": 15 }"
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid timestamp format",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"error\": \"VALIDATION_ERROR\", \"message\": \"Timestamp must be in format YYYY-MM-DDTHH:mm:ss\" }"
                )
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Wallet not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    type = SchemaType.OBJECT,
                    example = "{ \"error\": \"WALLET_NOT_FOUND\", \"message\": \"Wallet not found: 550e8400-e29b-41d4-a716-446655440000\" }"
                )
            )
        )
    })
    public Uni<Response> getHistoricalBalance(
            @Parameter(
                description = "Unique wallet identifier (UUID format)",
                example = "550e8400-e29b-41d4-a716-446655440000",
                required = true
            )
            @PathParam("walletId") 
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
                     message = "Wallet ID must be a valid UUID") 
            String walletId,
            @Parameter(
                description = "Point in time for balance calculation (ISO format)",
                example = "2025-12-31T23:59:59",
                required = true
            )
            @QueryParam("timestamp") 
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$", 
                     message = "Timestamp must be in format YYYY-MM-DDTHH:mm:ss") 
            String timestampStr) {
        
        if (timestampStr == null || timestampStr.trim().isEmpty()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"timestamp parameter is required\"}")
                    .build()
            );
        }

        try {
            // Parse the ISO-8601 timestamp
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            GetHistoricalBalanceQuery query = new GetHistoricalBalanceQuery(walletId, timestamp);

            // Using QueryBus for proper CQRS architecture
            return queryBus.dispatch(query)
                .map(response -> Response.ok(response).build())
                .onFailure(IllegalArgumentException.class)
                .recoverWithItem(ex -> Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + ((Exception) ex).getMessage() + "\"}")
                    .build());
        } catch (Exception e) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Invalid timestamp format. Use ISO-8601 format (e.g., 2024-01-01T10:30:00)\"}")
                    .build()
            );
        }
    }
}
