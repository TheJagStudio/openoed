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
@Table(name = "applicant_response")
public class ApplicantResponse {

	@Id
	private UUID id;
	@Column(name = "office_action_id", nullable = false)
	private UUID officeActionId;
	@Column(name = "applicant_id", nullable = false)
	private UUID applicantId;
	@Column(nullable = false, length = 8000)
	private String body;
	@Column(name = "submitted_at", nullable = false)
	private Instant submittedAt;
}
