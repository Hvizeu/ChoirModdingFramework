package choir.internal.platform;

import java.util.List;

import choir.api.platform.ModManifest;
import choir.api.platform.PlatformSnapshot;
import choir.api.platform.ResolvedMod;
import choir.internal.ChoirDiagnostics;

public final class PlatformRuntime {
	private static long generation;
	private static String catalogSignature = "";
	private static PlatformSnapshot snapshot = new PlatformSnapshot(0, List.of(), java.util.Map.of());
	private PlatformRuntime() { }
	public static synchronized void replaceManifests(List<ModManifest> manifests) {
		String nextSignature = signature(manifests);
		if (nextSignature.equals(catalogSignature)) return;
		catalogSignature = nextSignature;
		generation++;
		snapshot = ManifestGraphResolver.resolve(generation, manifests);
		ChoirDiagnostics.beginValidationSession();
		int active = 0, blocked = 0;
		for (ResolvedMod mod : snapshot.mods()) {
			if (mod.state() == choir.api.platform.PlatformModState.ACTIVE) active++; else blocked++;
			ChoirDiagnostics.info("PLATFORM manifest id=" + mod.manifest().modId() + " source=" + mod.manifest().sourceModFolder()
					+ " version=" + mod.manifest().version() + " state=" + mod.state() + " order=" + mod.loadOrder()
					+ " diagnostics=" + mod.diagnostics());
		}
		ChoirDiagnostics.info("PLATFORM graph generation=" + generation + " manifests=" + snapshot.mods().size() + " active=" + active + " blocked=" + blocked);
	}
	public static synchronized PlatformSnapshot snapshot() { return snapshot; }
	public static synchronized boolean isActive(String providerId) { return snapshot.isActive(providerId); }
	private static String signature(List<ModManifest> manifests) {
		java.util.TreeMap<String, ModManifest> sorted = new java.util.TreeMap<String, ModManifest>();
		for (ModManifest manifest : manifests) sorted.put(manifest.modId() + "\u0000" + manifest.sourceModFolder(), manifest);
		StringBuilder value = new StringBuilder();
		for (ModManifest manifest : sorted.values()) value.append(manifest.modId()).append('\u0000').append(manifest.sourceModFolder()).append('\u0000')
				.append(manifest.version()).append('\u0000').append(manifest.requires()).append('\u0000').append(manifest.optional()).append('\u0000')
				.append(manifest.incompatible()).append('\u0000').append(manifest.capabilities()).append('\u0001');
		return value.toString();
	}
}
