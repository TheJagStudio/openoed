package gov.openoed.api.outbox;

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
@Table(name = "domain_outbox")
public class DomainOutbox {

	@Id
	private UUID id;
	@Column(nullable = false)
	private String type;
	@Column(nullable = false, columnDefinition = "text")
	private String payload;
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
	@Column(name = "delivered_at")
	private Instant deliveredAt;
}
