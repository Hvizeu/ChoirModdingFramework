package choir.adapter.v71_44;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.jar.JarFile;

/** Exact V71.44 gate for the sole multi-output production shadow. */
final class V7144ProductionTargetFingerprint {
	private static final String ENTRY = "settlement/room/main/job/RoomResDeposit.class";
	private static final String EXPECTED = "9886202a077c5c2e545714127f38e16c3ddb0aa162101fcf486e17d0cc65f05c";
	private V7144ProductionTargetFingerprint() { }

	static Result verify() {
		try {
			for (String item : System.getProperty("java.class.path", "").split(java.io.File.pathSeparator)) {
				Path path = Paths.get(item);
				if (path.getFileName() == null || !path.getFileName().toString().equalsIgnoreCase("SongsOfSyx.jar")
						|| !Files.isRegularFile(path)) continue;
				try (JarFile jar = new JarFile(path.toFile())) {
					if (jar.getJarEntry(ENTRY) == null) return new Result(path.toAbsolutePath().toString(), "<missing>", false);
					try (InputStream in = jar.getInputStream(jar.getJarEntry(ENTRY))) {
						String actual = hex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
						return new Result(path.toAbsolutePath().toString(), actual, EXPECTED.equals(actual));
					}
				}
			}
		} catch (Exception failure) {
			return new Result("<error>", failure.getClass().getSimpleName() + ':' + failure.getMessage(), false);
		}
		return new Result("<not-found>", "<unavailable>", false);
	}
	private static String hex(byte[] bytes) { StringBuilder out = new StringBuilder(); for (byte b : bytes) out.append(String.format("%02x", b & 255)); return out.toString(); }
	static final class Result {
		final String jar; final String actual; final boolean matches;
		Result(String jar, String actual, boolean matches) { this.jar = jar; this.actual = actual; this.matches = matches; }
		String detail() { return "jar=" + jar + " entry=" + ENTRY + " expected=" + EXPECTED + " actual=" + actual; }
	}
}
