package gov.openoed.api.fraud;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import gov.openoed.api.config.AppProperties;
import gov.openoed.fraud.Assessment;
import gov.openoed.fraud.Finding;
import org.springframework.stereotype.Component;

/** Anthropic Messages API. Rules always run first; this narrative is advisory. */
@Component
public class ClaudeClient {

	public record ClaudeReview(String summary, String suspicion, List<String> flags) {
	}

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private final JsonMapper json;
	private final AppProperties props;

	public ClaudeClient(JsonMapper json, AppProperties props) {
		this.json = json;
		this.props = props;
	}

	public ClaudeReview review(Assessment assessment) throws Exception {
		StringBuilder user = new StringBuilder();
		user.append("Rules score ").append(assessment.score()).append(" (").append(assessment.band()).append(").\n");
		for (Finding f : assessment.findings()) {
			user.append("- ").append(f.code()).append(": ").append(f.detail()).append('\n');
		}
		user.append("Return JSON {\"summary\":\"\",\"suspicion\":\"LOW|MEDIUM|HIGH\",\"flags\":[\"\"]}. ");
		user.append("Do not invent facts that are not in the findings. Two or three sentences.");
		String body = json.writeValueAsString(java.util.Map.of(
				"model", props.claude().model(),
				"max_tokens", 500,
				"system", "You assist USPTO OED analysts. You flag patterns. You do not make legal determinations. Reply with JSON only.",
				"messages", List.of(java.util.Map.of("role", "user", "content", user.toString()))));
		HttpRequest request = HttpRequest.newBuilder(URI.create(props.claude().baseUrl() + "/v1/messages"))
				.timeout(Duration.ofSeconds(30))
				.header("content-type", "application/json")
				.header("x-api-key", props.claude().apiKey())
				.header("anthropic-version", "2023-06-01")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() / 100 != 2) throw new IllegalStateException("Claude HTTP " + response.statusCode());
		JsonNode root = json.readTree(response.body());
		String text = root.path("content").path(0).path("text").asText("");
		int start = text.indexOf('{');
		int end = text.lastIndexOf('}');
		if (start < 0 || end <= start) return new ClaudeReview(text, "UNKNOWN", List.of());
		JsonNode parsed = json.readTree(text.substring(start, end + 1));
		List<String> flags = new java.util.ArrayList<>();
		parsed.path("flags").forEach(n -> flags.add(n.asText()));
		return new ClaudeReview(parsed.path("summary").asText(text), parsed.path("suspicion").asText("UNKNOWN"), flags);
	}
}
