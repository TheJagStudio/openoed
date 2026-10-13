package gov.openoed.deadline;

import java.util.UUID;

/** Final notice the Lambda asks SES to send. {@code providerMessageId} is filled after SES accepts it. */
public record NoticeDraft(
		UUID officeActionId,
		UUID applicationId,
		String toEmail,
		String subject,
		String text,
		String providerMessageId) {

	public NoticeDraft(UUID officeActionId, UUID applicationId, String toEmail, String subject, String text) {
		this(officeActionId, applicationId, toEmail, subject, text, null);
	}

	public NoticeDraft withMessageId(String id) {
		return new NoticeDraft(officeActionId, applicationId, toEmail, subject, text, id);
	}
}
