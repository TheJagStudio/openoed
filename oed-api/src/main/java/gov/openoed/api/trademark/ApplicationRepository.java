package gov.openoed.api.trademark;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<TrademarkApplication, UUID> {
	Optional<TrademarkApplication> findBySerialNumber(String serialNumber);
	boolean existsBySerialNumber(String serialNumber);
	List<TrademarkApplication> findTop300ByOrderByFiledAtDesc();

	@Query("""
			select a from TrademarkApplication a
			where (:owner is null or a.ownerId = :owner)
			  and (:status is null or a.status = :status)
			  and (:band is null or a.riskBand = :band)
			  and (:q is null or lower(a.mark) like :q or lower(a.serialNumber) like :q or lower(a.ownerName) like :q)
			order by a.filedAt desc
			""")
	List<TrademarkApplication> search(@Param("owner") UUID owner, @Param("status") ApplicationStatus status,
			@Param("band") String band, @Param("q") String q, Pageable pageable);
	long countByRiskBandIn(List<String> bands);
	long countByStatus(ApplicationStatus status);
}
