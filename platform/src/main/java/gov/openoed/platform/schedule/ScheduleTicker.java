package gov.openoed.platform.schedule;

import java.time.Instant;

import gov.openoed.platform.lambda.LambdaRuntime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Fires due one-shot schedules by invoking the abandonment Lambda. Failures stay unfired and retry. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleTicker {

	private final ScheduleStore schedules;
	private final LambdaRuntime lambda;

	@Scheduled(fixedDelay = 1000)
	public void tick() {
		for (ScheduleStore.Schedule schedule : schedules.due(Instant.now())) {
			try {
				String outcome = lambda.invoke(lambda.eventFromPayload(schedule.payload()));
				schedules.markFired(schedule.name(), Instant.now());
				log.info("Schedule {} fired ({})", schedule.name(), outcome);
			} catch (RuntimeException ex) {
				log.warn("Schedule {} did not fire: {}", schedule.name(), ex.getMessage());
			}
		}
	}

	/** Used by the demo "fire now" control and by tests. */
	public String fireNow(String name) {
		ScheduleStore.Schedule schedule = schedules.get(name);
		if (schedule == null) throw new IllegalArgumentException("No schedule " + name);
		String outcome = lambda.invoke(lambda.eventFromPayload(schedule.payload()));
		schedules.markFired(name, Instant.now());
		return outcome;
	}
}
