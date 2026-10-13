package gov.openoed.api.review;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<ReviewItem, UUID> {
	Optional<ReviewItem> findByApplicationId(UUID applicationId);
	List<ReviewItem> findByStatusOrderByPriorityDescOpenedAtAsc(String status);
	long countByStatus(String status);
}
