package gov.openoed.api.outbox;

import java.time.Clock;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import gov.openoed.api.platform.PlatformClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Pushes office-action events to SQS and buffered audit rows to the ledger. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

	private final DomainOutboxRepository domain;
	private final AuditOutboxRepository audit;
	private final PlatformClient platform;
	private final JsonMapper json;
	private final Clock clock;

	@Scheduled(fixedDelayString = "${app.relay.delay-ms:2000}")
	@Transactional
	public void relay() {
		for (DomainOutbox row : domain.findTop20ByDeliveredAtIsNullOrderByCreatedAtAsc()) {
			try {
				JsonNode node = json.readTree(row.getPayload());
				if ("OFFICE_ACTION_ISSUED".equals(row.getType())) {
					platform.sendMessage("office-actions", row.getPayload());
				}
				row.setDeliveredAt(clock.instant());
				log.info("Relayed {} {}", row.getType(), node.path("scheduleName").asText());
			} catch (RuntimeException ex) {
				log.info("Outbox row {} waiting: {}", row.getId(), ex.getMessage());
				break;
			} catch (Exception ex) {
				log.warn("Outbox row {} is unreadable: {}", row.getId(), ex.getMessage());
				row.setDeliveredAt(clock.instant());
			}
		}
		List<AuditOutbox> pending = audit.findTop50ByDeliveredAtIsNullOrderByCreatedAtAsc();
		for (AuditOutbox row : pending) {
			try {
				var item = json.readValue(row.getPayload(), java.util.Map.class);
				String pk = String.valueOf(item.remove("pk"));
				String sk = String.valueOf(item.remove("sk"));
				platform.putAudit(java.util.Map.of("pk", pk, "sk", sk, "attributes", item));
				row.setDeliveredAt(clock.instant());
			} catch (RuntimeException ex) {
				break;
			} catch (Exception ex) {
				row.setDeliveredAt(clock.instant());
			}
		}
	}
}
