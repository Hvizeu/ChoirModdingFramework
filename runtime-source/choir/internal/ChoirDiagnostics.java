package choir.internal;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import snake2d.LOG;

/**
 * Duplicates Choir diagnostics into one append-only log per game process.
 * The engine's UnhandledDump.txt is a rolling log and is therefore not enough
 * evidence for a later Phase 1 review.
 */
public final class ChoirDiagnostics {
	public static final String BUILD_ID = "choir-v71_44-maven-api-runtime-split-20260717.19";
	private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);
	private static Path sessionLog;
	private static boolean buildIdentityLogged;
	private static String validationSessionId;

	private ChoirDiagnostics() { }

	public static synchronized void info(String text) { emit(false, text); }
	public static synchronized void error(String text) { emit(true, text); }
	public static synchronized void beginValidationSession() {
		if (validationSessionId != null) return;
		validationSessionId = "choir-" + UUID.randomUUID().toString();
		info("VALIDATION-SESSION begin id=" + validationSessionId);
	}
	public static synchronized String validationSessionId() { return validationSessionId; }

	public static synchronized void logBuildIdentity() {
		if (buildIdentityLogged) return;
		buildIdentityLogged = true;
		Path source = codeSource();
		String location = source == null ? "<unavailable>" : source.toAbsolutePath().toString();
		String size = "<unavailable>";
		String modified = "<unavailable>";
		String hash = "<unavailable>";
		try {
			if (source != null && Files.isRegularFile(source)) {
				size = Long.toString(Files.size(source));
				modified = Files.getLastModifiedTime(source).toInstant().toString();
				hash = sha256(Files.readAllBytes(source));
			}
		} catch (Exception e) { hash = "<error:" + e.getClass().getSimpleName() + ">"; }
		info("BUILD id=" + BUILD_ID + " choir.version=" + choir.api.Choir.VERSION + " codeSource=" + location + " bytes=" + size + " modifiedUtc=" + modified + " sha256=" + hash + " dedicatedLog=" + sessionLogPath());
	}

	private static void emit(boolean error, String text) {
		String session = validationSessionId == null ? "<uninitialized>" : validationSessionId;
		String line = "[Choir] " + text + " session=" + session;
		if (error) LOG.err(line); else LOG.ln(line);
		append(line);
	}

	private static void append(String line) {
		try {
			Files.writeString(sessionLog(), Instant.now().toString() + " " + line + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (Exception ignored) {
			// Engine logging remains available if local diagnostic storage is unavailable.
		}
	}

	private static Path sessionLog() throws Exception {
		if (sessionLog != null) return sessionLog;
		String appData = System.getenv("APPDATA");
		Path directory = appData == null || appData.length() == 0
				? Paths.get(System.getProperty("user.home"), ".songsofsyx", "logs")
				: Paths.get(appData, "songsofsyx", "logs");
		Files.createDirectories(directory);
		sessionLog = directory.resolve("ChoirRuntime-" + FILE_TIME.format(Instant.now()) + "-pid" + ProcessHandle.current().pid() + ".log");
		return sessionLog;
	}

	private static String sessionLogPath() {
		try { return sessionLog().toAbsolutePath().toString(); }
		catch (Exception e) { return "<unavailable:" + e.getClass().getSimpleName() + ">"; }
	}

	private static Path codeSource() {
		try {
			URI uri = ChoirDiagnostics.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			return Path.of(uri);
		} catch (Exception e) { return null; }
	}

	private static String sha256(byte[] bytes) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
		StringBuilder out = new StringBuilder();
		for (byte b : digest) out.append(String.format("%02x", b & 255));
		return out.toString();
	}
}
