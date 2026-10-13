package gov.openoed.api.fraud;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskRepository extends JpaRepository<RiskAssessment, UUID> {
	Optional<RiskAssessment> findByApplicationId(UUID applicationId);
}
