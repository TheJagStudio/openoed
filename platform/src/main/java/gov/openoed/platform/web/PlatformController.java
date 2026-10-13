package gov.openoed.platform.web;

import java.time.Instant;
import java.util.Map;

import gov.openoed.platform.bridge.OfficeActionBridge;
import gov.openoed.platform.lambda.LambdaRuntime;
import gov.openoed.platform.ledger.AppendOnlyLedger;
import gov.openoed.platform.mail.Mailbox;
import gov.openoed.platform.schedule.ScheduleStore;
import gov.openoed.platform.schedule.ScheduleTicker;
import gov.openoed.platform.sqs.SqsQueues;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class PlatformController {

	private final SqsQueues queues;
	private final ScheduleStore schedules;
	private final ScheduleTicker ticker;
	private final Mailbox mailbox;
	private final AppendOnlyLedger ledger;
	private final LambdaRuntime lambda;

	@GetMapping("/status")
	public Map<String, Object> status() {
		return Map.of(
				"queueDepth", queues.depth(OfficeActionBridge.QUEUE),
				"inflight", queues.inflight(),
				"schedules", schedules.all().size(),
				"emails", mailbox.all().size(),
				"ledgerItems", ledger.size());
	}

	@PostMapping("/sqs/queues/{name}/messages")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public SqsQueues.Message send(@PathVariable String name, @RequestBody Map<String, String> body) {
		String payload = body.get("body");
		if (payload == null || payload.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
		return queues.send(name, payload);
	}

	@GetMapping("/sqs/queues/{name}/messages")
	public java.util.List<SqsQueues.Message> receive(@PathVariable String name, @RequestParam(defaultValue = "1") int max) {
		return queues.receive(name, max);
	}

	@DeleteMapping("/sqs/queues/{name}/messages/{receipt}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMessage(@PathVariable String name, @PathVariable String receipt) {
		queues.delete(name, receipt);
	}

	public record ScheduleRequest(String name, Instant fireAt, String payload) {
	}

	@PostMapping("/schedules")
	@ResponseStatus(HttpStatus.CREATED)
	public ScheduleStore.Schedule create(@RequestBody ScheduleRequest request) {
		if (request.name() == null || request.fireAt() == null || request.payload() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name, fireAt and payload are required");
		}
		return schedules.upsert(request.name(), request.fireAt(), request.payload());
	}

	@GetMapping("/schedules")
	public java.util.List<ScheduleStore.Schedule> listSchedules() {
		return schedules.all();
	}

	@DeleteMapping("/schedules/{name}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteSchedule(@PathVariable String name) {
		schedules.delete(name);
	}

	@PostMapping("/schedules/{name}/fire")
	public Map<String, String> fire(@PathVariable String name) {
		try {
			return Map.of("outcome", ticker.fireNow(name));
		} catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
		}
	}

	public record EmailRequest(String to, String subject, String text) {
	}

	@PostMapping("/ses/emails")
	@ResponseStatus(HttpStatus.CREATED)
	public Mailbox.Email sendEmail(@RequestBody EmailRequest request) {
		return mailbox.send(request.to(), request.subject(), request.text());
	}

	@GetMapping("/ses/emails")
	public java.util.List<Mailbox.Email> emails() {
		return mailbox.all();
	}

	public record LedgerPut(String pk, String sk, Map<String, Object> attributes) {
	}

	@PutMapping("/dynamodb/tables/{table}/items")
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, String> putItem(@PathVariable String table, @RequestBody LedgerPut body) {
		if (!AppendOnlyLedger.TABLE.equals(table)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown table");
		try {
			ledger.put(body.pk(), body.sk(), body.attributes());
		} catch (AppendOnlyLedger.DuplicateItemException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
		} catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
		}
		return Map.of("result", "appended");
	}

	public record LedgerQuery(String pk, Boolean anomaly, Integer limit) {
	}

	@PostMapping("/dynamodb/tables/{table}/query")
	public java.util.List<Map<String, Object>> query(@PathVariable String table, @RequestBody LedgerQuery body) {
		if (!AppendOnlyLedger.TABLE.equals(table)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown table");
		return ledger.query(body.pk(), body.anomaly(), body.limit() == null ? 100 : body.limit());
	}

	@DeleteMapping("/dynamodb/tables/{table}/items")
	public void deleteItem(@PathVariable String table) {
		throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "The audit ledger is append-only");
	}

	@PostMapping("/lambda/abandonment")
	public Map<String, String> abandon(@RequestBody String payload) {
		return Map.of("outcome", lambda.invoke(lambda.eventFromPayload(payload)));
	}
}
