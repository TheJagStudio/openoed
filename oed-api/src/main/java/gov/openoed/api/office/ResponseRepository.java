package gov.openoed.api.office;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponseRepository extends JpaRepository<ApplicantResponse, UUID> {
	Optional<ApplicantResponse> findByOfficeActionId(UUID officeActionId);
	boolean existsByOfficeActionId(UUID officeActionId);
}
