package choir.adapter.v71_44;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import choir.api.platform.ModManifest;
import choir.internal.ChoirDiagnostics;
import choir.internal.platform.PlatformRuntime;
import init.paths.ModInfo;
import init.paths.PATHS;
import snake2d.util.sets.LIST;

/** Reads enabled-mod static declarations without loading consumer Java classes. */
public final class V7144PlatformManifestCatalog {
	public static final String RELATIVE_PATH = "choir/core-platform.properties";
	private V7144PlatformManifestCatalog() { }
	public static void refresh() {
		if (!PATHS.inited()) { PlatformRuntime.replaceManifests(List.of()); return; }
		LIST<ModInfo> enabled = PATHS.currentMods();
		ArrayList<ModManifest> manifests = new ArrayList<ModManifest>();
		int invalid = 0;
		for (int i = 0; i < enabled.size(); i++) {
			ModInfo mod = enabled.get(i);
			Path root = Paths.get(mod.absolutePath).toAbsolutePath().normalize();
			Path path = root.resolve("V" + mod.majorVersion).resolve(RELATIVE_PATH).normalize();
			if (!path.startsWith(root) || !Files.isRegularFile(path)) continue;
			try { manifests.add(read(mod.path, mod.name, mod.version, path)); }
			catch (Exception failure) { invalid++; ChoirDiagnostics.error("PLATFORM manifest-rejected folder=" + mod.path + " file=" + path + " cause=" + failure.getMessage()); }
		}
		PlatformRuntime.replaceManifests(manifests);
		ChoirDiagnostics.info("PLATFORM catalog source=PATHS.currentMods enabledMods=" + enabled.size() + " manifests=" + manifests.size() + " invalid=" + invalid);
	}
	static ModManifest read(String sourceFolder, String fallbackName, String fallbackVersion, Path path) throws Exception {
		Properties properties = new Properties();
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { properties.load(reader); }
		if (!"1".equals(required(properties, "formatVersion"))) throw new IllegalArgumentException("unsupported formatVersion");
		String displayName = properties.getProperty("displayName", fallbackName).trim();
		String version = properties.getProperty("version", fallbackVersion).trim();
		return new ModManifest(sourceFolder, required(properties, "modId"), displayName, version,
				list(properties, "requires"), list(properties, "optional"), list(properties, "incompatible"), set(properties, "capabilities"));
	}
	private static String required(Properties properties, String key) { String value = properties.getProperty(key); if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("missing " + key); return value.trim(); }
	private static List<String> list(Properties properties, String key) { String value = properties.getProperty(key, "").trim(); return value.isEmpty() ? List.of() : Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isEmpty()).toList(); }
	private static Set<String> set(Properties properties, String key) { return new TreeSet<String>(list(properties, key)); }
}
