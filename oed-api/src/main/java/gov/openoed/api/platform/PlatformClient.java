package gov.openoed.api.platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import gov.openoed.api.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** HTTP client for the local SQS / EventBridge / SES / DynamoDB stand-in. */
@Slf4j
@Component
public class PlatformClient {

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
	private final JsonMapper json;
	private final AppProperties props;

	public PlatformClient(JsonMapper json, AppProperties props) {
		this.json = json;
		this.props = props;
	}

	public void sendMessage(String queue, String body) {
		exchange("POST", "/sqs/queues/" + queue + "/messages", Map.of("body", body));
	}

	public void deleteSchedule(String name) {
		exchange("DELETE", "/schedules/" + name, null);
	}

	public String fireSchedule(String name) {
		JsonNode result = exchange("POST", "/schedules/" + name + "/fire", null);
		return result.path("outcome").asText();
	}

	public void putAudit(Map<String, Object> item) {
		exchange("PUT", "/dynamodb/tables/audit-ledger/items", item);
	}

	public List<Map<String, Object>> queryAudit(String pk, Boolean anomaly) {
		Map<String, Object> body = new java.util.LinkedHashMap<>();
		body.put("pk", pk);
		if (anomaly != null) body.put("anomaly", anomaly);
		body.put("limit", 200);
		return json.convertValue(exchange("POST", "/dynamodb/tables/audit-ledger/query", body), new TypeReference<>() {
		});
	}

	public Map<String, Object> status() {
		return json.convertValue(exchange("GET", "/status", null), new TypeReference<>() {
		});
	}

	private JsonNode exchange(String method, String path, Object body) {
		try {
			HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(props.platform().baseUrl() + path))
					.timeout(Duration.ofSeconds(3))
					.header("Accept", "application/json");
			if ("PUT".equals(method) && body != null) {
				builder.header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
			} else if (body == null) {
				builder.method(method, HttpRequest.BodyPublishers.noBody());
			} else {
				builder.header("Content-Type", "application/json")
						.method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
			}
			HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() / 100 != 2) {
				throw new IllegalStateException("Platform " + method + " " + path + " -> " + response.statusCode());
			}
			if (response.body() == null || response.body().isBlank()) return json.createObjectNode();
			return json.readTree(response.body());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Platform call interrupted", e);
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Platform unavailable: " + e.getMessage(), e);
		}
	}
}
