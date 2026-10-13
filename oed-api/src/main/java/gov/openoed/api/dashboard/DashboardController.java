package gov.openoed.api.dashboard;

import java.util.List;
import java.util.Map;

import gov.openoed.api.auth.Role;
import gov.openoed.api.auth.UserAccount;
import gov.openoed.api.auth.service.ActorService;
import gov.openoed.api.config.AppProperties;
import gov.openoed.api.office.NoticeRepository;
import gov.openoed.api.office.OfficeActionRepository;
import gov.openoed.api.outbox.AuditRecorder;
import gov.openoed.api.platform.PlatformClient;
import gov.openoed.api.reference.ReferenceCatalog;
import gov.openoed.api.reference.ShellAddress;
import gov.openoed.api.review.ReviewRepository;
import gov.openoed.api.trademark.ApplicationRepository;
import gov.openoed.api.trademark.ApplicationStatus;
import gov.openoed.api.trademark.ApplicationViews.ApplicationSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

	private final ApplicationRepository applications;
	private final ReviewRepository reviews;
	private final OfficeActionRepository officeActions;
	private final NoticeRepository notices;
	private final AuditRecorder audit;
	private final PlatformClient platform;
	private final AppProperties props;
	private final ReferenceCatalog catalog;
	private final ActorService actors;

	public record PublicStats(long applications, long priorityReviews, long abandoned, long notices) {
	}

	public record PublicStatus(String serialNumber, String mark, String status, java.time.Instant filedAt) {
	}

	@GetMapping("/api/v1/public/stats")
	public PublicStats stats() {
		return new PublicStats(applications.count(), reviews.countByStatus("OPEN"),
				applications.countByStatus(ApplicationStatus.ABANDONED), notices.count());
	}

	@GetMapping("/api/v1/public/status/{serial}")
	public PublicStatus status(@org.springframework.web.bind.annotation.PathVariable String serial) {
		var app = applications.findBySerialNumber(serial).orElseThrow(() -> new gov.openoed.api.common.NotFoundException("No application with that serial"));
		return new PublicStatus(app.getSerialNumber(), app.getMark(), app.getStatus().name(), app.getFiledAt());
	}

	@GetMapping("/api/v1/dashboard")
	public Map<String, Object> dashboard(@AuthenticationPrincipal Jwt jwt) {
		UserAccount actor = actors.require(jwt);
		List<ApplicationSummary> recent = applications.findTop300ByOrderByFiledAtDesc().stream()
				.filter(a -> actor.getRole() != Role.APPLICANT || actor.getId().equals(a.getOwnerId()))
				.limit(8)
				.map(ApplicationSummary::of)
				.toList();
		long anomalies = audit.search(2, null, null, true).size();
		return Map.of(
				"openReviews", reviews.countByStatus("OPEN"),
				"highRisk", applications.countByRiskBandIn(List.of("HIGH", "CRITICAL")),
				"openDeadlines", officeActions.countByStatus("OPEN"),
				"anomalies", anomalies,
				"recent", recent,
				"role", actor.getRole().name());
	}

	@GetMapping("/api/v1/audit")
	@PreAuthorize("hasAnyRole('IG','ADMIN','OED')")
	public List<Map<String, Object>> audit(@RequestParam(defaultValue = "14") int days,
			@RequestParam(required = false) String action,
			@RequestParam(required = false) String actor,
			@RequestParam(required = false) Boolean anomaly) {
		return audit.search(days, action, actor, anomaly);
	}

	@GetMapping("/api/v1/system")
	public Map<String, Object> system(@AuthenticationPrincipal Jwt jwt) {
		actors.require(jwt);
		Map<String, Object> platformStatus;
		boolean up;
		try {
			platformStatus = platform.status();
			up = true;
		} catch (RuntimeException ex) {
			platformStatus = Map.of("error", "unreachable");
			up = false;
		}
		return Map.of(
				"claudeEnabled", props.claude().active(),
				"simulationEnabled", props.deadlines().simulationEnabled(),
				"statutoryMonths", props.deadlines().statutoryMonths(),
				"schedulerDelay", props.deadlines().schedulerDelay().toString(),
				"platformUp", up,
				"platform", platformStatus);
	}

	@GetMapping("/api/v1/admin/shell-addresses")
	@PreAuthorize("hasAnyRole('ADMIN','OED')")
	public List<ShellAddress> shells() {
		return catalog.shellAddresses();
	}

	public record ShellRequest(String address, String note) {
	}

	@PostMapping("/api/v1/admin/shell-addresses")
	@PreAuthorize("hasRole('ADMIN')")
	public ShellAddress addShell(@RequestBody ShellRequest request) {
		return catalog.addShell(request.address(), request.note());
	}
}
