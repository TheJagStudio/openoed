package gov.openoed.api.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.repository.JpaRepository;

@Getter
@Setter
@Entity
@Table(name = "audit_outbox")
public class AuditOutbox {

	@Id
	private UUID id;
	@Column(nullable = false, columnDefinition = "text")
	private String payload;
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
	@Column(name = "delivered_at")
	private Instant deliveredAt;
}

interface AuditOutboxRepository extends JpaRepository<AuditOutbox, UUID> {
	List<AuditOutbox> findTop50ByDeliveredAtIsNullOrderByCreatedAtAsc();
	List<AuditOutbox> findTop200ByOrderByCreatedAtDesc();
}
