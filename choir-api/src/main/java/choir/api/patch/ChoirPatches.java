package choir.api.patch;

import choir.api.spi.ChoirRuntimeServices;

/** Generic typed, deterministic process-scope patch composition. */
public final class ChoirPatches {
	public static final int API_VERSION = 1;
	private ChoirPatches() { }
	public static <T> PatchRegistrationResult registerTarget(PatchTarget<T> target) { return ChoirRuntimeServices.require().registerPatchTarget(target); }
	public static <T> PatchRegistrationResult contribute(PatchContribution<T> contribution) { return ChoirRuntimeServices.require().contributePatch(contribution); }
	public static <T> PatchResolution<T> resolve(String targetId, Class<T> valueType) { return ChoirRuntimeServices.require().resolvePatch(targetId, valueType); }
}
