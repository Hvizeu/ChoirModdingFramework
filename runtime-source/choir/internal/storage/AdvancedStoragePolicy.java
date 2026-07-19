package choir.internal.storage;

import choir.api.options.OptionApplyMode;
import choir.api.options.OptionRegistrationResult;
import choir.api.options.OptionSchema;
import choir.api.options.OptionSetting;
import choir.internal.ChoirDiagnostics;
import choir.internal.diagnostics.DecisionDiagnostics;
import choir.internal.options.OptionsRegistry;

/**
 * Owns Choir's framework-level advanced-storage option and its current-world latch.
 *
 * <p>The persisted option is deliberately sampled only when a world begins. Applying
 * a changed draft updates the value for the next world, but cannot change storage
 * semantics underneath a live settlement.</p>
 */
public final class AdvancedStoragePolicy {
	public static final String PROVIDER_ID = "choir.framework";
	public static final String OPTION_KEY = "advanced_storage_for_vanilla_rooms";

	private static final OptionSchema OPTIONS = OptionSchema.builder(PROVIDER_ID, "Choir Modding Framework")
			.description("Framework extensions and compatibility behavior.")
			.schemaVersion(2)
			.add(OptionSetting.section("section.extensions", "Extensions"))
			.add(OptionSetting.bool(OPTION_KEY, "Use advanced storage", false)
					.description("Enables multi-resource shelves for supported stockpiles and ordinary "
							+ "single-output workshops and refiners. Registered multi-output rooms and explicit "
							+ "room storage policies remain advanced. Takes effect on the next world load.")
					.applyMode(OptionApplyMode.WORLD_RELOAD)
					.build())
			.add(OptionSetting.section("section.diagnostics", "Diagnostics"))
			.add(OptionSetting.bool(DecisionDiagnostics.DETAILED_OPTION_KEY,
					"Enable detailed framework diagnostics", false)
					.description("Adds rate-limited production-input, production-output, and storage reservation "
							+ "traces to Choir's dedicated log. Stalled input/output problems are always summarized; "
							+ "this option adds successful decisions for deeper troubleshooting.")
					.applyMode(OptionApplyMode.IMMEDIATE)
					.build())
			.build();

	private static boolean worldLatched;
	private static boolean advancedVanillaRooms;

	private AdvancedStoragePolicy() { }

	/** Registers Choir's own options page. Repeating the call is idempotent. */
	public static OptionRegistrationResult registerOptions() {
		return OptionsRegistry.register(OPTIONS);
	}

	/**
	 * Samples the persisted option once for the current world and returns the
	 * effective value. Repeated calls during that world return the same value.
	 */
	public static synchronized boolean latchForWorld() {
		if (!worldLatched) {
			advancedVanillaRooms = OptionsRegistry.getBoolean(PROVIDER_ID, OPTION_KEY, false);
			worldLatched = true;
			ChoirDiagnostics.info("MULTI-STORAGE world-policy-latched advancedVanillaRooms="
					+ advancedVanillaRooms);
		}
		return advancedVanillaRooms;
	}

	/** Returns the latched current-world setting; an unlatch is safely vanilla. */
	public static synchronized boolean advancedVanillaRoomsEnabled() {
		return worldLatched && advancedVanillaRooms;
	}

	public static synchronized boolean worldLatched() { return worldLatched; }

	/** Releases process-local world state without changing the persisted option. */
	public static synchronized void clearWorldLatch() {
		worldLatched = false;
		advancedVanillaRooms = false;
		ChoirDiagnostics.info("MULTI-STORAGE world-policy-released");
	}

	static synchronized void setWorldLatchForTests(boolean enabled) {
		worldLatched = true;
		advancedVanillaRooms = enabled;
	}

	static synchronized void resetForTests() {
		worldLatched = false;
		advancedVanillaRooms = false;
	}
}
