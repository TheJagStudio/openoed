package gov.openoed.api.trademark;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import gov.openoed.fraud.Finding;

public final class ApplicationViews {

	private ApplicationViews() {
	}

	public record ApplicationSummary(
			UUID id, String serialNumber, String mark, int internationalClass, String artUnit,
			String status, Integer riskScore, String riskBand, String ownerName, Instant filedAt) {
		public static ApplicationSummary of(TrademarkApplication a) {
			return new ApplicationSummary(a.getId(), a.getSerialNumber(), a.getMark(), a.getInternationalClass(),
					a.getArtUnit(), a.getStatus().name(), a.getRiskScore(), a.getRiskBand(), a.getOwnerName(), a.getFiledAt());
		}
	}

	public record FindingView(String code, String title, String detail, int weight, String severity) {
		public static FindingView of(Finding f) {
			return new FindingView(f.code(), f.title(), f.detail(), f.weight(), f.severity());
		}
	}

	public record ClaudeView(String status, String summary, String suspicion, List<String> flags) {
	}

	public record ReviewView(UUID id, String status, int priority, String notes, Instant openedAt, Instant closedAt) {
	}

	public record OfficeActionView(UUID id, String type, String body, String status, Instant issuedAt,
			Instant statutoryDueAt, Instant scheduleFireAt, String scheduleName) {
	}

	public record NoticeView(String toEmail, String subject, String body, String providerMessageId, Instant sentAt) {
	}

	public record TimelineView(String kind, String detail, Instant at) {
	}

	public record ApplicationDetail(
			UUID id, String serialNumber, String mark, String markType, int internationalClass, String artUnit,
			String goodsDescription, String ownerName, String ownerAddress, String ownerEmail,
			String attorneyName, String attorneyBarNumber, String status, Integer riskScore, String riskBand,
			Instant filedAt, List<FindingView> findings, ClaudeView claude, ReviewView review,
			List<OfficeActionView> officeActions, List<NoticeView> notices, List<TimelineView> timeline,
			boolean canRespond, boolean canIssueOfficeAction, boolean canReview, boolean canApprove) {
	}
}
