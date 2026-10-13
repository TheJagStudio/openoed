package gov.openoed.platform.schedule;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/** EventBridge Scheduler stand-in: one-shot schedules keyed by name. */
@Component
public class ScheduleStore {

	public record Schedule(String name, Instant fireAt, String payload, boolean fired, Instant firedAt) {
	}

	private final ConcurrentHashMap<String, Schedule> schedules = new ConcurrentHashMap<>();

	public Schedule upsert(String name, Instant fireAt, String payload) {
		Schedule created = new Schedule(name, fireAt, payload, false, null);
		schedules.put(name, created);
		return created;
	}

	public boolean delete(String name) {
		return schedules.remove(name) != null;
	}

	public Schedule get(String name) {
		return schedules.get(name);
	}

	public List<Schedule> all() {
		return List.copyOf(schedules.values());
	}

	public List<Schedule> due(Instant now) {
		List<Schedule> due = new ArrayList<>();
		for (Schedule schedule : schedules.values()) {
			if (!schedule.fired() && !schedule.fireAt().isAfter(now)) due.add(schedule);
		}
		return due;
	}

	public void markFired(String name, Instant when) {
		schedules.computeIfPresent(name, (k, s) -> new Schedule(s.name(), s.fireAt(), s.payload(), true, when));
	}
}
