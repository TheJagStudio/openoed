package gov.openoed.api.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainOutboxRepository extends JpaRepository<DomainOutbox, UUID> {
	List<DomainOutbox> findTop20ByDeliveredAtIsNullOrderByCreatedAtAsc();
}
