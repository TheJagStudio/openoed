package gov.openoed.api.trademark;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "trademark_application")
public class TrademarkApplication {

	@Id
	private UUID id;
	@Column(name = "serial_number", nullable = false)
	private String serialNumber;
	@Column(name = "owner_id", nullable = false)
	private UUID ownerId;
	@Column(nullable = false)
	private String mark;
	@Column(name = "mark_type", nullable = false)
	private String markType;
	@Column(name = "international_class", nullable = false)
	private int internationalClass;
	@Column(name = "art_unit", nullable = false)
	private String artUnit;
	@Column(name = "goods_description", nullable = false, length = 4000)
	private String goodsDescription;
	@Column(name = "owner_name", nullable = false)
	private String ownerName;
	@Column(name = "owner_address", nullable = false, length = 500)
	private String ownerAddress;
	@Column(name = "owner_email", nullable = false)
	private String ownerEmail;
	@Column(name = "attorney_name")
	private String attorneyName;
	@Column(name = "attorney_bar_number")
	private String attorneyBarNumber;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ApplicationStatus status;
	@Column(name = "risk_score")
	private Integer riskScore;
	@Column(name = "risk_band")
	private String riskBand;
	@Column(name = "filed_at", nullable = false)
	private Instant filedAt;
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
