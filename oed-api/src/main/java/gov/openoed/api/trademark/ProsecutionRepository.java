package gov.openoed.api.trademark;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProsecutionRepository extends JpaRepository<ProsecutionEvent, UUID> {
	List<ProsecutionEvent> findByApplicationIdOrderByAtAsc(UUID applicationId);
}
