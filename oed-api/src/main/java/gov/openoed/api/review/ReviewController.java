package gov.openoed.api.review;

import java.util.UUID;

import gov.openoed.api.audit.Audited;
import gov.openoed.api.auth.service.ActorService;
import gov.openoed.api.review.service.ReviewService;
import gov.openoed.api.trademark.ApplicationViews.ApplicationDetail;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService reviews;
	private final ActorService actors;

	public record DecisionRequest(@NotBlank String decision, String notes) {
	}

	@GetMapping
	public java.util.List<ReviewService.QueueRow> queue(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String status) {
		return reviews.queue(actors.require(jwt), status);
	}

	@PostMapping("/{applicationId}")
	@Audited(action = "REVIEW_DECIDE")
	public ApplicationDetail decide(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId, @RequestBody DecisionRequest request) {
		return reviews.decide(actors.require(jwt), applicationId, request.decision(), request.notes());
	}
}
