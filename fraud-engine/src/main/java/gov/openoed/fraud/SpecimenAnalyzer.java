package gov.openoed.fraud;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Local specimen forensics: SHA-256, an average hash, JPEG EXIF presence, JPEG blockiness
 * (recompression), duplicated 16×16 blocks (clone / paste) and flat regions (digital overlays).
 * No native libraries — ImageIO only.
 */
public final class SpecimenAnalyzer {

	private static final int BLOCK = 16;

	public SpecimenFacts analyze(byte[] bytes) {
		if (bytes == null || bytes.length < 8) return SpecimenFacts.missing();
		boolean jpeg = isJpeg(bytes);
		boolean exif = jpeg && hasExif(bytes);
		String sha = sha256(bytes);
		BufferedImage image;
		try {
			image = ImageIO.read(new ByteArrayInputStream(bytes));
		} catch (IOException e) {
			return new SpecimenFacts(false, sha, null, 0, 0, exif, jpeg, 0, 0, 0);
		}
		if (image == null) {
			return new SpecimenFacts(false, sha, null, 0, 0, exif, jpeg, 0, 0, 0);
		}
		int w = image.getWidth();
		int h = image.getHeight();
		int[][] gray = grayscale(image);
		return new SpecimenFacts(true, sha, averageHash(gray), w, h, exif, jpeg,
				blockiness(gray), duplicateBlocks(gray), flatRegionRatio(gray));
	}

	public static String sha256(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	static boolean isJpeg(byte[] bytes) {
		return bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
	}

	/** True when an APP1 Exif segment follows SOI (or appears before the SOS). */
	static boolean hasExif(byte[] bytes) {
		int i = 2;
		while (i + 4 < bytes.length) {
			if ((bytes[i] & 0xFF) != 0xFF) return false;
			int marker = bytes[i + 1] & 0xFF;
			if (marker == 0xDA || marker == 0xD9) return false;
			int len = ((bytes[i + 2] & 0xFF) << 8) | (bytes[i + 3] & 0xFF);
			if (len < 2 || i + 2 + len > bytes.length) return false;
			if (marker == 0xE1 && len >= 8) {
				if (bytes[i + 4] == 'E' && bytes[i + 5] == 'x' && bytes[i + 6] == 'i' && bytes[i + 7] == 'f') return true;
			}
			if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD9)) {
				i += 2;
				continue;
			}
			i += 2 + len;
		}
		return false;
	}

	private static int[][] grayscale(BufferedImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		int[][] g = new int[h][w];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int rgb = image.getRGB(x, y);
				int r = (rgb >> 16) & 0xFF;
				int gr = (rgb >> 8) & 0xFF;
				int b = rgb & 0xFF;
				g[y][x] = (r * 30 + gr * 59 + b * 11) / 100;
			}
		}
		return g;
	}

	static String averageHash(int[][] gray) {
		int h = gray.length;
		int w = gray[0].length;
		int[] samples = new int[64];
		int n = 0;
		long sum = 0;
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				int sx = Math.min(w - 1, x * w / 8);
				int sy = Math.min(h - 1, y * h / 8);
				samples[n] = gray[sy][sx];
				sum += samples[n];
				n++;
			}
		}
		int mean = (int) (sum / 64);
		char[] bits = new char[64];
		for (int i = 0; i < 64; i++) bits[i] = samples[i] >= mean ? '1' : '0';
		return new String(bits);
	}

	/** Mean boundary discontinuity at multiples of 8, normalized to 0..1. High means heavy JPEG. */
	static double blockiness(int[][] gray) {
		int h = gray.length;
		int w = gray[0].length;
		if (w < 16 || h < 16) return 0;
		long boundary = 0;
		long interior = 0;
		int bc = 0;
		int ic = 0;
		for (int y = 1; y < h - 1; y++) {
			for (int x = 8; x < w; x += 8) {
				boundary += Math.abs(gray[y][x] - gray[y][x - 1]);
				bc++;
			}
			for (int x = 4; x < w; x += 8) {
				interior += Math.abs(gray[y][x] - gray[y][x - 1]);
				ic++;
			}
		}
		if (bc == 0 || ic == 0) return 0;
		double b = boundary / (double) bc;
		double i = interior / (double) ic;
		if (i < 0.5) i = 0.5;
		double ratio = b / i;
		return Math.clamp((ratio - 1.0) / 2.5, 0, 1);
	}

	/** Extra copies of identical non-flat 16×16 blocks (a pasted clone shows up as extras). */
	static int duplicateBlocks(int[][] gray) {
		int h = gray.length;
		int w = gray[0].length;
		Map<Long, Integer> counts = new HashMap<>();
		for (int y = 0; y + BLOCK <= h; y += BLOCK) {
			for (int x = 0; x + BLOCK <= w; x += BLOCK) {
				if (blockStdDev(gray, x, y) < 3) continue;
				long hash = blockHash(gray, x, y);
				counts.merge(hash, 1, Integer::sum);
			}
		}
		int extras = 0;
		for (int c : counts.values()) if (c >= 2) extras += c - 1;
		return extras;
	}

	static double flatRegionRatio(int[][] gray) {
		int h = gray.length;
		int w = gray[0].length;
		int blocks = 0;
		int flat = 0;
		for (int y = 0; y + BLOCK <= h; y += BLOCK) {
			for (int x = 0; x + BLOCK <= w; x += BLOCK) {
				blocks++;
				if (blockStdDev(gray, x, y) < 1.5) flat++;
			}
		}
		return blocks == 0 ? 0 : flat / (double) blocks;
	}

	private static long blockHash(int[][] gray, int x0, int y0) {
		long hash = 1125899906842597L;
		for (int y = y0; y < y0 + BLOCK; y += 2) {
			for (int x = x0; x < x0 + BLOCK; x += 2) {
				hash = hash * 31 + gray[y][x];
			}
		}
		return hash;
	}

	private static double blockStdDev(int[][] gray, int x0, int y0) {
		long sum = 0;
		int n = BLOCK * BLOCK;
		for (int y = y0; y < y0 + BLOCK; y++) {
			for (int x = x0; x < x0 + BLOCK; x++) sum += gray[y][x];
		}
		double mean = sum / (double) n;
		double var = 0;
		for (int y = y0; y < y0 + BLOCK; y++) {
			for (int x = x0; x < x0 + BLOCK; x++) {
				double d = gray[y][x] - mean;
				var += d * d;
			}
		}
		return Math.sqrt(var / n);
	}

	/** Hamming distance between two 64-bit average hashes. */
	public static int hashDistance(String a, String b) {
		if (a == null || b == null || a.length() != b.length()) return 64;
		int d = 0;
		for (int i = 0; i < a.length(); i++) if (a.charAt(i) != b.charAt(i)) d++;
		return d;
	}
}
