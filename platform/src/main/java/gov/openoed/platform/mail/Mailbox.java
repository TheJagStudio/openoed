package gov.openoed.platform.mail;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

/** SES outbox. Messages are kept so the UI and tests can read what was sent. */
@Component
public class Mailbox {

	public record Email(String messageId, String to, String subject, String text, Instant sentAt) {
	}

	private final CopyOnWriteArrayList<Email> emails = new CopyOnWriteArrayList<>();

	public Email send(String to, String subject, String text) {
		Email email = new Email("ses-" + UUID.randomUUID(), to, subject, text, Instant.now());
		emails.add(email);
		return email;
	}

	public List<Email> all() {
		return List.copyOf(emails);
	}
}
