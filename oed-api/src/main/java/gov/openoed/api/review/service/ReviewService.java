package gov.openoed.api.review.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import gov.openoed.api.audit.AuditContext;
import gov.openoed.api.auth.Role;
import gov.openoed.api.auth.UserAccount;
import gov.openoed.api.common.BadRequestException;
import gov.openoed.api.common.ConflictException;
import gov.openoed.api.common.ForbiddenException;
import gov.openoed.api.common.NotFoundException;
import gov.openoed.api.review.ReviewItem;
import gov.openoed.api.review.ReviewRepository;
import gov.openoed.api.trademark.ApplicationStatus;
import gov.openoed.api.trademark.ApplicationViews.ApplicationDetail;
import gov.openoed.api.trademark.ApplicationViews.ApplicationSummary;
import gov.openoed.api.trademark.TrademarkApplication;
import gov.openoed.api.trademark.service.FilingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

	private final ReviewRepository reviews;
	private final FilingService filings;
	private final Clock clock;

	public record QueueRow(UUID reviewId, String reviewStatus, int priority, ApplicationSummary application) {
	}

	@Transactional(readOnly = true)
	public List<QueueRow> queue(UserAccount actor, String status) {
		requireOed(actor);
		String wanted = status == null || status.isBlank() ? "OPEN" : status;
		return reviews.findByStatusOrderByPriorityDescOpenedAtAsc(wanted).stream().map(item -> {
			TrademarkApplication app = filings.loadVisible(actor, item.getApplicationId());
			return new QueueRow(item.getId(), item.getStatus(), item.getPriority(), ApplicationSummary.of(app));
		}).toList();
	}

	@Transactional
	public ApplicationDetail decide(UserAccount actor, UUID applicationId, String decision, String notes) {
		requireOed(actor);
		ReviewItem item = reviews.findByApplicationId(applicationId).orElseThrow(() -> new NotFoundException("This filing is not in the review queue"));
		if ("CLEARED".equals(item.getStatus()) || "CONFIRMED".equals(item.getStatus())) {
			throw new ConflictException("This review is already " + item.getStatus());
		}
		TrademarkApplication app = filings.loadVisible(actor, applicationId);
		String normalized = decision == null ? "" : decision.trim().toUpperCase();
		item.setNotes(notes);
		item.setAssigneeId(actor.getId());
		if ("CONFIRM".equals(normalized)) {
			item.setStatus("CONFIRMED");
			item.setClosedAt(clock.instant());
			app.setStatus(ApplicationStatus.REFUSED);
			app.setUpdatedAt(clock.instant());
			filings.event(app.getId(), actor.getId(), "FRAUD_CONFIRMED", notes == null ? "Fraud confirmed by OED." : notes, clock.instant());
		} else if ("CLEAR".equals(normalized)) {
			item.setStatus("CLEARED");
			item.setClosedAt(clock.instant());
			filings.event(app.getId(), actor.getId(), "CLEARED", notes == null ? "Cleared as a false positive." : notes, clock.instant());
		} else if ("ASSIGN".equals(normalized)) {
			item.setStatus("ASSIGNED");
			filings.event(app.getId(), actor.getId(), "ASSIGNED", "Assigned for manual review.", clock.instant());
		} else {
			throw new BadRequestException("Decision must be CONFIRM, CLEAR or ASSIGN");
		}
		AuditContext.resource(app.getId().toString());
		AuditContext.put("detail", normalized);
		return filings.detail(app, actor);
	}

	private static void requireOed(UserAccount actor) {
		if (actor.getRole() != Role.OED && actor.getRole() != Role.ADMIN) {
			throw new ForbiddenException("The review queue is limited to OED");
		}
	}
}
