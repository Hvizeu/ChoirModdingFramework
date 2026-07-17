package choir.adapter.v71_44.options;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Exact original-class gate and duplicate-shadow detector for Choir-owned option menus. */
final class OptionsUiTargetFingerprintVerifier {
	private static final String MANIFEST = "choir/adapter/v71_44/options-ui-targets.properties";

	private OptionsUiTargetFingerprintVerifier() { }

	static Result verify() {
		try {
			Properties properties = new Properties();
			InputStream manifest = OptionsUiTargetFingerprintVerifier.class.getClassLoader().getResourceAsStream(MANIFEST);
			if (manifest == null) return Result.failure("Fingerprint manifest is missing.");
			try { properties.load(manifest); } finally { manifest.close(); }
			Path gameJar = findGameJar();
			if (gameJar == null) return Result.failure("SongsOfSyx.jar is missing from the runtime classpath.");
			List<String> failures = new ArrayList<String>();
			JarFile jar = new JarFile(gameJar.toFile());
			try {
				verifyFamily(properties, jar, gameJar, "main", failures);
				verifyFamily(properties, jar, gameJar, "ingame", failures);
			} finally { jar.close(); }
			String signature = sha256((properties.getProperty("target.main.family.aggregate.sha256") + "|"
					+ properties.getProperty("target.ingame.family.aggregate.sha256")).getBytes(StandardCharsets.UTF_8));
			return failures.isEmpty() ? Result.success(signature, gameJar.toAbsolutePath().toString())
					: Result.failure(String.join("; ", failures), signature, gameJar.toAbsolutePath().toString());
		} catch (Exception e) {
			return Result.failure(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private static void verifyFamily(Properties properties, JarFile jar, Path gameJar, String family,
			List<String> failures) throws Exception {
		int count = Integer.parseInt(properties.getProperty("target." + family + ".family.count"));
		List<String> aggregateRows = new ArrayList<String>(count);
		for (int i = 0; i < count; i++) {
			String[] parts = properties.getProperty("target." + family + ".class." + i).split("\\|", 2);
			String entryName = parts[0];
			String expected = parts[1];
			JarEntry entry = jar.getJarEntry(entryName);
			if (entry == null) { failures.add("missing " + entryName); continue; }
			InputStream in = jar.getInputStream(entry);
			String actual;
			try { actual = sha256(in.readAllBytes()); } finally { in.close(); }
			if (!expected.equals(actual)) failures.add("hash " + entryName + " expected=" + expected + " actual=" + actual);
			aggregateRows.add(entryName + "=" + actual);
			verifyClasspathLocations(entryName, gameJar, i == count - 1, failures);
		}
		Collections.sort(aggregateRows);
		String aggregate = sha256(String.join("\n", aggregateRows).getBytes(StandardCharsets.UTF_8));
		String expectedAggregate = properties.getProperty("target." + family + ".family.aggregate.sha256");
		if (!expectedAggregate.equals(aggregate)) failures.add("family aggregate " + family
				+ " expected=" + expectedAggregate + " actual=" + aggregate);
	}

	private static void verifyClasspathLocations(String entryName, Path gameJar, boolean requireOwned,
			List<String> failures) throws Exception {
		String own = OptionsUiTargetFingerprintVerifier.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
		String game = gameJar.toUri().toURL().toExternalForm();
		Enumeration<URL> resources = OptionsUiTargetFingerprintVerifier.class.getClassLoader().getResources(entryName);
		int ownCopies = 0;
		int gameCopies = 0;
		List<String> unknown = new ArrayList<String>();
		while (resources.hasMoreElements()) {
			String location = resources.nextElement().toExternalForm();
			if (location.contains(own)) ownCopies++;
			else if (location.contains(game)) gameCopies++;
			else unknown.add(location);
		}
		if (ownCopies > 1) failures.add("duplicate Choir copies " + entryName + " count=" + ownCopies);
		if (requireOwned && ownCopies != 1) failures.add("approved Choir shadow missing " + entryName + " count=" + ownCopies);
		if (gameCopies > 1) failures.add("duplicate game copies " + entryName + " count=" + gameCopies);
		if (!unknown.isEmpty()) failures.add("conflicting classpath copies " + entryName + " locations=" + unknown);
	}

	private static Path findGameJar() {
		for (String item : System.getProperty("java.class.path", "").split(java.io.File.pathSeparator)) {
			Path path = Paths.get(item);
			if (path.getFileName() != null && path.getFileName().toString().equalsIgnoreCase("SongsOfSyx.jar")
					&& Files.isRegularFile(path)) return path;
		}
		return null;
	}

	private static String sha256(byte[] bytes) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
		StringBuilder out = new StringBuilder();
		for (byte b : digest) out.append(String.format("%02x", b & 255));
		return out.toString();
	}

	static final class Result {
		final boolean matches;
		final String signature;
		final String detail;
		final String gameJar;
		private Result(boolean matches, String signature, String detail, String gameJar) {
			this.matches = matches; this.signature = signature; this.detail = detail; this.gameJar = gameJar;
		}
		static Result success(String signature, String gameJar) { return new Result(true, signature, "matched", gameJar); }
		static Result failure(String detail) { return new Result(false, "<unavailable>", detail, "<unavailable>"); }
		static Result failure(String detail, String signature, String gameJar) { return new Result(false, signature, detail, gameJar); }
	}
}
