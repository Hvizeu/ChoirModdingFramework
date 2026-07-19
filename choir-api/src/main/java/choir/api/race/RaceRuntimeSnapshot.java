package choir.api.race;

import java.util.List;

public final class RaceRuntimeSnapshot {
	private final long cycleId;
	private final long runtimeGeneration;
	private final List<String> resolvedDeclarations;
	private final List<String> materializedPatchTargets;
	private final List<String> materializedPreferenceTargets;
	private final List<String> materializedAttributeTargets;
	private final List<String> materializedHomeResourceTargets;
	public RaceRuntimeSnapshot(long cycleId, long runtimeGeneration, List<String> resolvedDeclarations, List<String> materializedPatchTargets) {
		this(cycleId, runtimeGeneration, resolvedDeclarations, materializedPatchTargets, List.of(), List.of(), List.of());
	}
	public RaceRuntimeSnapshot(long cycleId, long runtimeGeneration, List<String> resolvedDeclarations,
			List<String> materializedPatchTargets, List<String> materializedPreferenceTargets) {
		this(cycleId, runtimeGeneration, resolvedDeclarations, materializedPatchTargets, materializedPreferenceTargets, List.of(), List.of());
	}
	public RaceRuntimeSnapshot(long cycleId, long runtimeGeneration, List<String> resolvedDeclarations,
			List<String> materializedPatchTargets, List<String> materializedPreferenceTargets,
			List<String> materializedAttributeTargets) {
		this(cycleId, runtimeGeneration, resolvedDeclarations, materializedPatchTargets,
				materializedPreferenceTargets, materializedAttributeTargets, List.of());
	}
	public RaceRuntimeSnapshot(long cycleId, long runtimeGeneration, List<String> resolvedDeclarations,
			List<String> materializedPatchTargets, List<String> materializedPreferenceTargets,
			List<String> materializedAttributeTargets, List<String> materializedHomeResourceTargets) {
		this.cycleId = cycleId; this.runtimeGeneration = runtimeGeneration;
		this.resolvedDeclarations = List.copyOf(resolvedDeclarations); this.materializedPatchTargets = List.copyOf(materializedPatchTargets);
		this.materializedPreferenceTargets = List.copyOf(materializedPreferenceTargets);
		this.materializedAttributeTargets = List.copyOf(materializedAttributeTargets);
		this.materializedHomeResourceTargets = List.copyOf(materializedHomeResourceTargets);
	}
	public long cycleId() { return cycleId; }
	public long runtimeGeneration() { return runtimeGeneration; }
	public List<String> resolvedDeclarations() { return resolvedDeclarations; }
	public List<String> materializedPatchTargets() { return materializedPatchTargets; }
	public List<String> materializedPreferenceTargets() { return materializedPreferenceTargets; }
	public List<String> materializedAttributeTargets() { return materializedAttributeTargets; }
	public List<String> materializedHomeResourceTargets() { return materializedHomeResourceTargets; }
}
