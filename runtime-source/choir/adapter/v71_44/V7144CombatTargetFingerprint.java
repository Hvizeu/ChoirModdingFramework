package choir.adapter.v71_44;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarFile;

/** Fingerprint gate for the exact V71.44 combat classes shadowed by Choir. */
final class V7144CombatTargetFingerprint {
	private static final Map<String, String> EXPECTED = Map.ofEntries(
			Map.entry("settlement/entity/humanoid/HEvent.class", "8ba50ab8bcf981e54422693af6533542dee196026369bf2357271ba25322ceb0"),
			Map.entry("settlement/entity/humanoid/HEvent$Handler.class", "027c1ff32f1d058a819a65f1697779f6f6f14bcb94a91b09f50a3cdaa0e73bfb"),
			Map.entry("settlement/entity/humanoid/HEvent$HEventData.class", "e2cf89ba5b6db4b99f1babdff0259f6905190c0bfe780cdd5b5bc4d1ff1fc96d"),
			Map.entry("settlement/thing/projectiles/SProjectiles.class", "1a48274257b8de3075307e2af4734d1c5029c2c460ee5d2dfdcbf1854e4d9000")
	);

	static Result verify() {
		LinkedHashMap<String, String> actual = new LinkedHashMap<String, String>();
		try {
			for (String item : System.getProperty("java.class.path", "").split(java.io.File.pathSeparator)) {
				Path path = Paths.get(item);
				if (!path.getFileName().toString().equalsIgnoreCase("SongsOfSyx.jar") || !Files.isRegularFile(path)) continue;
				try (JarFile jar = new JarFile(path.toFile())) {
					for (String entry : EXPECTED.keySet()) {
						if (jar.getJarEntry(entry) == null) { actual.put(entry, "<missing>"); continue; }
						try (InputStream in = jar.getInputStream(jar.getJarEntry(entry))) {
							actual.put(entry, hex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes())));
						}
					}
				}
				return new Result(path.toAbsolutePath().toString(), actual, actual.equals(EXPECTED));
			}
		} catch (Exception failure) { actual.put("<error>", failure.getClass().getSimpleName() + ':' + failure.getMessage()); }
		return new Result("<not-found>", actual, false);
	}
	private static String hex(byte[] bytes) { StringBuilder out = new StringBuilder(); for (byte b : bytes) out.append(String.format("%02x", b & 255)); return out.toString(); }
	static final class Result {
		final String jar; final Map<String, String> actual; final boolean matches;
		Result(String jar, Map<String, String> actual, boolean matches) { this.jar = jar; this.actual = Map.copyOf(actual); this.matches = matches; }
		String detail() { return "jar=" + jar + " actual=" + actual; }
	}
}
