package gov.openoed.api.trademark;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "specimen")
public class Specimen {

	@Id
	private UUID id;
	@Column(name = "application_id", nullable = false)
	private UUID applicationId;
	@Column(name = "content_type", nullable = false)
	private String contentType;
	@Column(name = "byte_size", nullable = false)
	private int byteSize;
	@Column(nullable = false)
	private String sha256;
	@Column(name = "storage_key", nullable = false)
	private String storageKey;
	@Column(nullable = false)
	private int width;
	@Column(nullable = false)
	private int height;
	@Column(name = "exif_present", nullable = false)
	private boolean exifPresent;
	@Column(nullable = false)
	private boolean jpeg;
}
