package gov.openoed.api.auth;

import java.util.UUID;
import gov.openoed.api.audit.Audited;
import gov.openoed.api.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService auth;

	public record RegisterRequest(
			@Email @NotBlank String email,
			@NotBlank @Size(min = 10, max = 200) String password,
			@NotBlank @Size(max = 80) String displayName) {
	}

	public record LoginRequest(@NotBlank String email, @NotBlank String password) {
	}

	public record RefreshRequest(@NotBlank String refreshToken) {
	}

	public record AuthResponse(String accessToken, String refreshToken, long expiresIn, String tokenType, AuthService.UserView user) {
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Audited(action = "REGISTER")
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return response(auth.register(request.email(), request.password(), request.displayName()));
	}

	@PostMapping("/login")
	@Audited(action = "LOGIN")
	public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
		return response(auth.login(request.email(), request.password()));
	}

	@PostMapping("/refresh")
	public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return response(auth.refresh(request.refreshToken()));
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@RequestBody(required = false) RefreshRequest request) {
		auth.logout(request == null ? null : request.refreshToken());
	}

	@GetMapping("/me")
	public AuthService.UserView me(@AuthenticationPrincipal Jwt jwt) {
		return auth.me(UUID.fromString(jwt.getSubject()));
	}

	private static AuthResponse response(AuthService.AuthResult result) {
		return new AuthResponse(result.accessToken(), result.refreshToken(), result.expiresIn(), "Bearer",
				AuthService.UserView.of(result.user()));
	}
}
