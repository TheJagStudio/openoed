package gov.openoed.api.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import gov.openoed.api.auth.Role;
import gov.openoed.api.auth.UserAccount;
import gov.openoed.api.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

	private final JwtEncoder encoder;
	private final AppProperties props;
	private final Clock clock;

	public record Issued(String value, long expiresInSeconds) {
	}

	public Issued issue(UserAccount user) {
		Instant now = clock.instant();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer(props.jwt().issuer())
				.subject(user.getId().toString())
				.issuedAt(now)
				.expiresAt(now.plus(props.jwt().accessTtl()))
				.id(UUID.randomUUID().toString())
				.claim("email", user.getEmail())
				.claim("name", user.getDisplayName())
				.claim("role", user.getRole().name());
		if (user.getArtUnit() != null) claims.claim("artUnit", user.getArtUnit());
		String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims.build())).getTokenValue();
		return new Issued(token, props.jwt().accessTtl().toSeconds());
	}
}
