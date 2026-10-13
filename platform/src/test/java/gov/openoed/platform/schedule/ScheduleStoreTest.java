package gov.openoed.platform.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ScheduleStoreTest {

	@Test
	void dueSchedulesExcludeFiredAndFuture() {
		ScheduleStore store = new ScheduleStore();
		Instant now = Instant.parse("2026-10-12T12:00:00Z");
		store.upsert("past", now.minusSeconds(5), "{}");
		store.upsert("future", now.plusSeconds(60), "{}");
		store.upsert("done", now.minusSeconds(1), "{}");
		store.markFired("done", now);
		assertThat(store.due(now)).extracting(ScheduleStore.Schedule::name).containsExactly("past");
		assertThat(store.delete("future")).isTrue();
		assertThat(store.get("future")).isNull();
	}
}
