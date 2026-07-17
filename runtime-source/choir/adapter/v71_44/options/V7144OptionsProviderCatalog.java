package choir.adapter.v71_44.options;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import choir.internal.ChoirDiagnostics;
import choir.internal.options.OptionsProviderCatalogEntry;
import choir.internal.options.OptionsRegistry;
import init.paths.ModInfo;
import init.paths.PATHS;
import snake2d.util.sets.LIST;
import choir.adapter.v71_44.V7144PlatformManifestCatalog;

/** V71.44 enabled-mod catalog loader. It reads data only and never loads consumer classes. */
public final class V7144OptionsProviderCatalog {
	public static final String RELATIVE_PATH = "choir/options-provider.properties";
	private static String lastSummary = "";

	private V7144OptionsProviderCatalog() { }

	public static synchronized void refresh() {
		V7144PlatformManifestCatalog.refresh();
		if (!PATHS.inited()) {
			OptionsRegistry.replaceCatalog(List.of());
			return;
		}
		LIST<ModInfo> enabled = PATHS.currentMods();
		ArrayList<OptionsProviderCatalogEntry> entries = new ArrayList<OptionsProviderCatalogEntry>();
		int invalid = 0;
		for (int i = 0; i < enabled.size(); i++) {
			ModInfo mod = enabled.get(i);
			Path root = Paths.get(mod.absolutePath).toAbsolutePath().normalize();
			Path manifest = root.resolve("V" + mod.majorVersion).resolve(RELATIVE_PATH).normalize();
			if (!manifest.startsWith(root) || !Files.isRegularFile(manifest)) continue;
			try {
				OptionsProviderCatalogEntry entry = read(mod.path, manifest);
				entries.add(entry);
			} catch (Exception e) {
				invalid++;
				ChoirDiagnostics.error("OPTIONS CATALOG rejected mod=" + quoted(mod.path) + " file=" + quoted(manifest.toString())
						+ " detail=" + e.getClass().getSimpleName() + ": " + e.getMessage());
			}
		}
		OptionsRegistry.replaceCatalog(entries);
		String summary = "enabledMods=" + enabled.size() + " declarations=" + entries.size() + " invalid=" + invalid;
		if (!summary.equals(lastSummary)) {
			lastSummary = summary;
			ChoirDiagnostics.info("OPTIONS CATALOG refreshed source=PATHS.currentMods " + summary);
		}
	}

	static OptionsProviderCatalogEntry read(String modId, Path manifest) throws Exception {
		Properties properties = new Properties();
		BufferedReader reader = Files.newBufferedReader(manifest, StandardCharsets.UTF_8);
		try { properties.load(reader); } finally { reader.close(); }
		String format = required(properties, "formatVersion");
		if (!"1".equals(format)) throw new IllegalArgumentException("unsupported formatVersion=" + format);
		return new OptionsProviderCatalogEntry(modId, required(properties, "providerId"),
				required(properties, "displayName"), properties.getProperty("description", "").trim());
	}

	private static String required(Properties properties, String key) {
		String value = properties.getProperty(key);
		if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("missing " + key);
		return value.trim();
	}

	private static String quoted(String value) {
		return "\"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
