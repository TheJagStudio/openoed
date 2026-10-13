package gov.openoed.platform;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record PlatformProperties(String dataDir, String apiBaseUrl, String internalToken) {

	public PlatformProperties {
		if (dataDir == null || dataDir.isBlank()) dataDir = "data/platform";
		if (apiBaseUrl == null || apiBaseUrl.isBlank()) apiBaseUrl = "http://localhost:8087";
		if (internalToken == null || internalToken.isBlank()) internalToken = "dev-internal-token";
	}
}
