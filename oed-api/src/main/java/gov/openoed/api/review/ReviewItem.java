package gov.openoed.api.review;

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
@Table(name = "review_item")
public class ReviewItem {

	@Id
	private UUID id;
	@Column(name = "application_id", nullable = false)
	private UUID applicationId;
	@Column(nullable = false)
	private String status;
	@Column(nullable = false)
	private int priority;
	@Column(name = "assignee_id")
	private UUID assigneeId;
	@Column(length = 4000)
	private String notes;
	@Column(name = "opened_at", nullable = false)
	private Instant openedAt;
	@Column(name = "closed_at")
	private Instant closedAt;
}
