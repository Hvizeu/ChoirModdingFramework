package choir.api.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class PlatformSnapshot {
	private final long generation;
	private final List<ResolvedMod> mods;
	private final Map<String, List<String>> capabilityProviders;

	public PlatformSnapshot(long generation, List<ResolvedMod> mods, Map<String, List<String>> capabilityProviders) {
		this.generation = generation;
		this.mods = List.copyOf(mods);
		TreeMap<String, List<String>> copy = new TreeMap<String, List<String>>();
		for (Map.Entry<String, List<String>> entry : capabilityProviders.entrySet()) {
			ArrayList<String> values = new ArrayList<String>(entry.getValue());
			Collections.sort(values);
			copy.put(entry.getKey(), Collections.unmodifiableList(values));
		}
		this.capabilityProviders = Collections.unmodifiableMap(copy);
	}
	public long generation() { return generation; }
	public List<ResolvedMod> mods() { return mods; }
	public Map<String, List<String>> capabilityProviders() { return capabilityProviders; }
	public boolean isActive(String modId) {
		for (ResolvedMod mod : mods) if (mod.manifest().modId().equals(modId)) return mod.state() == PlatformModState.ACTIVE;
		return false;
	}
	public List<String> providersOf(String capability) { return capabilityProviders.getOrDefault(capability, List.of()); }
}
