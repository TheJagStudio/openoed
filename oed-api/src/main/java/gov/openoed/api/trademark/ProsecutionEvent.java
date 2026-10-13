package gov.openoed.api.trademark;

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
@Table(name = "prosecution_event")
public class ProsecutionEvent {

	@Id
	private UUID id;
	@Column(name = "application_id", nullable = false)
	private UUID applicationId;
	@Column(nullable = false)
	private String kind;
	@Column(nullable = false, length = 2000)
	private String detail;
	@Column(name = "actor_id")
	private UUID actorId;
	@Column(name = "at", nullable = false)
	private Instant at;
}
