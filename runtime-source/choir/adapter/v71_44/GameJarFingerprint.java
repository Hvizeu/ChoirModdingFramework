package choir.adapter.v71_44;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.jar.JarFile;
final class GameJarFingerprint {
	private static final String ENTRY = "settlement/room/main/util/RoomsCreator.class";
	private static final String EXPECTED = "3fe714a57c711196c2aa078393a780e01e59d526954e4539ca23cd9c52bb9bb9";
	static Result inspect() {
		try {
			for (String item : System.getProperty("java.class.path", "").split(java.io.File.pathSeparator)) {
				Path path = Paths.get(item); if (!path.getFileName().toString().equalsIgnoreCase("SongsOfSyx.jar") || !Files.isRegularFile(path)) continue;
				String jarHash = hex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
				JarFile jar = new JarFile(path.toFile()); try { InputStream in = jar.getInputStream(jar.getJarEntry(ENTRY)); try { return new Result(path.toAbsolutePath().toString(), jarHash, EXPECTED, hex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()))); } finally { in.close(); } } finally { jar.close(); }
			}
		} catch (Exception e) { return new Result("<error>", "<unavailable>", EXPECTED, "<error:" + e.getClass().getSimpleName() + ">"); }
		return new Result("<not-found>", "<unavailable>", EXPECTED, "<unavailable>");
	}
	private static String hex(byte[] bytes) { StringBuilder out = new StringBuilder(); for (byte b : bytes) out.append(String.format("%02x", b & 255)); return out.toString(); }
	static final class Result {
		final String path, jarHash, expectedRoomsCreatorHash, actualRoomsCreatorHash;
		Result(String path, String jarHash, String expected, String actual) { this.path=path; this.jarHash=jarHash; this.expectedRoomsCreatorHash=expected; this.actualRoomsCreatorHash=actual; }
		boolean matches() { return expectedRoomsCreatorHash.equals(actualRoomsCreatorHash); }
	}
}
