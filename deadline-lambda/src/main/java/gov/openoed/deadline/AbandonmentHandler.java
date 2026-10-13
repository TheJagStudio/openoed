package gov.openoed.deadline;

/**
 * Lambda entry point. If the applicant never filed a response, the API has already marked the
 * application Abandoned; this handler sends the final notice and records the SES message id.
 * A response, or a notice that was already sent, is a no-op so retries stay safe.
 */
public final class AbandonmentHandler {

	private final DeadlineApi api;
	private final Mailer mailer;

	public AbandonmentHandler(DeadlineApi api, Mailer mailer) {
		this.api = api;
		this.mailer = mailer;
	}

	public DeadlineDecision.Outcome handle(DeadlineEvent event) {
		if (event == null || event.officeActionId() == null) {
			throw new IllegalArgumentException("officeActionId is required");
		}
		DeadlineDecision decision = api.evaluate(event.officeActionId());
		if (decision.outcome() != DeadlineDecision.Outcome.ABANDONED) {
			return decision.outcome();
		}
		String messageId = mailer.send(decision.notice());
		api.recordNotice(decision.notice().withMessageId(messageId));
		return DeadlineDecision.Outcome.ABANDONED;
	}
}
