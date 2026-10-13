package gov.openoed.api.office;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notice")
public class Notice {

	@Id
	private UUID id;
	@Column(name = "application_id", nullable = false)
	private UUID applicationId;
	@Column(name = "office_action_id", nullable = false)
	private UUID officeActionId;
	@Column(name = "to_email", nullable = false)
	private String toEmail;
	@Column(nullable = false)
	private String subject;
	@Column(nullable = false, length = 8000)
	private String body;
	@Column(name = "provider_message_id")
	private String providerMessageId;
	@Column(name = "sent_at", nullable = false)
	private Instant sentAt;
}
