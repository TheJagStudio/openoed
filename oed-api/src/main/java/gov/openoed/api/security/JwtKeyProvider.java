package gov.openoed.api.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import gov.openoed.api.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** RSA signing key for access tokens. Generated once under app.jwt.key-path and reused across restarts. */
@Slf4j
@Component
public class JwtKeyProvider {

	private static final String PRIVATE_KEY_FILE = "openoed-jwt-private.pem";

	private final RSAKey signingKey;

	public JwtKeyProvider(AppProperties props) {
		this.signingKey = loadOrCreate(java.nio.file.Path.of(props.jwt().keyPath()));
	}

	public RSAKey signingKey() {
		return signingKey;
	}

	public RSAPublicKey publicKey() {
		try {
			return signingKey.toRSAPublicKey();
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
	}

	private static RSAKey loadOrCreate(Path dir) {
		try {
			Files.createDirectories(dir);
			Path privateFile = dir.resolve(PRIVATE_KEY_FILE);
			if (!Files.exists(privateFile)) generate(dir, privateFile);
			RSAPrivateCrtKey privateKey = readPrivateKey(privateFile);
			RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
					.generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
			RSAKey key = new RSAKey.Builder(publicKey).privateKey(privateKey).keyUse(KeyUse.SIGNATURE)
					.algorithm(JWSAlgorithm.RS256).keyIDFromThumbprint().build();
			log.info("JWT signing key loaded (kid={})", key.getKeyID());
			return key;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (GeneralSecurityException | JOSEException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void generate(Path dir, Path privateFile) throws IOException, GeneralSecurityException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		Path tmp = Files.createTempFile(dir, "jwt", ".tmp");
		try {
			Files.writeString(tmp, pem(generator.generateKeyPair().getPrivate().getEncoded()), StandardCharsets.US_ASCII);
			Files.move(tmp, privateFile, StandardCopyOption.ATOMIC_MOVE);
		} catch (FileAlreadyExistsException raced) {
			log.debug("JWT key already created");
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	private static RSAPrivateCrtKey readPrivateKey(Path file) throws IOException, GeneralSecurityException {
		String pem = Files.readString(file, StandardCharsets.US_ASCII)
				.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "")
				.replaceAll("\\s", "");
		return (RSAPrivateCrtKey) KeyFactory.getInstance("RSA")
				.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
	}

	private static String pem(byte[] der) {
		String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
		return "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n";
	}
}
