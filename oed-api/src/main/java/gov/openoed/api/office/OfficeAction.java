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
@Table(name = "office_action")
public class OfficeAction {

	@Id
	private UUID id;
	@Column(name = "application_id", nullable = false)
	private UUID applicationId;
	@Column(name = "examiner_id", nullable = false)
	private UUID examinerId;
	@Column(nullable = false)
	private String type;
	@Column(nullable = false, length = 8000)
	private String body;
	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;
	@Column(name = "statutory_due_at", nullable = false)
	private Instant statutoryDueAt;
	@Column(name = "schedule_fire_at", nullable = false)
	private Instant scheduleFireAt;
	@Column(name = "schedule_name", nullable = false)
	private String scheduleName;
	@Column(nullable = false)
	private String status;
}
