package gov.openoed.api.auth.service;

import java.util.UUID;
import gov.openoed.api.auth.UserAccount;
import gov.openoed.api.auth.UserRepository;
import gov.openoed.api.common.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActorService {

	private final UserRepository users;

	@Transactional(readOnly = true)
	public UserAccount require(Jwt jwt) {
		return users.findById(UUID.fromString(jwt.getSubject()))
				.orElseThrow(() -> new NotFoundException("Account no longer exists"));
	}
}
