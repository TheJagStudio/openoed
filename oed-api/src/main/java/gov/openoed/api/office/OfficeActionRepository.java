package gov.openoed.api.office;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OfficeActionRepository extends JpaRepository<OfficeAction, UUID> {

	List<OfficeAction> findByApplicationIdOrderByIssuedAtDesc(UUID applicationId);

	List<OfficeAction> findByStatusOrderByScheduleFireAtAsc(String status);

	long countByStatus(String status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from OfficeAction o where o.id = :id")
	Optional<OfficeAction> lock(UUID id);
}
