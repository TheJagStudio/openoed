package gov.openoed.platform.bridge;

import java.time.Instant;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import gov.openoed.platform.schedule.ScheduleStore;
import gov.openoed.platform.sqs.SqsQueues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Decoupled consumer: an office-action event on SQS becomes an EventBridge Scheduler one-shot.
 * In AWS this is a separate worker; locally it shares the platform process.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfficeActionBridge {

	public static final String QUEUE = "office-actions";

	private final SqsQueues queues;
	private final ScheduleStore schedules;
	private final JsonMapper json;

	@Scheduled(fixedDelay = 1000)
	public void poll() {
		for (SqsQueues.Message message : queues.receive(QUEUE, 10)) {
			try {
				JsonNode node = json.readTree(message.body());
				String name = node.path("scheduleName").asText();
				Instant fireAt = Instant.parse(node.path("fireAt").asText());
				schedules.upsert(name, fireAt, message.body());
				queues.delete(QUEUE, message.receiptHandle());
				log.info("Scheduled {} at {}", name, fireAt);
			} catch (Exception ex) {
				log.warn("Office-action message {} was not scheduled: {}", message.messageId(), ex.getMessage());
			}
		}
	}
}
