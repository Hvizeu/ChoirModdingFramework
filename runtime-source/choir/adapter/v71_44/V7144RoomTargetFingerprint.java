package choir.adapter.v71_44;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Exact V71.44 gate for the stable passive-room implementation surface. */
final class V7144RoomTargetFingerprint {
	private static final Map<String, String> EXPECTED = expected();
	private V7144RoomTargetFingerprint() { }

	static Result verify() {
		Path gameJar = findGameJar();
		if (gameJar == null) return new Result(false, "<game-jar-not-found>", "SongsOfSyx.jar was not found on the classpath");
		StringBuilder signatureInput = new StringBuilder();
		try (JarFile jar = new JarFile(gameJar.toFile())) {
			for (Map.Entry<String, String> target : EXPECTED.entrySet()) {
				JarEntry entry = jar.getJarEntry(target.getKey());
				if (entry == null) return new Result(false, "<missing>", "missing " + target.getKey());
				String actual;
				try (InputStream in = jar.getInputStream(entry)) { actual = sha256(in.readAllBytes()); }
				signatureInput.append(target.getKey()).append('=').append(actual).append('\n');
				if (!target.getValue().equals(actual))
					return new Result(false, sha256(signatureInput.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
							"fingerprint mismatch " + target.getKey() + " expected=" + target.getValue() + " actual=" + actual);
			}
			return new Result(true, sha256(signatureInput.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
					"matched=" + EXPECTED.size() + " gameJar=" + gameJar.toAbsolutePath());
		} catch (Exception failure) {
			return new Result(false, "<error>", failure.getClass().getSimpleName() + ": " + failure.getMessage());
		}
	}

	private static Path findGameJar() {
		for (String item : System.getProperty("java.class.path", "").split(java.io.File.pathSeparator)) {
			Path path = Paths.get(item);
			if (path.getFileName() != null && path.getFileName().toString().equalsIgnoreCase("SongsOfSyx.jar") && Files.isRegularFile(path))
				return path;
		}
		return null;
	}

	private static Map<String, String> expected() {
		LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
		values.put("game/GameSpec.class", "1851685dc66416b2ce9431d8cb62afae96289eba16f852f3f0122398693dda26");
		values.put("game/save/GameSaver.class", "733a7431e87399043fc8530b6f77ce8c91fe59bbf754b482ed044c035449c9d9");
		values.put("settlement/main/SETT.class", "86035df0eeb27ac2fe9dd1b35a0b62cfcaa7d79b121b3c91ccaa8e00beb1d29c");
		values.put("settlement/room/main/RoomBlueprint.class", "eee505b8757f668d8bed5ba6c176cca4afa9c6a1e97199916939dcd125ebab92");
		values.put("settlement/room/main/RoomBlueprintImp.class", "ea9f96786db8e97614755396d3d7b52b92ad678253721fecb0e40dcb7ddef2de");
		values.put("settlement/room/main/RoomBlueprintIns.class", "916f29523af61ade595db32fc67c113c5cc479daa7767a7fc147f04a43a76684");
		values.put("settlement/room/main/RoomInstance.class", "58910b098936f2eb7fa71ac8694afe6ee646b8451aad903805b79a65f984ddc5");
		values.put("settlement/room/main/RoomsMap.class", "9d9c15f5581e92f3fee24706ab429cc92f12ecbc75519cfd0d3e747fd9c3761b");
		values.put("settlement/room/main/ROOMS.class", "52e003e8e418c48dd449ea9dc3864b8ffeef23d6dfb420f24ee23a72f3cd8fe4");
		values.put("util/keymap/RMAPS.class", "f31e9a1c5e8d110b7a62de34b9f40c86b716a3baa0e0084cecfb519420f757b1");
		values.put("settlement/room/main/util/RoomInitData.class", "8902c3170ab1ce191f5260cb80996bf4edea95704a4014fed10496297bae2e7f");
		values.put("settlement/room/main/furnisher/Furnisher.class", "d16376e5c3949b0c767911fc0fec749d5506fdf267320205c27c9315e3dc5e24");
		values.put("settlement/room/main/furnisher/FurnisherItem.class", "e79f332ddb2ec846b74016e7c28044ac34ce91611b66b30721cb6be7f7b8d9af");
		values.put("settlement/room/main/furnisher/FurnisherItemTile.class", "6ddf897b9f628ba5dab0c000d21dc1e0d73d4a9c7a83e45d85765d3d273e92c9");
		values.put("settlement/room/sprite/RoomSprite1x1.class", "7076e4560de90ec724d7fed7a949999630394348d026bebe6c01d35d37fa8103");
		return values;
	}

	private static String sha256(byte[] bytes) throws Exception {
		byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
		StringBuilder out = new StringBuilder();
		for (byte value : hash) out.append(String.format("%02x", value & 255));
		return out.toString();
	}

	static final class Result {
		final boolean matches;
		final String signature;
		final String detail;
		Result(boolean matches, String signature, String detail) {
			this.matches = matches;
			this.signature = signature;
			this.detail = detail;
		}
	}
}
