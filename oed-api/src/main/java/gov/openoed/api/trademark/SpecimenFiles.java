package gov.openoed.api.trademark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import gov.openoed.api.config.AppProperties;
import org.springframework.stereotype.Component;

@Component
public class SpecimenFiles {

	private final Path dir;

	public SpecimenFiles(AppProperties props) {
		this.dir = java.nio.file.Path.of(props.storage().specimenDir());
	}

	public void write(String key, byte[] bytes) {
		try {
			Files.createDirectories(dir);
			Files.write(dir.resolve(key), bytes);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public byte[] read(String key) {
		try {
			return Files.readAllBytes(dir.resolve(key));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static String keyFor(UUID id) {
		return id + ".bin";
	}
}
