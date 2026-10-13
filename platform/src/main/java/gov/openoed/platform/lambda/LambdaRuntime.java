package gov.openoed.platform.lambda;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import gov.openoed.deadline.AbandonmentHandler;
import gov.openoed.deadline.DeadlineApi;
import gov.openoed.deadline.DeadlineDecision;
import gov.openoed.deadline.DeadlineEvent;
import gov.openoed.deadline.NoticeDraft;
import gov.openoed.platform.PlatformProperties;
import gov.openoed.platform.mail.Mailbox;

import org.springframework.stereotype.Service;

/** Invokes the abandonment Lambda in-process and talks to the filing API over HTTP. */
@Service
public class LambdaRuntime {

	private final AbandonmentHandler handler;
	private final JsonMapper json;

	public LambdaRuntime(PlatformProperties props, Mailbox mailbox, JsonMapper json) {
		this.json = json;
		HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
		DeadlineApi api = new HttpDeadlineApi(http, json, props);
		this.handler = new AbandonmentHandler(api, notice -> mailbox.send(notice.toEmail(), notice.subject(), notice.text()).messageId());
	}

	public String invoke(DeadlineEvent event) {
		return handler.handle(event).name();
	}

	public DeadlineEvent eventFromPayload(String payload) {
		try {
			JsonNode node = json.readTree(payload);
			return new DeadlineEvent(
					UUID.fromString(node.path("officeActionId").asText()),
					node.path("applicationId").asText(null) == null || node.path("applicationId").asText().isBlank()
							? null : UUID.fromString(node.path("applicationId").asText()),
					node.path("serialNumber").asText(null),
					node.path("statutoryDueAt").asText(null) == null || node.path("statutoryDueAt").asText().isBlank()
							? null : Instant.parse(node.path("statutoryDueAt").asText()));
		} catch (Exception e) {
			throw new IllegalArgumentException("Unreadable schedule payload", e);
		}
	}

	private record HttpDeadlineApi(HttpClient http, JsonMapper json, PlatformProperties props) implements DeadlineApi {

		@Override
		public DeadlineDecision evaluate(UUID officeActionId) {
			JsonNode node = post("/internal/v1/deadlines/evaluate", "{\"officeActionId\":\"" + officeActionId + "\"}");
			String outcome = node.path("outcome").asText();
			if (!"ABANDONED".equals(outcome)) {
				return "ALREADY_RESPONDED".equals(outcome) ? DeadlineDecision.responded() : DeadlineDecision.closed();
			}
			JsonNode notice = node.path("notice");
			return new DeadlineDecision(DeadlineDecision.Outcome.ABANDONED, new NoticeDraft(
					UUID.fromString(notice.path("officeActionId").asText()),
					UUID.fromString(notice.path("applicationId").asText()),
					notice.path("toEmail").asText(),
					notice.path("subject").asText(),
					notice.path("text").asText()));
		}

		@Override
		public void recordNotice(NoticeDraft notice) {
			try {
				post("/internal/v1/notices", json.writeValueAsString(notice));
			} catch (Exception e) {
				throw new IllegalStateException("Could not record the notice", e);
			}
		}

		private JsonNode post(String path, String body) {
			try {
				HttpRequest request = HttpRequest.newBuilder(URI.create(props.apiBaseUrl() + path))
						.timeout(Duration.ofSeconds(8))
						.header("Content-Type", "application/json")
						.header("X-Internal-Token", props.internalToken())
						.POST(HttpRequest.BodyPublishers.ofString(body))
						.build();
				HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() / 100 != 2) {
					throw new IllegalStateException("API " + path + " returned " + response.statusCode() + " " + response.body());
				}
				return json.readTree(response.body());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			} catch (IllegalStateException e) {
				throw e;
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}
}
