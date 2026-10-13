package gov.openoed.deadline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AbandonmentHandlerTest {

	private final UUID officeActionId = UUID.randomUUID();
	private final UUID applicationId = UUID.randomUUID();
	private final List<NoticeDraft> sent = new ArrayList<>();
	private final List<NoticeDraft> recorded = new ArrayList<>();

	@Test
	void abandonedApplicationSendsOneNoticeAndRecordsTheProviderId() {
		NoticeDraft draft = new NoticeDraft(officeActionId, applicationId, "ada@example.com",
				"Application abandoned", "No response was filed.");
		AbandonmentHandler handler = handler(new DeadlineDecision(DeadlineDecision.Outcome.ABANDONED, draft), "ses-1");
		assertThat(handler.handle(event())).isEqualTo(DeadlineDecision.Outcome.ABANDONED);
		assertThat(sent).hasSize(1);
		assertThat(recorded).singleElement().extracting(NoticeDraft::providerMessageId).isEqualTo("ses-1");
	}

	@Test
	void aTimelyResponseSendsNothing() {
		AbandonmentHandler handler = handler(DeadlineDecision.responded(), "unused");
		assertThat(handler.handle(event())).isEqualTo(DeadlineDecision.Outcome.ALREADY_RESPONDED);
		assertThat(sent).isEmpty();
		assertThat(recorded).isEmpty();
	}

	@Test
	void anAlreadyClosedDeadlineDoesNotSendASecondNotice() {
		AbandonmentHandler handler = handler(DeadlineDecision.closed(), "unused");
		assertThat(handler.handle(event())).isEqualTo(DeadlineDecision.Outcome.ALREADY_CLOSED);
		assertThat(sent).isEmpty();
	}

	@Test
	void missingEventIsRejected() {
		AbandonmentHandler handler = handler(DeadlineDecision.closed(), "unused");
		assertThatThrownBy(() -> handler.handle(null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> handler.handle(new DeadlineEvent(null, applicationId, "97", Instant.now())))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void abandonedDecisionWithoutANoticeIsRejected() {
		assertThatThrownBy(() -> new DeadlineDecision(DeadlineDecision.Outcome.ABANDONED, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private AbandonmentHandler handler(DeadlineDecision decision, String messageId) {
		return new AbandonmentHandler(new DeadlineApi() {
			@Override
			public DeadlineDecision evaluate(UUID id) {
				assertThat(id).isEqualTo(officeActionId);
				return decision;
			}

			@Override
			public void recordNotice(NoticeDraft notice) {
				recorded.add(notice);
			}
		}, notice -> {
			sent.add(notice);
			return messageId;
		});
	}

	private DeadlineEvent event() {
		return new DeadlineEvent(officeActionId, applicationId, "97000001", Instant.parse("2026-10-12T00:00:00Z"));
	}
}
