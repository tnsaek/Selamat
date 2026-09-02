package com.tinsae.socialmediaplatform.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFoundReturnsNotFound() {
        var response = handler.handleResourceNotFound(new ResourceNotFoundException("Missing"));

        assertError(response, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Missing");
    }

    @Test
    void handleDuplicateResourceReturnsConflict() {
        var response = handler.handleDuplicateResource(new DuplicateResourceException("Duplicate"));

        assertError(response, HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "Duplicate");
    }

    @Test
    void handleBusinessRuleReturnsUnprocessableEntity() {
        var response = handler.handleBusinessRule(new BusinessRuleException("Rule"));

        assertError(response, HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_ERROR", "Rule");
    }

    @Test
    void handleUnauthorizedActionReturnsForbidden() {
        var response = handler.handleUnauthorizedAction(new UnauthorizedActionException("Forbidden"));

        assertError(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden");
    }

    @Test
    void handleAccessDeniedReturnsForbidden() {
        var response = handler.handleAccessDenied(new AccessDeniedException("Denied"));

        assertError(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action.");
    }

    @Test
    void handleAuthenticationReturnsUnauthorized() {
        var response = handler.handleAuthentication(new BadCredentialsException("Bad"));

        assertError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required.");
    }

    @Test
    void handleValidationReturnsFieldErrorDetails() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new MockMultipartFile("file", new byte[0]), "request");
        bindingResult.addError(new FieldError("request", "username", "must not be blank"));
        Method method = SampleController.class.getDeclaredMethod("sample", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        var response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().details()).containsExactly("username: must not be blank");
    }

    @Test
    void handleConstraintViolationReturnsViolationDetails() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("limit");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be positive");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        var response = handler.handleConstraintViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details()).containsExactly("limit: must be positive");
    }

    @Test
    void handleUnreadableMessageReturnsBadRequest() {
        var response = handler.handleUnreadableMessage(mock(HttpMessageNotReadableException.class));

        assertError(response, HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Request body is invalid or malformed.");
    }

    @Test
    void handleTypeMismatchUsesRequiredTypeNameWhenPresent() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "x",
                Integer.class,
                "limit",
                null,
                new IllegalArgumentException("bad")
        );

        var response = handler.handleTypeMismatch(exception);

        assertError(response, HttpStatus.BAD_REQUEST, "BAD_REQUEST", "limit must be a valid Integer.");
    }

    @Test
    void handleTypeMismatchUsesFallbackWhenRequiredTypeIsNull() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "x",
                null,
                "value",
                null,
                new IllegalArgumentException("bad")
        );

        var response = handler.handleTypeMismatch(exception);

        assertError(response, HttpStatus.BAD_REQUEST, "BAD_REQUEST", "value must be a valid required type.");
    }

    @Test
    void handleMissingRequestParameterReturnsBadRequest() {
        var response = handler.handleMissingRequestParameter(new MissingServletRequestParameterException("limit", "Integer"));

        assertError(response, HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Missing required request parameter: limit");
    }

    @Test
    void handleMissingRequestPartReturnsBadRequest() {
        var response = handler.handleMissingRequestPart(new MissingServletRequestPartException("file"));

        assertError(response, HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Missing required multipart field: file");
    }

    @Test
    void handleMethodNotSupportedReturnsMethodNotAllowed() {
        var response = handler.handleMethodNotSupported(mock(HttpRequestMethodNotSupportedException.class));

        assertError(response, HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "HTTP method is not supported for this endpoint.");
    }

    @Test
    void handleMediaTypeNotSupportedReturnsUnsupportedMediaType() {
        var response = handler.handleMediaTypeNotSupported(mock(HttpMediaTypeNotSupportedException.class));

        assertError(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Content type is not supported.");
    }

    @Test
    void handleMaxUploadSizeExceededReturnsPayloadTooLarge() {
        var response = handler.handleMaxUploadSizeExceeded(mock(MaxUploadSizeExceededException.class));

        assertError(response, HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "Uploaded file must not exceed 10 MB.");
    }

    @Test
    void handleUnexpectedReturnsInternalServerError() {
        var response = handler.handleUnexpected(new RuntimeException("boom"));

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred.");
    }

    private void assertError(org.springframework.http.ResponseEntity<com.tinsae.socialmediaplatform.common.dto.ErrorResponse> response,
                             HttpStatus status,
                             String code,
                             String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(code);
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().details()).isEmpty();
    }

    private static class SampleController {
        @SuppressWarnings("unused")
        void sample(String value) {
        }
    }
}
