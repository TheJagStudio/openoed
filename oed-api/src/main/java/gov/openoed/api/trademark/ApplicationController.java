package gov.openoed.api.trademark;

import java.util.UUID;

import gov.openoed.api.audit.Audited;
import gov.openoed.api.auth.service.ActorService;
import gov.openoed.api.trademark.service.FilingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

	private final FilingService filings;
	private final ActorService actors;

	public record CreateRequest(
			@NotBlank @Size(max = 200) String mark,
			String markType,
			@Min(1) @Max(45) int internationalClass,
			@NotBlank @Size(max = 4000) String goodsDescription,
			@NotBlank @Size(max = 200) String ownerName,
			@NotBlank @Size(max = 500) String ownerAddress,
			@Email @NotBlank String ownerEmail,
			@Size(max = 200) String attorneyName,
			@Size(max = 32) String attorneyBarNumber) {
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@Audited(action = "APPLICATION_FILE")
	public ApplicationViews.ApplicationDetail file(@AuthenticationPrincipal Jwt jwt,
			@RequestPart("application") @Valid CreateRequest request,
			@RequestPart("specimen") MultipartFile specimen) throws java.io.IOException {
		return filings.file(actors.require(jwt), new FilingService.CreateCommand(
				request.mark(), request.markType(), request.internationalClass(), request.goodsDescription(),
				request.ownerName(), request.ownerAddress(), request.ownerEmail(), request.attorneyName(),
				request.attorneyBarNumber(), specimen.getBytes(), specimen.getContentType()));
	}

	@GetMapping
	public java.util.List<ApplicationViews.ApplicationSummary> list(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String band) {
		return filings.list(actors.require(jwt), q, status, band);
	}

	@GetMapping("/{id}")
	@Audited(action = "APPLICATION_VIEW")
	public ApplicationViews.ApplicationDetail get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return filings.get(actors.require(jwt), id);
	}

	@GetMapping("/{id}/specimen")
	public ResponseEntity<byte[]> specimen(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		FilingService.SpecimenBytes bytes = filings.specimen(actors.require(jwt), id);
		return ResponseEntity.ok().header("Content-Type", bytes.contentType()).body(bytes.bytes());
	}

	@PostMapping("/{id}/approve")
	@Audited(action = "APPLICATION_APPROVE")
	public ApplicationViews.ApplicationDetail approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return filings.approve(actors.require(jwt), id);
	}
}
