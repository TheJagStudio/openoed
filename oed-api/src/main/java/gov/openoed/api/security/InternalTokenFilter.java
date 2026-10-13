package gov.openoed.api.security;

import java.io.IOException;
import gov.openoed.api.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Shared secret for the Lambda and other local workers. Browser traffic never uses this path. */
@Component
@RequiredArgsConstructor
public class InternalTokenFilter extends OncePerRequestFilter {

	private final AppProperties props;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/internal/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String token = request.getHeader("X-Internal-Token");
		if (!props.platform().internalToken().equals(token)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal token");
			return;
		}
		chain.doFilter(request, response);
	}
}
