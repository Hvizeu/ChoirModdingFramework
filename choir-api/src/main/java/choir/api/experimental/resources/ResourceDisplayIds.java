package choir.api.experimental.resources;

import java.util.regex.Pattern;

final class ResourceDisplayIds {
	private static final Pattern PROVIDER = Pattern.compile("[a-z0-9]+(\\.[a-z0-9]+)+");
	private static final Pattern GROUP = Pattern.compile("[a-z0-9]+(\\.[a-z0-9]+)+:[A-Z][A-Z0-9_]*");
	private static final Pattern RESOURCE = Pattern.compile("[A-Z][A-Z0-9_]*");
	private static final Pattern LOCALIZATION = Pattern.compile("[A-Za-z0-9_.:-]+");

	private ResourceDisplayIds() { }

	static String provider(String value) { return require(value, PROVIDER, "provider ID"); }
	static String group(String value) { return require(value, GROUP, "group ID"); }
	static String resource(String value) { return require(value, RESOURCE, "resource ID"); }
	static String localization(String value) { return require(value, LOCALIZATION, "localization key"); }

	static String label(String value) {
		if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Fallback label must not be blank.");
		return value;
	}

	private static String require(String value, Pattern pattern, String description) {
		if (value == null || !pattern.matcher(value).matches())
			throw new IllegalArgumentException("Invalid experimental resource-display " + description + ": " + value);
		return value;
	}
}
