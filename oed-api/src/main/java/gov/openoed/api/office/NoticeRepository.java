package gov.openoed.api.office;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {
	Optional<Notice> findByOfficeActionId(UUID officeActionId);
	List<Notice> findTop50ByOrderBySentAtDesc();
	List<Notice> findByApplicationIdOrderBySentAtDesc(UUID applicationId);
}
