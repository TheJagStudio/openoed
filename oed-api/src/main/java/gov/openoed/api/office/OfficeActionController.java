package gov.openoed.api.office;

import java.util.List;
import java.util.UUID;

import gov.openoed.api.audit.Audited;
import gov.openoed.api.auth.Role;
import gov.openoed.api.auth.UserAccount;
import gov.openoed.api.auth.service.ActorService;
import gov.openoed.api.trademark.ApplicationRepository;
import gov.openoed.api.office.service.OfficeActionService;
import gov.openoed.api.trademark.ApplicationViews.ApplicationDetail;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OfficeActionController {

	private final OfficeActionService actions;
	private final OfficeActionRepository officeActions;
	private final NoticeRepository notices;
	private final ApplicationRepository applications;
	private final ActorService actors;

	public record IssueRequest(@NotBlank String type, @NotBlank @Size(max = 8000) String body) {
	}

	public record ResponseRequest(@NotBlank @Size(max = 8000) String body) {
	}

	public record DeadlineRow(UUID id, UUID applicationId, String scheduleName, String status,
			java.time.Instant statutoryDueAt, java.time.Instant scheduleFireAt) {
	}

	@PostMapping("/api/v1/applications/{id}/office-actions")
	@Audited(action = "OFFICE_ACTION_ISSUE")
	public ApplicationDetail issue(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody IssueRequest request) {
		return actions.issue(actors.require(jwt), id, request.type(), request.body());
	}

	@PostMapping("/api/v1/office-actions/{id}/responses")
	@Audited(action = "OFFICE_ACTION_RESPONSE")
	public ApplicationDetail respond(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody ResponseRequest request) {
		return actions.respond(actors.require(jwt), id, request.body());
	}

	@PostMapping("/api/v1/office-actions/{id}/simulate-deadline")
	@Audited(action = "DEADLINE_SIMULATE")
	public java.util.Map<String, String> simulate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return java.util.Map.of("outcome", actions.simulate(actors.require(jwt), id));
	}

	@GetMapping("/api/v1/deadlines")
	public List<DeadlineRow> deadlines(@AuthenticationPrincipal Jwt jwt) {
		UserAccount actor = actors.require(jwt);
		return officeActions.findByStatusOrderByScheduleFireAtAsc("OPEN").stream()
				.filter(o -> visible(actor, o.getApplicationId()))
				.map(o -> new DeadlineRow(o.getId(), o.getApplicationId(), o.getScheduleName(), o.getStatus(),
						o.getStatutoryDueAt(), o.getScheduleFireAt()))
				.toList();
	}

	@GetMapping("/api/v1/notices")
	public List<Notice> recentNotices(@AuthenticationPrincipal Jwt jwt) {
		UserAccount actor = actors.require(jwt);
		return notices.findTop50ByOrderBySentAtDesc().stream()
				.filter(n -> visible(actor, n.getApplicationId()))
				.toList();
	}

	private boolean visible(UserAccount actor, java.util.UUID applicationId) {
		if (actor.getRole() != Role.APPLICANT) return true;
		return applications.findById(applicationId).map(a -> actor.getId().equals(a.getOwnerId())).orElse(false);
	}
}
