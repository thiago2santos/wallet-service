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

@Path("/api/v1/wallets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
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

    public Uni<Response> getWallet(
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

    public Uni<Response> depositFunds(
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

    public Uni<Response> withdrawFunds(
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

    public Uni<Response> transferFunds(
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

    public Uni<Response> getHistoricalBalance(
            @PathParam("walletId") 
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
                     message = "Wallet ID must be a valid UUID") 
            String walletId,
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
