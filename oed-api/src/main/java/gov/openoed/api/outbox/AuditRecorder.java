package gov.openoed.api.outbox;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import gov.openoed.api.audit.AuditContext;
import gov.openoed.api.platform.PlatformClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Writes audit items to DynamoDB, falling back to a Postgres outbox when the ledger is unreachable. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditRecorder {

	private final PlatformClient platform;
	private final AuditOutboxRepository outbox;
	private final JsonMapper json;
	private final TransactionTemplate transactions;

	public void success(String action, HttpServletRequest request) {
		write(action, "SUCCESS", request);
	}

	public void failure(String action, String outcome, HttpServletRequest request) {
		write(action, outcome, request);
	}

	public void denied(HttpServletRequest request, String outcome, String principal) {
		Map<String, Object> item = base(outcome.equals("UNAUTHENTICATED") ? "ACCESS" : "ACCESS", outcome, request);
		if (principal != null) item.put("actorId", principal);
		item.put("anomaly", "FORBIDDEN".equals(outcome));
		deliver(item);
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> search(int days, String action, String actor, Boolean anomaly) {
		List<Map<String, Object>> all = new ArrayList<>();
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		for (int i = 0; i < Math.max(days, 1); i++) {
			String pk = "AUDIT#" + today.minusDays(i);
			try {
				all.addAll(platform.queryAudit(pk, null));
			} catch (RuntimeException ex) {
				log.debug("Ledger query skipped for {}: {}", pk, ex.getMessage());
			}
		}
		for (AuditOutbox row : outbox.findTop200ByOrderByCreatedAtDesc()) {
			try {
				all.add(json.readValue(row.getPayload(), new TypeReference<>() {
				}));
			} catch (Exception ignored) {
				// skip a corrupt buffer row
			}
		}
		return all.stream()
				.filter(item -> action == null || action.isBlank() || action.equals(item.get("action")))
				.filter(item -> actor == null || actor.isBlank() || actor.equalsIgnoreCase(String.valueOf(item.get("actorEmail"))))
				.filter(item -> anomaly == null || anomaly.equals(Boolean.TRUE.equals(item.get("anomaly")) || "true".equals(String.valueOf(item.get("anomaly")))))
				.toList();
	}

	private void write(String action, String outcome, HttpServletRequest request) {
		deliver(base(action, outcome, request));
		AuditContext.clear();
	}

	private Map<String, Object> base(String action, String outcome, HttpServletRequest request) {
		Instant now = Instant.now();
		String id = UUID.randomUUID().toString();
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("pk", "AUDIT#" + LocalDate.ofInstant(now, ZoneOffset.UTC));
		item.put("sk", now + "#" + id);
		item.put("action", action);
		item.put("outcome", outcome);
		item.put("at", now.toString());
		Map<String, String> extra = AuditContext.snapshot();
		item.put("resourceId", extra.get("resourceId"));
		item.put("detail", extra.get("detail"));
		item.put("anomalyCode", extra.get("anomalyCode"));
		item.put("anomaly", "true".equals(extra.get("anomaly")));
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
			item.put("actorId", jwt.getSubject());
			item.put("actorEmail", jwt.getClaimAsString("email"));
			item.put("actorRole", jwt.getClaimAsString("role"));
			item.put("artUnit", jwt.getClaimAsString("artUnit"));
		} else if (extra.get("actorEmail") != null) {
			item.put("actorEmail", extra.get("actorEmail"));
			item.put("actorRole", extra.get("actorRole"));
		}
		if (request != null) {
			item.put("ip", clientIp(request));
			item.put("userAgent", request.getHeader("User-Agent"));
			item.put("path", request.getRequestURI());
		}
		return item;
	}

	private void deliver(Map<String, Object> item) {
		Map<String, Object> attributes = new LinkedHashMap<>(item);
		String pk = String.valueOf(attributes.remove("pk"));
		String sk = String.valueOf(attributes.remove("sk"));
		try {
			platform.putAudit(Map.of("pk", pk, "sk", sk, "attributes", attributes));
		} catch (RuntimeException ex) {
			log.info("Audit ledger unreachable, buffering {} {}", item.get("action"), ex.getMessage());
			buffer(item);
		}
	}

	public void buffer(Map<String, Object> item) {
		try {
			transactions.executeWithoutResult(status -> {
				AuditOutbox row = new AuditOutbox();
				row.setId(UUID.randomUUID());
				row.setPayload(write(item));
				row.setCreatedAt(Instant.now());
				outbox.save(row);
			});
		} catch (RuntimeException e) {
			log.warn("Audit event could not be buffered: {}", e.getMessage());
		}
	}

	private String write(Map<String, Object> item) {
		try {
			return json.writeValueAsString(item);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static String clientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
		return request.getRemoteAddr();
	}
}
