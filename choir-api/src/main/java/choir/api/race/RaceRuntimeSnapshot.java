package choir.api.race;

import java.util.List;

public final class RaceRuntimeSnapshot {
	private final long cycleId;
	private final long runtimeGeneration;
	private final List<String> resolvedDeclarations;
	private final List<String> materializedPatchTargets;
	private final List<String> materializedPreferenceTargets;
	public RaceRuntimeSnapshot(long cycleId, long runtimeGeneration, List<String> resolvedDeclarations, List<String> materializedPatchTargets) {
		this(cycleId, runtimeGeneration, resolvedDeclarations, materializedPatchTargets, List.of());
	}
	public RaceRuntimeSnapshot(long cycleId, long runtimeGeneration, List<String> resolvedDeclarations,
			List<String> materializedPatchTargets, List<String> materializedPreferenceTargets) {
		this.cycleId = cycleId; this.runtimeGeneration = runtimeGeneration;
		this.resolvedDeclarations = List.copyOf(resolvedDeclarations); this.materializedPatchTargets = List.copyOf(materializedPatchTargets);
		this.materializedPreferenceTargets = List.copyOf(materializedPreferenceTargets);
	}
	public long cycleId() { return cycleId; }
	public long runtimeGeneration() { return runtimeGeneration; }
	public List<String> resolvedDeclarations() { return resolvedDeclarations; }
	public List<String> materializedPatchTargets() { return materializedPatchTargets; }
	public List<String> materializedPreferenceTargets() { return materializedPreferenceTargets; }
}
