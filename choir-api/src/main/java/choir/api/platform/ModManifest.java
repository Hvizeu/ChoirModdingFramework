package choir.api.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Immutable, content-free declaration read from an enabled mod folder. */
public final class ModManifest {
	private final String sourceModFolder;
	private final String modId;
	private final String displayName;
	private final String version;
	private final List<String> requires;
	private final List<String> optional;
	private final List<String> incompatible;
	private final Set<String> capabilities;

	public ModManifest(String sourceModFolder, String modId, String displayName, String version,
			List<String> requires, List<String> optional, List<String> incompatible,
			Set<String> capabilities) {
		this.sourceModFolder = required(sourceModFolder, "source mod folder");
		this.modId = stableId(modId, "mod ID");
		this.displayName = required(displayName, "display name");
		this.version = required(version, "version");
		this.requires = immutable(requires);
		this.optional = immutable(optional);
		this.incompatible = immutable(incompatible);
		TreeSet<String> caps = new TreeSet<String>();
		if (capabilities != null) for (String capability : capabilities) caps.add(stableId(capability, "capability"));
		this.capabilities = Collections.unmodifiableSet(caps);
	}

	public String sourceModFolder() { return sourceModFolder; }
	public String modId() { return modId; }
	public String displayName() { return displayName; }
	public String version() { return version; }
	public List<String> requires() { return requires; }
	public List<String> optional() { return optional; }
	public List<String> incompatible() { return incompatible; }
	public Set<String> capabilities() { return capabilities; }

	private static List<String> immutable(List<String> values) {
		ArrayList<String> copy = new ArrayList<String>();
		if (values != null) for (String value : values) if (value != null && !value.trim().isEmpty()) copy.add(value.trim());
		Collections.sort(copy);
		return Collections.unmodifiableList(copy);
	}

	private static String required(String value, String label) {
		if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " must not be blank.");
		return value.trim();
	}

	private static String stableId(String value, String label) {
		String id = required(value, label);
		if (!id.matches("[a-z][a-z0-9._-]*")) throw new IllegalArgumentException(label + " is not a stable lowercase ID: " + id);
		return id;
	}
}
