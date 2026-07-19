package choir.api.race;

import choir.api.spi.ChoirRuntimeServices;

/** Public, game-type-free facade for data-backed race declaration and composable race patches. */
public final class ChoirRaces {
	public static final int API_VERSION = 4;
	private ChoirRaces() { }
	public static RaceRegistrationResult declareDataBackedRace(RaceDeclaration declaration) { return ChoirRuntimeServices.require().declareDataBackedRace(declaration); }
	public static RaceRegistrationResult patchBoost(RaceBoostPatch patch) { return ChoirRuntimeServices.require().patchRaceBoost(patch); }
	public static RaceRegistrationResult patchPreference(RacePreferencePatch patch) { return ChoirRuntimeServices.require().patchRacePreference(patch); }
	public static RaceRegistrationResult patchText(RaceTextPatch patch) { return ChoirRuntimeServices.require().patchRaceText(patch); }
	public static RaceRegistrationResult patchRelationship(RaceRelationshipPatch patch) { return ChoirRuntimeServices.require().patchRaceRelationship(patch); }
	public static RaceRegistrationResult patchNumericAttribute(RaceNumericPatch patch) { return ChoirRuntimeServices.require().patchRaceNumericAttribute(patch); }
	public static RaceRegistrationResult patchStanding(RaceStandingPatch patch) { return ChoirRuntimeServices.require().patchRaceStanding(patch); }
	public static RaceRegistrationResult requireHomeResource(RaceHomeResourceRequirement requirement) { return ChoirRuntimeServices.require().requireRaceHomeResource(requirement); }
	public static RaceRuntimeSnapshot runtimeSnapshot() { return ChoirRuntimeServices.require().raceRuntimeSnapshot(); }
}
