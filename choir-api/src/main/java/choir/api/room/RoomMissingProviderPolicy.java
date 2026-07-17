package choir.api.room;

/** V71.44 save compatibility policy for a declared room provider. */
public enum RoomMissingProviderPolicy {
	/**
	 * A provider that contributed a room definition must remain enabled when loading
	 * every save created with that definition. V71.44 records the complete room
	 * registry signature and treats a mismatch as a critical SETT load failure.
	 */
	REQUIRE_PROVIDER_FOR_SAVE_LOAD;

	public boolean requiresProviderForSaveLoad() { return true; }
	public boolean permitsLoadWithoutProvider() { return false; }
}
