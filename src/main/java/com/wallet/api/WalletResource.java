package com.wallet.api;

import java.net.URI;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

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

    @POST
    @WithTransaction
    public Uni<Response> createWallet(CreateWalletRequest request) {
        CreateWalletCommand command = new CreateWalletCommand(
            request.getUserId(),
            request.getCurrency()
        );

        // Temporary direct call for debugging
        return createWalletHandler.handle(command)
            .map(walletId -> Response
                .created(URI.create("/api/v1/wallets/" + walletId))
                .build()
            );
    }

    @GET
    @Path("/{walletId}")
    @WithTransaction
    public Uni<Response> getWallet(@PathParam("walletId") String walletId) {
        GetWalletQuery query = new GetWalletQuery(walletId);

        // Temporary direct call to test the handler
        return getWalletQueryHandler.handle(query)
            .map(wallet -> Response.ok(wallet).build())
            .onFailure(NotFoundException.class)
            .recoverWithItem(e -> Response.status(Status.NOT_FOUND).build());
    }

    @POST
    @Path("/{walletId}/deposit")
    @WithTransaction
    public Uni<Response> depositFunds(
            @PathParam("walletId") String walletId,
            DepositFundsRequest request) {
        DepositFundsCommand command = new DepositFundsCommand(
            walletId,
            request.getAmount(),
            request.getReferenceId()
        );

        // Temporary direct call to test the handler
        return depositFundsHandler.handle(command)
            .map(transactionId -> Response.ok()
                .location(URI.create("/api/v1/transactions/" + transactionId))
                .build()
            );
    }

    @POST
    @Path("/{walletId}/withdraw")
    @WithTransaction
    public Uni<Response> withdrawFunds(
            @PathParam("walletId") String walletId,
            WithdrawFundsRequest request) {
        WithdrawFundsCommand command = new WithdrawFundsCommand(
            walletId,
            request.getAmount(),
            request.getReferenceId()
        );

        // Temporary direct call to test the handler
        return withdrawFundsHandler.handle(command)
            .map(transactionId -> Response.ok()
                .location(URI.create("/api/v1/transactions/" + transactionId))
                .build()
            );
    }

    @POST
    @Path("/{sourceWalletId}/transfer")
    @WithTransaction
    public Uni<Response> transferFunds(
            @PathParam("sourceWalletId") String sourceWalletId,
            TransferFundsRequest request) {
        TransferFundsCommand command = new TransferFundsCommand(
            sourceWalletId,
            request.getDestinationWalletId(),
            request.getAmount(),
            request.getReferenceId()
        );

        // Temporary direct call to test the handler
        return transferFundsHandler.handle(command)
            .map(transactionId -> Response.ok()
                .location(URI.create("/api/v1/transactions/" + transactionId))
                .build()
            );
    }
}
