package gov.openoed.deadline;

import java.util.UUID;

/** The filing API, as the Lambda sees it. The platform supplies an HTTP implementation. */
public interface DeadlineApi {

	DeadlineDecision evaluate(UUID officeActionId);

	void recordNotice(NoticeDraft notice);
}
