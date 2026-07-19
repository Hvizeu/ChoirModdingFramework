package choir.adapter.v71_44;

import choir.internal.platform.CorePlatformRuntime;

/** Public only so the exact-package vanilla shadow can call the adapter boundary. */
public final class V7144RaceExpansionBridge {
	private V7144RaceExpansionBridge() { }
	public static void afterRacePreferencesInitialized() {
		long generation = CorePlatformRuntime.runtimeGeneration();
		V7144RaceBridge.materializePreferences(generation);
		V7144RaceAttributeBridge.materialize(generation);
		V7144RaceHomeResourceBridge.materialize(generation);
	}
}
