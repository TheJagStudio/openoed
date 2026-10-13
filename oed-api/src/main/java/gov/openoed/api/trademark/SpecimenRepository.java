package gov.openoed.api.trademark;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecimenRepository extends JpaRepository<Specimen, UUID> {
	Optional<Specimen> findByApplicationId(UUID applicationId);
}
