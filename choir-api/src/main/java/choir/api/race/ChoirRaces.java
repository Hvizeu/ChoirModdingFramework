package choir.api.race;

import choir.api.spi.ChoirRuntimeServices;

/** Public, game-type-free facade for data-backed race declaration and composable race patches. */
public final class ChoirRaces {
	public static final int API_VERSION = 2;
	private ChoirRaces() { }
	public static RaceRegistrationResult declareDataBackedRace(RaceDeclaration declaration) { return ChoirRuntimeServices.require().declareDataBackedRace(declaration); }
	public static RaceRegistrationResult patchBoost(RaceBoostPatch patch) { return ChoirRuntimeServices.require().patchRaceBoost(patch); }
	public static RaceRegistrationResult patchPreference(RacePreferencePatch patch) { return ChoirRuntimeServices.require().patchRacePreference(patch); }
	public static RaceRuntimeSnapshot runtimeSnapshot() { return ChoirRuntimeServices.require().raceRuntimeSnapshot(); }
}
