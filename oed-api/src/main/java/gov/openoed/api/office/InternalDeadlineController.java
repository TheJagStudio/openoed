package gov.openoed.api.office;

import java.util.UUID;

import gov.openoed.api.office.service.OfficeActionService;
import gov.openoed.deadline.NoticeDraft;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Called by the abandonment Lambda. Authenticated with the internal token, not a user JWT. */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalDeadlineController {

	private final OfficeActionService actions;

	public record EvaluateRequest(UUID officeActionId) {
	}

	public record EvaluateResponse(String outcome, NoticeDraft notice) {
	}

	@PostMapping("/deadlines/evaluate")
	public EvaluateResponse evaluate(@RequestBody EvaluateRequest request) {
		OfficeActionService.DeadlineOutcome outcome = actions.evaluate(request.officeActionId());
		return new EvaluateResponse(outcome.outcome(), outcome.notice());
	}

	@PostMapping("/notices")
	public java.util.Map<String, String> notice(@RequestBody NoticeDraft draft) {
		actions.recordNotice(draft);
		return java.util.Map.of("result", "recorded");
	}
}
