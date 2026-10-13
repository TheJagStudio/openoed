package gov.openoed.deadline;

import java.time.Instant;
import java.util.UUID;

/** Payload EventBridge Scheduler delivers when an office-action timer fires. */
public record DeadlineEvent(UUID officeActionId, UUID applicationId, String serialNumber, Instant statutoryDueAt) {
}
