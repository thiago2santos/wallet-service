package com.wallet.api.exception;

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
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", Response.Status.BAD_REQUEST.getStatusCode());
        response.put("error", "Validation Error");
        response.put("message", "Request validation failed");
        response.put("details", exception.getConstraintViolations().stream()
            .map(this::violationToMap)
            .collect(Collectors.toList()));

        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(response)
            .build();
    }

    private Map<String, String> violationToMap(ConstraintViolation<?> violation) {
        Map<String, String> violationMap = new HashMap<>();
        violationMap.put("field", violation.getPropertyPath().toString());
        violationMap.put("message", violation.getMessage());
        violationMap.put("rejectedValue", String.valueOf(violation.getInvalidValue()));
        return violationMap;
    }
}
