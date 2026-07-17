package choir.internal.options;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import choir.api.options.OptionSchema;
import choir.api.Choir;

final class OptionsStore {

	private static final String DIR = "Choir";
	private static final String OPTIONS_DIR = "options";
	private static final String GLOBAL_DIR = "global";

	private OptionsStore() {
	}

	static Map<String, Object> loadGlobal(OptionSchema schema) {
		Path file = fileFor(schema.providerId());
		if (!Files.isRegularFile(file)) {
			return new LinkedHashMap<String, Object>();
		}
		try {
			Object parsed = JsonUtil.parse(Files.readString(file, StandardCharsets.UTF_8));
			if (!(parsed instanceof Map<?, ?>)) {
				throw new IllegalArgumentException("Root JSON is not an object");
			}
			Object values = ((Map<?, ?>) parsed).get("values");
			if (!(values instanceof Map<?, ?>)) {
				return new LinkedHashMap<String, Object>();
			}
			LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) values).entrySet()) {
				if (entry.getKey() instanceof String) {
					out.put((String) entry.getKey(), entry.getValue());
				}
			}
			return out;
		} catch (Exception e) {
			backupInvalid(file);
			OptionsDiagnostics.error("Malformed configuration for " + schema.providerId() + " was ignored and backed up", e);
			return new LinkedHashMap<String, Object>();
		}
	}

	static void saveGlobal(OptionSchema schema, Map<String, Object> values) {
		Path file = fileFor(schema.providerId());
		try {
			Files.createDirectories(file.getParent());
			LinkedHashMap<String, Object> root = new LinkedHashMap<String, Object>();
			root.put("frameworkVersion", Choir.VERSION);
			root.put("providerId", schema.providerId());
			root.put("schemaVersion", Integer.valueOf(schema.schemaVersion()));
			root.put("values", new LinkedHashMap<String, Object>(values));
			String json = pretty(JsonUtil.stringify(root));
			Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
			Files.writeString(tmp, json, StandardCharsets.UTF_8);
			try {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException atomicFailure) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			OptionsDiagnostics.error("Could not save configuration for " + schema.providerId(), e);
		}
	}

	static Path rootDirectory() {
		String override = System.getProperty("choir.options.root");
		if (override != null && !override.isBlank()) return Paths.get(override).toAbsolutePath();
		String appData = System.getenv("APPDATA");
		if (appData != null && !appData.isBlank()) return Paths.get(appData, "songsofsyx", DIR);
		return Paths.get(System.getProperty("user.home"), ".songsofsyx", DIR).toAbsolutePath();
	}

	static Path globalDirectory() {
		return rootDirectory().resolve(OPTIONS_DIR).resolve(GLOBAL_DIR);
	}

	private static Path fileFor(String providerId) {
		return globalDirectory().resolve(safeName(providerId) + ".json");
	}

	private static String safeName(String providerId) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < providerId.length(); i++) {
			char c = providerId.charAt(i);
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
				out.append(c);
			} else {
				out.append('_');
			}
		}
		return out.length() == 0 ? "unnamed" : out.toString();
	}

	private static void backupInvalid(Path file) {
		try {
			String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
			Files.move(file, file.resolveSibling(file.getFileName().toString() + ".invalid-" + timestamp), StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			OptionsDiagnostics.error("Could not back up invalid configuration file " + file.getFileName(), e);
		}
	}

	private static String pretty(String compact) {
		StringBuilder out = new StringBuilder();
		int depth = 0;
		boolean string = false;
		for (int i = 0; i < compact.length(); i++) {
			char c = compact.charAt(i);
			if (c == '"' && (i == 0 || compact.charAt(i - 1) != '\\')) {
				string = !string;
			}
			if (!string && (c == '{' || c == '[')) {
				out.append(c).append('\n');
				depth++;
				indent(out, depth);
			} else if (!string && (c == '}' || c == ']')) {
				out.append('\n');
				depth--;
				indent(out, depth);
				out.append(c);
			} else if (!string && c == ',') {
				out.append(c).append('\n');
				indent(out, depth);
			} else if (!string && c == ':') {
				out.append(": ");
			} else {
				out.append(c);
			}
		}
		out.append('\n');
		return out.toString();
	}

	private static void indent(StringBuilder out, int depth) {
		for (int i = 0; i < depth; i++) {
			out.append("  ");
		}
	}
}
