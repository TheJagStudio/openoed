package gov.openoed.api.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
		Jwt jwt,
		Platform platform,
		Claude claude,
		Deadlines deadlines,
		Seed seed,
		Storage storage) {

	public record Jwt(String issuer, Duration accessTtl, Duration refreshTtl, String keyPath) {
	}

	public record Platform(String baseUrl, String internalToken) {
	}

	public record Claude(boolean enabled, String apiKey, String model, String baseUrl) {
		public boolean active() {
			return enabled && apiKey != null && !apiKey.isBlank();
		}
	}

	public record Deadlines(int statutoryMonths, Duration schedulerDelay, boolean simulationEnabled) {
	}

	public record Seed(boolean enabled) {
	}

	public record Storage(String specimenDir) {
	}
}
