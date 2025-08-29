package com.wallet.resource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    
    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", "VALIDATION_ERROR");
        error.put("message", "Validation failed");
        error.put("details", exception.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                    violation -> getPropertyPath(violation),
                    ConstraintViolation::getMessage
                )));
        error.put("timestamp", LocalDateTime.now());

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(error)
                .build();
    }

    private String getPropertyPath(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        // Remove method name from the path if present
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(lastDot + 1) : path;
    }
}
