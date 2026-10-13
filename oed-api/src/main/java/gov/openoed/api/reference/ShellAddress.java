package gov.openoed.api.reference;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.repository.JpaRepository;

@Getter
@Setter
@Entity
@Table(name = "shell_address")
public class ShellAddress {
	@Id
	private UUID id;
	@Column(nullable = false)
	private String address;
	private String note;
}

@Getter
@Setter
@Entity
@Table(name = "famous_mark")
class FamousMark {
	@Id
	private UUID id;
	@Column(nullable = false)
	private String mark;
}

@Getter
@Setter
@Entity
@Table(name = "attorney_watch")
class AttorneyWatch {
	@Id
	@Column(name = "bar_number")
	private String barNumber;
	@Column(nullable = false)
	private String name;
	@Column(nullable = false)
	private String standing;
}

interface ShellAddressRepository extends JpaRepository<ShellAddress, UUID> {
}

interface FamousMarkRepository extends JpaRepository<FamousMark, UUID> {
}

interface AttorneyWatchRepository extends JpaRepository<AttorneyWatch, String> {
}
