package com.wallet.api;

import com.wallet.security.JwtTokenGenerator;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * Authentication endpoint for demonstration purposes.
 * 
 * In a production environment, authentication would be handled by an external
 * identity provider (like Keycloak, Auth0, etc.). This endpoint is provided
 * for testing and demonstration of the JWT authentication flow.
 */
@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    JwtTokenGenerator tokenGenerator;

    /**
     * Login endpoint for demonstration purposes.
     * 
     * In production, this would validate credentials against a user store
     * and return a JWT token upon successful authentication.
     */
    @POST
    @Path("/login")
    public Uni<Response> login(LoginRequest request) {
        // Simple demonstration logic - in production, validate against user store
        if (request == null || request.username == null || request.password == null) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Username and password are required"))
                    .build()
            );
        }

        // Demo credentials - in production, validate against secure user store
        String token;
        if ("admin".equals(request.username) && "admin123".equals(request.password)) {
            token = tokenGenerator.generateAdminToken(request.username);
        } else if ("user".equals(request.username) && "user123".equals(request.password)) {
            token = tokenGenerator.generateUserToken(request.username);
        } else {
            return Uni.createFrom().item(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid credentials"))
                    .build()
            );
        }

        Map<String, Object> response = Map.of(
            "access_token", token,
            "token_type", "Bearer",
            "expires_in", 3600, // 1 hour
            "user", request.username
        );

        return Uni.createFrom().item(Response.ok(response).build());
    }

    /**
     * Simple login request DTO.
     */
    public static class LoginRequest {
        public String username;
        public String password;
        
        // Default constructor for JSON deserialization
        public LoginRequest() {}
        
        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
