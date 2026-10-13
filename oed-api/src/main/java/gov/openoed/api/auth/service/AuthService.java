package gov.openoed.api.auth.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import gov.openoed.api.auth.RefreshToken;
import gov.openoed.api.auth.RefreshTokenRepository;
import gov.openoed.api.auth.Role;
import gov.openoed.api.auth.UserAccount;
import gov.openoed.api.auth.UserRepository;
import gov.openoed.api.common.BadRequestException;
import gov.openoed.api.common.ConflictException;
import gov.openoed.api.common.ForbiddenException;
import gov.openoed.api.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final int LOCK_AFTER = 5;

	private final UserRepository users;
	private final RefreshTokenRepository refreshTokens;
	private final PasswordEncoder passwords;
	private final TokenService tokens;
	private final AppProperties props;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	public record AuthResult(String accessToken, long expiresIn, String refreshToken, UserAccount user) {
	}

	public record UserView(UUID id, String email, String displayName, Role role, String artUnit) {
		public static UserView of(UserAccount user) {
			return new UserView(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getArtUnit());
		}
	}

	@Transactional
	public AuthResult register(String email, String password, String displayName) {
		String normalized = email.trim().toLowerCase();
		if (users.existsByEmailIgnoreCase(normalized)) throw new ConflictException("An account with that email already exists");
		if (password.length() < 10) throw new BadRequestException("Password must be at least 10 characters");
		UserAccount user = new UserAccount();
		user.setId(UUID.randomUUID());
		user.setEmail(normalized);
		user.setPasswordHash(passwords.encode(password));
		user.setDisplayName(displayName.trim());
		user.setRole(Role.APPLICANT);
		user.setEnabled(true);
		user.setCreatedAt(clock.instant());
		users.save(user);
		return issue(user);
	}

	@Transactional
	public AuthResult login(String email, String password) {
		UserAccount user = users.findByEmailIgnoreCase(email.trim()).orElseThrow(() -> new ForbiddenException("Invalid email or password"));
		Instant now = clock.instant();
		if (!user.isEnabled()) throw new ForbiddenException("This account is disabled");
		if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
			throw new ForbiddenException("Account locked after repeated sign-in failures. Try again later.");
		}
		if (!passwords.matches(password, user.getPasswordHash())) {
			user.setFailedLogins(user.getFailedLogins() + 1);
			if (user.getFailedLogins() >= LOCK_AFTER) user.setLockedUntil(now.plusSeconds(15 * 60));
			throw new ForbiddenException("Invalid email or password");
		}
		user.setFailedLogins(0);
		user.setLockedUntil(null);
		return issue(user);
	}

	@Transactional
	public AuthResult refresh(String rawRefresh) {
		RefreshToken stored = refreshTokens.findByTokenHash(hash(rawRefresh)).orElseThrow(() -> new ForbiddenException("Refresh token is not valid"));
		if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(clock.instant())) {
			refreshTokens.revokeAll(stored.getUserId());
			throw new ForbiddenException("Refresh token is not valid");
		}
		stored.setRevokedAt(clock.instant());
		UserAccount user = users.findById(stored.getUserId()).orElseThrow(() -> new ForbiddenException("Refresh token is not valid"));
		return issue(user);
	}

	@Transactional
	public void logout(String rawRefresh) {
		if (rawRefresh == null || rawRefresh.isBlank()) return;
		refreshTokens.findByTokenHash(hash(rawRefresh)).ifPresent(token -> token.setRevokedAt(clock.instant()));
	}

	@Transactional(readOnly = true)
	public UserView me(UUID id) {
		return UserView.of(users.findById(id).orElseThrow(() -> new ForbiddenException("Unknown account")));
	}

	private AuthResult issue(UserAccount user) {
		TokenService.Issued access = tokens.issue(user);
		String raw = newRawToken();
		RefreshToken refresh = new RefreshToken();
		refresh.setId(UUID.randomUUID());
		refresh.setUserId(user.getId());
		refresh.setTokenHash(hash(raw));
		refresh.setExpiresAt(clock.instant().plus(props.jwt().refreshTtl()));
		refresh.setCreatedAt(clock.instant());
		refreshTokens.save(refresh);
		return new AuthResult(access.value(), access.expiresInSeconds(), raw, user);
	}

	private String newRawToken() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String hash(String raw) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
