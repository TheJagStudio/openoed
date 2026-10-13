package gov.openoed.api.trademark.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import gov.openoed.api.audit.AuditContext;
import gov.openoed.api.auth.Role;
import gov.openoed.api.auth.UserAccount;
import gov.openoed.api.common.BadRequestException;
import gov.openoed.api.common.ForbiddenException;
import gov.openoed.api.common.NotFoundException;
import gov.openoed.api.config.AppProperties;
import gov.openoed.api.fraud.NarrativeService;
import gov.openoed.api.fraud.RiskAssessment;
import gov.openoed.api.fraud.RiskRepository;
import gov.openoed.api.office.NoticeRepository;
import gov.openoed.api.office.OfficeActionRepository;
import gov.openoed.api.reference.ReferenceCatalog;
import gov.openoed.api.review.ReviewItem;
import gov.openoed.api.review.ReviewRepository;
import gov.openoed.api.trademark.ApplicationRepository;
import gov.openoed.api.trademark.ApplicationStatus;
import gov.openoed.api.trademark.ApplicationViews;
import gov.openoed.api.trademark.ApplicationViews.ApplicationDetail;
import gov.openoed.api.trademark.ApplicationViews.ApplicationSummary;
import gov.openoed.api.trademark.ProsecutionEvent;
import gov.openoed.api.trademark.ProsecutionRepository;
import gov.openoed.api.trademark.Specimen;
import gov.openoed.api.trademark.SpecimenFiles;
import gov.openoed.api.trademark.SpecimenRepository;
import gov.openoed.api.trademark.TrademarkApplication;
import gov.openoed.fraud.ApplicationFacts;
import gov.openoed.fraud.Assessment;
import gov.openoed.fraud.Finding;
import gov.openoed.fraud.FraudRules;
import gov.openoed.fraud.PriorFiling;
import gov.openoed.fraud.SpecimenAnalyzer;
import gov.openoed.fraud.SpecimenFacts;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class FilingService {

	private final ApplicationRepository applications;
	private final SpecimenRepository specimens;
	private final SpecimenFiles files;
	private final ProsecutionRepository events;
	private final RiskRepository risks;
	private final ReviewRepository reviews;
	private final OfficeActionRepository officeActions;
	private final NoticeRepository notices;
	private final ReferenceCatalog catalog;
	private final NarrativeService narrative;
	private final AppProperties props;
	private final JsonMapper json;
	private final Clock clock;
	private final FraudRules rules = new FraudRules();
	private final SpecimenAnalyzer analyzer = new SpecimenAnalyzer();

	public record CreateCommand(String mark, String markType, int internationalClass, String goodsDescription,
			String ownerName, String ownerAddress, String ownerEmail, String attorneyName, String attorneyBarNumber,
			byte[] specimen, String contentType) {
	}

	@Transactional
	public ApplicationDetail file(UserAccount actor, CreateCommand cmd) {
		if (actor.getRole() != Role.APPLICANT && actor.getRole() != Role.ADMIN) {
			throw new ForbiddenException("Only applicants can file a trademark application");
		}
		if (cmd.internationalClass() < 1 || cmd.internationalClass() > 45) throw new BadRequestException("Class must be 1–45");
		if (cmd.specimen() == null || cmd.specimen().length < 32) throw new BadRequestException("A specimen image is required");
		Instant now = clock.instant();
		TrademarkApplication app = new TrademarkApplication();
		app.setId(UUID.randomUUID());
		app.setSerialNumber(nextSerial());
		app.setOwnerId(actor.getId());
		app.setMark(cmd.mark().trim());
		app.setMarkType(cmd.markType() == null || cmd.markType().isBlank() ? "STANDARD" : cmd.markType());
		app.setInternationalClass(cmd.internationalClass());
		app.setArtUnit(artUnit(cmd.internationalClass()));
		app.setGoodsDescription(cmd.goodsDescription().trim());
		app.setOwnerName(cmd.ownerName().trim());
		app.setOwnerAddress(cmd.ownerAddress().trim());
		app.setOwnerEmail(cmd.ownerEmail().trim());
		app.setAttorneyName(blankToNull(cmd.attorneyName()));
		app.setAttorneyBarNumber(blankToNull(cmd.attorneyBarNumber()));
		app.setStatus(ApplicationStatus.SUBMITTED);
		app.setFiledAt(now);
		app.setUpdatedAt(now);
		SpecimenFacts facts = analyzer.analyze(cmd.specimen());
		Assessment assessment = rules.assess(toFacts(app, facts), catalog.data());
		app.setRiskScore(assessment.score());
		app.setRiskBand(assessment.band().name());
		applications.save(app);
		Specimen specimen = new Specimen();
		specimen.setId(UUID.randomUUID());
		specimen.setApplicationId(app.getId());
		specimen.setContentType(cmd.contentType() == null ? "application/octet-stream" : cmd.contentType());
		specimen.setByteSize(cmd.specimen().length);
		specimen.setSha256(facts.sha256() == null ? SpecimenAnalyzer.sha256(cmd.specimen()) : facts.sha256());
		specimen.setStorageKey(SpecimenFiles.keyFor(app.getId()));
		specimen.setWidth(facts.width());
		specimen.setHeight(facts.height());
		specimen.setExifPresent(facts.exifPresent());
		specimen.setJpeg(facts.jpeg());
		specimens.save(specimen);
		files.write(specimen.getStorageKey(), cmd.specimen());

		RiskAssessment row = new RiskAssessment();
		row.setId(UUID.randomUUID());
		row.setApplicationId(app.getId());
		row.setScore(assessment.score());
		row.setBand(assessment.band().name());
		row.setFindingsJson(writeFindings(assessment.findings()));
		row.setClaudeStatus(props.claude().active() ? "PENDING" : "DISABLED");
		row.setCreatedAt(now);
		risks.save(row);
		if (assessment.band().priorityQueue()) openReview(app, now);
		event(app.getId(), actor.getId(), "FILED", "Application filed. Risk " + assessment.score() + " " + assessment.band() + ".", now);
		AuditContext.resource(app.getId().toString());
		AuditContext.put("detail", app.getSerialNumber() + " score " + assessment.score());
		UUID id = app.getId();
		if (props.claude().active()) afterCommit(() -> narrative.narrate(id));
		return detail(app, actor);
	}

	@Transactional(readOnly = true)
	public List<ApplicationSummary> list(UserAccount actor, String q, String status, String band) {
		UUID owner = actor.getRole() == Role.APPLICANT ? actor.getId() : null;
		ApplicationStatus parsed = status == null || status.isBlank() ? null : ApplicationStatus.valueOf(status);
		String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
		String bandFilter = band == null || band.isBlank() ? null : band;
		return applications.search(owner, parsed, bandFilter, like, PageRequest.of(0, 100)).stream()
				.map(ApplicationSummary::of).toList();
	}

	@Transactional(readOnly = true)
	public ApplicationDetail get(UserAccount actor, UUID id) {
		TrademarkApplication app = loadVisible(actor, id);
		if (actor.getRole() == Role.EXAMINER && actor.getArtUnit() != null && !actor.getArtUnit().equals(app.getArtUnit())) {
			AuditContext.anomaly("ART_UNIT_MISMATCH");
			AuditContext.put("detail", "Examiner art unit " + actor.getArtUnit() + " opened " + app.getArtUnit() + " serial " + app.getSerialNumber());
		}
		AuditContext.resource(id.toString());
		return detail(app, actor);
	}

	@Transactional(readOnly = true)
	public SpecimenBytes specimen(UserAccount actor, UUID id) {
		TrademarkApplication app = loadVisible(actor, id);
		Specimen specimen = specimens.findByApplicationId(app.getId()).orElseThrow(() -> new NotFoundException("No specimen"));
		return new SpecimenBytes(files.read(specimen.getStorageKey()), specimen.getContentType());
	}

	@Transactional
	public ApplicationDetail approve(UserAccount actor, UUID id) {
		if (actor.getRole() != Role.EXAMINER && actor.getRole() != Role.ADMIN) {
			throw new ForbiddenException("Only an examiner can approve an application");
		}
		TrademarkApplication app = applications.findById(id).orElseThrow(() -> new NotFoundException("Application not found"));
		if (app.getStatus() != ApplicationStatus.SUBMITTED && app.getStatus() != ApplicationStatus.RESPONSE_FILED
				&& app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
			throw new gov.openoed.api.common.ConflictException("This application cannot be approved from " + app.getStatus());
		}
		app.setStatus(ApplicationStatus.APPROVED);
		app.setUpdatedAt(clock.instant());
		event(app.getId(), actor.getId(), "APPROVED", "Approved for publication.", clock.instant());
		AuditContext.resource(id.toString());
		return detail(app, actor);
	}

	public record SpecimenBytes(byte[] bytes, String contentType) {
	}

	public TrademarkApplication loadVisible(UserAccount actor, UUID id) {
		TrademarkApplication app = applications.findById(id).orElseThrow(() -> new NotFoundException("Application not found"));
		if (actor.getRole() == Role.APPLICANT && !actor.getId().equals(app.getOwnerId())) {
			throw new NotFoundException("Application not found");
		}
		return app;
	}

	public void event(UUID applicationId, UUID actorId, String kind, String detail, Instant at) {
		ProsecutionEvent event = new ProsecutionEvent();
		event.setId(UUID.randomUUID());
		event.setApplicationId(applicationId);
		event.setActorId(actorId);
		event.setKind(kind);
		event.setDetail(detail);
		event.setAt(at);
		events.save(event);
	}

	public void openReview(TrademarkApplication app, Instant now) {
		if (reviews.findByApplicationId(app.getId()).isPresent()) return;
		ReviewItem item = new ReviewItem();
		item.setId(UUID.randomUUID());
		item.setApplicationId(app.getId());
		item.setStatus("OPEN");
		item.setPriority(app.getRiskScore() == null ? 0 : app.getRiskScore());
		item.setOpenedAt(now);
		reviews.save(item);
		event(app.getId(), null, "QUEUED", "Routed to the OED priority review queue.", now);
	}

	public ApplicationDetail detail(TrademarkApplication app, UserAccount actor) {
		RiskAssessment risk = risks.findByApplicationId(app.getId()).orElse(null);
		ReviewItem review = reviews.findByApplicationId(app.getId()).orElse(null);
		List<Finding> findings = List.of();
		if (risk != null) {
			try {
				findings = json.readValue(risk.getFindingsJson(), new TypeReference<>() {
				});
			} catch (Exception ignored) {
				findings = List.of();
			}
		}
		List<String> flags = List.of();
		if (risk != null && risk.getClaudeFlagsJson() != null) {
			try {
				flags = json.readValue(risk.getClaudeFlagsJson(), new TypeReference<>() {
				});
			} catch (Exception ignored) {
				flags = List.of();
			}
		}
		boolean owner = actor.getId().equals(app.getOwnerId());
		boolean examiner = actor.getRole() == Role.EXAMINER || actor.getRole() == Role.ADMIN;
		boolean oed = actor.getRole() == Role.OED || actor.getRole() == Role.ADMIN;
		boolean openAction = officeActions.findByApplicationIdOrderByIssuedAtDesc(app.getId()).stream()
				.anyMatch(o -> "OPEN".equals(o.getStatus()));
		return new ApplicationDetail(
				app.getId(), app.getSerialNumber(), app.getMark(), app.getMarkType(), app.getInternationalClass(),
				app.getArtUnit(), app.getGoodsDescription(), app.getOwnerName(), app.getOwnerAddress(), app.getOwnerEmail(),
				app.getAttorneyName(), app.getAttorneyBarNumber(), app.getStatus().name(), app.getRiskScore(), app.getRiskBand(),
				app.getFiledAt(),
				findings.stream().map(ApplicationViews.FindingView::of).toList(),
				risk == null ? null : new ApplicationViews.ClaudeView(risk.getClaudeStatus(), risk.getClaudeSummary(), risk.getClaudeSuspicion(), flags),
				review == null ? null : new ApplicationViews.ReviewView(review.getId(), review.getStatus(), review.getPriority(), review.getNotes(), review.getOpenedAt(), review.getClosedAt()),
				officeActions.findByApplicationIdOrderByIssuedAtDesc(app.getId()).stream()
						.map(o -> new ApplicationViews.OfficeActionView(o.getId(), o.getType(), o.getBody(), o.getStatus(),
								o.getIssuedAt(), o.getStatutoryDueAt(), o.getScheduleFireAt(), o.getScheduleName()))
						.toList(),
				notices.findByApplicationIdOrderBySentAtDesc(app.getId()).stream()
						.map(n -> new ApplicationViews.NoticeView(n.getToEmail(), n.getSubject(), n.getBody(), n.getProviderMessageId(), n.getSentAt()))
						.toList(),
				events.findByApplicationIdOrderByAtAsc(app.getId()).stream()
						.map(e -> new ApplicationViews.TimelineView(e.getKind(), e.getDetail(), e.getAt())).toList(),
				owner && openAction,
				examiner && (app.getStatus() == ApplicationStatus.SUBMITTED || app.getStatus() == ApplicationStatus.UNDER_REVIEW
						|| app.getStatus() == ApplicationStatus.RESPONSE_FILED),
				oed && review != null && ("OPEN".equals(review.getStatus()) || "ASSIGNED".equals(review.getStatus())),
				examiner && (app.getStatus() == ApplicationStatus.SUBMITTED || app.getStatus() == ApplicationStatus.RESPONSE_FILED
						|| app.getStatus() == ApplicationStatus.UNDER_REVIEW));
	}

	private ApplicationFacts toFacts(TrademarkApplication app, SpecimenFacts specimen) {
		List<PriorFiling> history = applications.findTop300ByOrderByFiledAtDesc().stream()
				.filter(other -> !other.getId().equals(app.getId()))
				.map(other -> new PriorFiling(other.getSerialNumber(), other.getOwnerEmail(), other.getOwnerAddress(),
						specimens.findByApplicationId(other.getId()).map(Specimen::getSha256).orElse(null)))
				.toList();
		return new ApplicationFacts(app.getSerialNumber(), app.getMark(), app.getInternationalClass(), app.getGoodsDescription(),
				app.getOwnerName(), app.getOwnerAddress(), app.getOwnerEmail(), app.getAttorneyName(), app.getAttorneyBarNumber(),
				specimen, history);
	}

	private String writeFindings(List<Finding> findings) {
		try {
			return json.writeValueAsString(findings);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private String nextSerial() {
		for (int i = 0; i < 8; i++) {
			String serial = "97" + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
			if (!applications.existsBySerialNumber(serial)) return serial;
		}
		throw new IllegalStateException("Could not allocate a serial number");
	}

	public static String artUnit(int internationalClass) {
		if (internationalClass <= 15) return "L1";
		if (internationalClass <= 30) return "L2";
		return "L3";
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static void afterCommit(Runnable action) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					action.run();
				}
			});
		} else {
			action.run();
		}
	}
}
