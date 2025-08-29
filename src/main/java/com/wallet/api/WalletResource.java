package com.wallet.api;

import com.wallet.application.command.CreateWalletCommand;
import com.wallet.application.command.DepositFundsCommand;
import com.wallet.application.query.GetWalletQuery;
import com.wallet.application.handler.CreateWalletCommandHandler;
import com.wallet.core.command.CommandBus;
import com.wallet.core.query.QueryBus;
import com.wallet.api.request.CreateWalletRequest;
import com.wallet.api.request.DepositFundsRequest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import java.net.URI;

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
    public Uni<Response> getWallet(@PathParam("walletId") String walletId) {
        GetWalletQuery query = new GetWalletQuery(walletId);

        return queryBus.dispatch(query)
            .map(wallet -> Response.ok(wallet).build())
            .onFailure(NotFoundException.class)
            .recoverWithItem(e -> Response.status(Status.NOT_FOUND).build());
    }

    @POST
    @Path("/{walletId}/deposit")
    public Uni<Response> depositFunds(
            @PathParam("walletId") String walletId,
            DepositFundsRequest request) {
        DepositFundsCommand command = new DepositFundsCommand(
            walletId,
            request.getAmount(),
            request.getReferenceId()
        );

        return commandBus.dispatch(command)
            .map(transactionId -> Response.ok()
                .location(URI.create("/api/v1/transactions/" + transactionId))
                .build()
            );
    }
}
