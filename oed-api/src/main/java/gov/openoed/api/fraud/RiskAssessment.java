package gov.openoed.api.fraud;

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
@Table(name = "risk_assessment")
public class RiskAssessment {

	@Id
	private UUID id;
	@Column(name = "application_id", nullable = false)
	private UUID applicationId;
	@Column(nullable = false)
	private int score;
	@Column(nullable = false)
	private String band;
	@Column(name = "findings_json", nullable = false, columnDefinition = "text")
	private String findingsJson;
	@Column(name = "claude_status", nullable = false)
	private String claudeStatus;
	@Column(name = "claude_summary", length = 4000)
	private String claudeSummary;
	@Column(name = "claude_suspicion")
	private String claudeSuspicion;
	@Column(name = "claude_flags_json", columnDefinition = "text")
	private String claudeFlagsJson;
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
