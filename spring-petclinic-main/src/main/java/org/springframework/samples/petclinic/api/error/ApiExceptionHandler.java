package org.springframework.samples.petclinic.api.error;

import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.api.response.StandardResponse;
import org.springframework.samples.petclinic.api.security.RequestIdResolver;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(basePackages = "org.springframework.samples.petclinic.api")
public class ApiExceptionHandler {

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<StandardResponse<Void>> handleResponseStatus(ResponseStatusException ex,
			HttpServletRequest request) {
		String requestId = RequestIdResolver.resolve(request);
		return ResponseEntity.status(ex.getStatusCode()).body(StandardResponse.error(ex.getReason(), requestId));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<StandardResponse<Void>> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		String requestId = RequestIdResolver.resolve(request);
		String message = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(this::formatFieldError)
			.collect(Collectors.joining(", "));
		return ResponseEntity.badRequest().body(StandardResponse.error(message, requestId));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<StandardResponse<Void>> handleConstraintViolation(ConstraintViolationException ex,
			HttpServletRequest request) {
		String requestId = RequestIdResolver.resolve(request);
		return ResponseEntity.badRequest().body(StandardResponse.error(ex.getMessage(), requestId));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<StandardResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
		String requestId = RequestIdResolver.resolve(request);
		return ResponseEntity.internalServerError()
			.body(StandardResponse.error("Unexpected server error", requestId));
	}

	private String formatFieldError(FieldError fieldError) {
		return fieldError.getField() + " " + fieldError.getDefaultMessage();
	}

}