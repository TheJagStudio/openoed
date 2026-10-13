package gov.openoed.api.common;

import java.net.URI;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail notFound(NotFoundException ex, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail conflict(ConflictException ex, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(BadRequestException.class)
	ProblemDetail badRequest(BadRequestException ex, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(ForbiddenException.class)
	ProblemDetail forbidden(ForbiddenException ex, HttpServletRequest request) {
		return problem(HttpStatus.FORBIDDEN, ex.getMessage(), request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail accessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return problem(HttpStatus.FORBIDDEN, "You do not have access to this action", request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail invalid(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String detail = ex.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return problem(HttpStatus.BAD_REQUEST, detail.isBlank() ? "Validation failed" : detail, request);
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MaxUploadSizeExceededException.class})
	ProblemDetail unreadable(Exception ex, HttpServletRequest request) {
		String detail = ex instanceof MaxUploadSizeExceededException ? "The specimen exceeds the 8 MB limit" : "Request body is not readable";
		return problem(HttpStatus.BAD_REQUEST, detail, request);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail unexpected(Exception ex, HttpServletRequest request) {
		log.error("Unhandled {} {}", request.getMethod(), request.getRequestURI(), ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request);
	}

	private static ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
		pd.setInstance(URI.create(request.getRequestURI()));
		return pd;
	}
}
