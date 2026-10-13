package gov.openoed.api.fraud;

import java.util.List;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import gov.openoed.api.config.AppProperties;
import gov.openoed.fraud.Assessment;
import gov.openoed.fraud.Finding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Calls Claude when a key is configured and stores the narrative on the assessment row. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NarrativeService {

	private final RiskRepository risks;
	private final ClaudeClient claude;
	private final AppProperties props;
	private final JsonMapper json;

	@Async
	@Transactional
	public void narrate(java.util.UUID applicationId) {
		if (!props.claude().active()) return;
		RiskAssessment row = risks.findByApplicationId(applicationId).orElse(null);
		if (row == null || !"PENDING".equals(row.getClaudeStatus())) return;
		try {
			List<Finding> findings = json.readValue(row.getFindingsJson(), new TypeReference<>() {
			});
			ClaudeClient.ClaudeReview review = claude.review(new Assessment(row.getScore(), null, findings));
			row.setClaudeStatus("COMPLETE");
			row.setClaudeSummary(review.summary());
			row.setClaudeSuspicion(review.suspicion());
			row.setClaudeFlagsJson(json.writeValueAsString(review.flags()));
		} catch (Exception ex) {
			log.warn("Claude review failed for {}: {}", applicationId, ex.getMessage());
			row.setClaudeStatus("FAILED");
			row.setClaudeSummary("The rules score stands. Claude could not complete a narrative for this filing.");
		}
	}
}
