package choir.internal.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import choir.api.platform.ModManifest;
import choir.api.platform.PlatformModState;
import choir.api.platform.PlatformSnapshot;
import choir.api.platform.ResolvedMod;

/** Pure deterministic graph resolver. Discovery order is never a tie-breaker. */
public final class ManifestGraphResolver {
	private ManifestGraphResolver() { }
	public static PlatformSnapshot resolve(long generation, List<ModManifest> input) {
		TreeMap<String, ModManifest> manifests = new TreeMap<String, ModManifest>();
		TreeMap<String, List<String>> blockers = new TreeMap<String, List<String>>();
		for (ModManifest manifest : input) {
			ModManifest old = manifests.putIfAbsent(manifest.modId(), manifest);
			if (old != null) {
				block(blockers, old.modId(), "duplicate-manifest source=" + old.sourceModFolder() + "," + manifest.sourceModFolder());
			}
		}

		Map<String, List<DependencyRequirement>> requirements = new HashMap<String, List<DependencyRequirement>>();
		for (ModManifest manifest : manifests.values()) {
			ArrayList<DependencyRequirement> parsed = new ArrayList<DependencyRequirement>();
			for (String text : manifest.requires()) {
				try { parsed.add(DependencyRequirement.parse(text)); }
				catch (RuntimeException e) { block(blockers, manifest.modId(), "invalid-requirement " + text); }
			}
			requirements.put(manifest.modId(), parsed);
			for (DependencyRequirement requirement : parsed) {
				ModManifest dependency = manifests.get(requirement.modId);
				if (dependency == null) block(blockers, manifest.modId(), "missing-required " + requirement.modId);
				else if (!requirement.accepts(dependency.version())) block(blockers, manifest.modId(), "version-mismatch " + requirement.modId + " actual=" + dependency.version());
			}
		}

		for (ModManifest manifest : manifests.values()) for (String incompatible : manifest.incompatible()) {
			String otherId = incompatible.contains("@") ? incompatible.substring(0, incompatible.indexOf('@')).trim() : incompatible.trim();
			if (manifests.containsKey(otherId)) {
				block(blockers, manifest.modId(), "incompatible " + otherId);
				block(blockers, otherId, "incompatible " + manifest.modId());
			}
		}

		boolean changed;
		do {
			changed = false;
			for (ModManifest manifest : manifests.values()) {
				if (blockers.containsKey(manifest.modId())) continue;
				for (DependencyRequirement requirement : requirements.get(manifest.modId())) if (blockers.containsKey(requirement.modId)) {
					block(blockers, manifest.modId(), "required-mod-blocked " + requirement.modId); changed = true; break;
				}
			}
		} while (changed);

		List<String> ordered = topological(manifests, requirements, blockers.keySet());
		Set<String> orderedSet = new HashSet<String>(ordered);
		for (String id : manifests.keySet()) if (!blockers.containsKey(id) && !orderedSet.contains(id)) block(blockers, id, "dependency-cycle");
		ordered = topological(manifests, requirements, blockers.keySet());

		ArrayList<ResolvedMod> resolved = new ArrayList<ResolvedMod>();
		int order = 0;
		for (String id : ordered) resolved.add(new ResolvedMod(manifests.get(id), PlatformModState.ACTIVE, order++, List.of()));
		for (String id : manifests.keySet()) if (blockers.containsKey(id)) resolved.add(new ResolvedMod(manifests.get(id), PlatformModState.BLOCKED, -1, blockers.get(id)));

		TreeMap<String, List<String>> providers = new TreeMap<String, List<String>>();
		for (ResolvedMod mod : resolved) if (mod.state() == PlatformModState.ACTIVE) for (String capability : mod.manifest().capabilities()) {
			providers.computeIfAbsent(capability, ignored -> new ArrayList<String>()).add(mod.manifest().modId());
		}
		return new PlatformSnapshot(generation, resolved, providers);
	}

	private static List<String> topological(Map<String, ModManifest> manifests,
			Map<String, List<DependencyRequirement>> requirements, Set<String> blocked) {
		TreeMap<String, Integer> incoming = new TreeMap<String, Integer>();
		TreeMap<String, Set<String>> outgoing = new TreeMap<String, Set<String>>();
		for (String id : manifests.keySet()) if (!blocked.contains(id)) { incoming.put(id, Integer.valueOf(0)); outgoing.put(id, new TreeSet<String>()); }
		for (String id : incoming.keySet()) for (DependencyRequirement requirement : requirements.get(id)) if (incoming.containsKey(requirement.modId)) {
			if (outgoing.get(requirement.modId).add(id)) incoming.put(id, Integer.valueOf(incoming.get(id).intValue() + 1));
		}
		PriorityQueue<String> ready = new PriorityQueue<String>();
		for (Map.Entry<String, Integer> entry : incoming.entrySet()) if (entry.getValue().intValue() == 0) ready.add(entry.getKey());
		ArrayList<String> result = new ArrayList<String>();
		while (!ready.isEmpty()) {
			String id = ready.remove(); result.add(id);
			for (String dependent : outgoing.get(id)) { int value = incoming.get(dependent).intValue() - 1; incoming.put(dependent, Integer.valueOf(value)); if (value == 0) ready.add(dependent); }
		}
		return result;
	}

	private static void block(Map<String, List<String>> blockers, String modId, String reason) {
		List<String> list = blockers.computeIfAbsent(modId, ignored -> new ArrayList<String>());
		if (!list.contains(reason)) { list.add(reason); list.sort(String::compareTo); }
	}
}
