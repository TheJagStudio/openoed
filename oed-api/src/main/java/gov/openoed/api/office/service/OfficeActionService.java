package gov.openoed.api.office.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.json.JsonMapper;
import gov.openoed.api.audit.AuditContext;
import gov.openoed.api.auth.Role;
import gov.openoed.api.auth.UserAccount;
import gov.openoed.api.common.ConflictException;
import gov.openoed.api.common.ForbiddenException;
import gov.openoed.api.common.NotFoundException;
import gov.openoed.api.config.AppProperties;
import gov.openoed.api.office.ApplicantResponse;
import gov.openoed.api.office.Notice;
import gov.openoed.api.office.NoticeRepository;
import gov.openoed.api.office.OfficeAction;
import gov.openoed.api.office.OfficeActionRepository;
import gov.openoed.api.office.ResponseRepository;
import gov.openoed.api.outbox.DomainOutbox;
import gov.openoed.api.outbox.DomainOutboxRepository;
import gov.openoed.api.platform.PlatformClient;
import gov.openoed.api.trademark.ApplicationStatus;
import gov.openoed.api.trademark.ApplicationViews.ApplicationDetail;
import gov.openoed.api.trademark.TrademarkApplication;
import gov.openoed.api.trademark.service.FilingService;
import gov.openoed.deadline.NoticeDraft;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfficeActionService {

	private final OfficeActionRepository officeActions;
	private final ResponseRepository responses;
	private final NoticeRepository notices;
	private final DomainOutboxRepository outbox;
	private final FilingService filings;
	private final PlatformClient platform;
	private final AppProperties props;
	private final JsonMapper json;
	private final Clock clock;

	@Transactional
	public ApplicationDetail issue(UserAccount actor, UUID applicationId, String type, String body) {
		if (actor.getRole() != Role.EXAMINER && actor.getRole() != Role.ADMIN) {
			throw new ForbiddenException("Only an examiner can issue an office action");
		}
		TrademarkApplication app = filings.loadVisible(actor, applicationId);
		if (app.getStatus() != ApplicationStatus.SUBMITTED && app.getStatus() != ApplicationStatus.UNDER_REVIEW
				&& app.getStatus() != ApplicationStatus.RESPONSE_FILED) {
			throw new ConflictException("An office action cannot be issued while the application is " + app.getStatus());
		}
		Instant now = clock.instant();
		OfficeAction action = new OfficeAction();
		action.setId(UUID.randomUUID());
		action.setApplicationId(app.getId());
		action.setExaminerId(actor.getId());
		action.setType(type == null || type.isBlank() ? "NONFINAL" : type);
		action.setBody(body.trim());
		action.setIssuedAt(now);
		action.setStatutoryDueAt(now.atZone(java.time.ZoneOffset.UTC).plusMonths(props.deadlines().statutoryMonths()).toInstant());
		action.setScheduleFireAt(now.plus(props.deadlines().schedulerDelay()));
		action.setScheduleName("oa-" + action.getId());
		action.setStatus("OPEN");
		officeActions.save(action);
		app.setStatus(ApplicationStatus.OFFICE_ACTION);
		app.setUpdatedAt(now);
		filings.event(app.getId(), actor.getId(), "OFFICE_ACTION",
				action.getType() + " office action issued. Statutory response due " + action.getStatutoryDueAt() + ".", now);
		enqueue(action, app);
		AuditContext.resource(app.getId().toString());
		AuditContext.put("detail", action.getScheduleName());
		return filings.detail(app, actor);
	}

	@Transactional
	public ApplicationDetail respond(UserAccount actor, UUID officeActionId, String body) {
		OfficeAction action = officeActions.findById(officeActionId).orElseThrow(() -> new NotFoundException("Office action not found"));
		TrademarkApplication app = filings.loadVisible(actor, action.getApplicationId());
		if (!actor.getId().equals(app.getOwnerId()) && actor.getRole() != Role.ADMIN) {
			throw new ForbiddenException("Only the applicant can respond");
		}
		if (!"OPEN".equals(action.getStatus())) throw new ConflictException("This office action is already " + action.getStatus());
		Instant now = clock.instant();
		ApplicantResponse response = new ApplicantResponse();
		response.setId(UUID.randomUUID());
		response.setOfficeActionId(action.getId());
		response.setApplicantId(actor.getId());
		response.setBody(body.trim());
		response.setSubmittedAt(now);
		responses.save(response);
		action.setStatus("RESPONDED");
		app.setStatus(ApplicationStatus.RESPONSE_FILED);
		app.setUpdatedAt(now);
		filings.event(app.getId(), actor.getId(), "RESPONSE", "Response filed before the statutory deadline.", now);
		try {
			platform.deleteSchedule(action.getScheduleName());
		} catch (RuntimeException ex) {
			log.info("Schedule {} will no-op when it fires: {}", action.getScheduleName(), ex.getMessage());
		}
		AuditContext.resource(app.getId().toString());
		return filings.detail(app, actor);
	}

	public record DeadlineOutcome(String outcome, NoticeDraft notice) {
	}

	@Transactional
	public DeadlineOutcome evaluate(UUID officeActionId) {
		OfficeAction action = officeActions.lock(officeActionId).orElseThrow(() -> new NotFoundException("Office action not found"));
		TrademarkApplication app = filings.loadVisible(systemActor(), action.getApplicationId());
		if (responses.existsByOfficeActionId(action.getId())) return new DeadlineOutcome("ALREADY_RESPONDED", null);
		if (notices.findByOfficeActionId(action.getId()).isPresent()) return new DeadlineOutcome("ALREADY_CLOSED", null);
		if (!"OPEN".equals(action.getStatus()) && app.getStatus() == ApplicationStatus.ABANDONED) {
			return new DeadlineOutcome("ABANDONED", draft(action, app));
		}
		if (!"OPEN".equals(action.getStatus())) return new DeadlineOutcome("ALREADY_CLOSED", null);
		Instant now = clock.instant();
		action.setStatus("EXPIRED");
		app.setStatus(ApplicationStatus.ABANDONED);
		app.setUpdatedAt(now);
		filings.event(app.getId(), null, "ABANDONED",
				"No response before the office-action deadline. Application abandoned.", now);
		return new DeadlineOutcome("ABANDONED", draft(action, app));
	}

	@Transactional
	public void recordNotice(NoticeDraft draft) {
		if (notices.findByOfficeActionId(draft.officeActionId()).isPresent()) return;
		Notice notice = new Notice();
		notice.setId(UUID.randomUUID());
		notice.setApplicationId(draft.applicationId());
		notice.setOfficeActionId(draft.officeActionId());
		notice.setToEmail(draft.toEmail());
		notice.setSubject(draft.subject());
		notice.setBody(draft.text());
		notice.setProviderMessageId(draft.providerMessageId());
		notice.setSentAt(clock.instant());
		notices.save(notice);
		filings.event(draft.applicationId(), null, "NOTICE", "Final abandonment notice sent to " + draft.toEmail() + ".", notice.getSentAt());
	}

	@Transactional
	public String simulate(UserAccount actor, UUID officeActionId) {
		if (!props.deadlines().simulationEnabled()) throw new ForbiddenException("Deadline simulation is disabled");
		if (actor.getRole() == Role.APPLICANT || actor.getRole() == Role.IG) {
			throw new ForbiddenException("You cannot fire a deadline");
		}
		OfficeAction action = officeActions.findById(officeActionId).orElseThrow(() -> new NotFoundException("Office action not found"));
		filings.loadVisible(actor, action.getApplicationId());
		try {
			return platform.fireSchedule(action.getScheduleName());
		} catch (RuntimeException ex) {
			throw new ConflictException("The scheduler has not picked up this office action yet. Try again in a moment.");
		}
	}

	private void enqueue(OfficeAction action, TrademarkApplication app) {
		try {
			String payload = json.writeValueAsString(java.util.Map.of(
					"officeActionId", action.getId(),
					"applicationId", app.getId(),
					"serialNumber", app.getSerialNumber(),
					"statutoryDueAt", action.getStatutoryDueAt().toString(),
					"fireAt", action.getScheduleFireAt().toString(),
					"scheduleName", action.getScheduleName()));
			DomainOutbox row = new DomainOutbox();
			row.setId(UUID.randomUUID());
			row.setType("OFFICE_ACTION_ISSUED");
			row.setPayload(payload);
			row.setCreatedAt(clock.instant());
			outbox.save(row);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static NoticeDraft draft(OfficeAction action, TrademarkApplication app) {
		String subject = "Trademark application " + app.getSerialNumber() + " abandoned";
		String text = "Application " + app.getSerialNumber() + " (" + app.getMark() + ") is abandoned. "
				+ "No response to the office action was received before the statutory deadline of " + action.getStatutoryDueAt() + ".";
		return new NoticeDraft(action.getId(), app.getId(), app.getOwnerEmail(), subject, text);
	}

	/** Internal calls have no browser user. Visibility checks are skipped by using an admin-shaped actor. */
	private UserAccount systemActor() {
		UserAccount actor = new UserAccount();
		actor.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
		actor.setRole(Role.ADMIN);
		return actor;
	}
}
