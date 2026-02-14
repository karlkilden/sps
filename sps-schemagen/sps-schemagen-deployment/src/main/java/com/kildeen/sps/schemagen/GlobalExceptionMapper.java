package com.kildeen.sps.schemagen;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Global exception mapper for consistent error responses.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    @Override
    public Response toResponse(Exception exception) {
        LOG.error("Unhandled exception", exception);

        Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
        String message = "Internal server error";

        if (exception instanceof IllegalArgumentException) {
            status = Response.Status.BAD_REQUEST;
            message = exception.getMessage();
        } else if (exception instanceof jakarta.validation.ValidationException) {
            status = Response.Status.BAD_REQUEST;
            message = "Validation failed: " + exception.getMessage();
        }

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(message, status.getStatusCode()))
                .build();
    }

    public record ErrorResponse(String message, int statusCode) {}
}
