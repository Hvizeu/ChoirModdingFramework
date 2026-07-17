package choir.adapter.v71_44.options;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import choir.internal.ChoirDiagnostics;
import game.VERSION;

/** Exact V71.44 gate for Choir's menu capitalization and save-load warning shadows. */
public final class V7144MenuUxBridge {
	private static boolean verified;
	private static boolean compatible;
	private static String detail = "not verified";
	private V7144MenuUxBridge() { }

	public static synchronized void requireCompatible() {
		if (!verified) verify();
		if (!compatible) throw new IllegalStateException("Choir menu UX compatibility gate failed: " + detail);
	}

	private static void verify() {
		verified = true;
		ChoirDiagnostics.beginValidationSession();
		ChoirDiagnostics.logBuildIdentity();
		if (VERSION.VERSION_MAJOR != 71 || VERSION.VERSION_MINOR != 44) {
			detail = "unsupported game version " + VERSION.VERSION_STRING;
			ChoirDiagnostics.error("MENU-UX compatibility-fingerprints matched=false detail=" + detail);
			return;
		}
		Path jarPath = findGameJar();
		if (jarPath == null) {
			detail = "SongsOfSyx.jar missing from classpath";
			ChoirDiagnostics.error("MENU-UX compatibility-fingerprints matched=false detail=" + detail);
			return;
		}
		try (JarFile jar = new JarFile(jarPath.toFile())) {
			for (Map.Entry<String, String> target : expected().entrySet()) {
				JarEntry entry = jar.getJarEntry(target.getKey());
				if (entry == null) { detail = "missing " + target.getKey(); return; }
				String actual;
				try (InputStream in = jar.getInputStream(entry)) { actual = sha256(in.readAllBytes()); }
				if (!target.getValue().equals(actual)) {
					detail = "fingerprint mismatch " + target.getKey() + " expected=" + target.getValue() + " actual=" + actual;
					ChoirDiagnostics.error("MENU-UX compatibility-fingerprints matched=false detail=" + detail);
					return;
				}
			}
			compatible = true;
			detail = "matched=" + expected().size() + " gameJar=" + jarPath.toAbsolutePath();
			ChoirDiagnostics.info("MENU-UX compatibility-fingerprints matched=true detail=" + detail);
		} catch (Exception failure) {
			detail = failure.getClass().getSimpleName() + ": " + failure.getMessage();
			ChoirDiagnostics.error("MENU-UX compatibility-fingerprints matched=false detail=" + detail);
		}
	}

	private static Map<String, String> expected() {
		LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
		values.put("menu/ScMain.class", "d838eee8e21bdc1dfb01c8ab07a4893f79bce2868e979fddd5d54aa6eae90c99");
		values.put("view/menu/MenuScreen.class", "e5b8ee39a4b10116ed76c63ac9be3a95acd7ab6e96b065ac3246595c4d78f5dd");
		values.put("view/menu/MenuScreenLoad.class", "5c6a55f9b36b73d212a7d790e326ea4d82dce21b4ba3a702dc819c909f98df19");
		return values;
	}

	private static Path findGameJar() {
		for (String item : System.getProperty("java.class.path", "").split(java.io.File.pathSeparator)) {
			Path path = Paths.get(item);
			if (path.getFileName() != null && path.getFileName().toString().equalsIgnoreCase("SongsOfSyx.jar") && Files.isRegularFile(path)) return path;
		}
		return null;
	}

	private static String sha256(byte[] bytes) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
		StringBuilder out = new StringBuilder();
		for (byte value : digest) out.append(String.format("%02x", value & 255));
		return out.toString();
	}
}
