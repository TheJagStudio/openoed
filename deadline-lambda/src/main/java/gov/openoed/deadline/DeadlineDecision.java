package gov.openoed.deadline;

/** What the API decided when the timer fired. */
public record DeadlineDecision(Outcome outcome, NoticeDraft notice) {

	public enum Outcome {
		/** No response was on file. The application is now Abandoned and a notice should go out. */
		ABANDONED,
		/** A response payload arrived before the timer. */
		ALREADY_RESPONDED,
		/** Already abandoned and the notice was already recorded. */
		ALREADY_CLOSED
	}

	public DeadlineDecision {
		if (outcome == null) throw new IllegalArgumentException("outcome");
		if (outcome == Outcome.ABANDONED && notice == null) throw new IllegalArgumentException("notice required");
	}

	public static DeadlineDecision responded() {
		return new DeadlineDecision(Outcome.ALREADY_RESPONDED, null);
	}

	public static DeadlineDecision closed() {
		return new DeadlineDecision(Outcome.ALREADY_CLOSED, null);
	}
}
