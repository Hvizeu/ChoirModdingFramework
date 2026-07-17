package choir.internal;
import choir.api.Capability;
import choir.api.spi.ChoirRuntimeServices;
import choir.internal.options.OptionsCapabilities;
import choir.internal.platform.CorePlatformRuntime;
import choir.internal.race.RaceRegistry;
import choir.internal.room.RoomRegistry;
public final class ChoirBootstrap {
	private static int invocationCount;
	private static boolean initialized;
	private static boolean roomSpike;
	private static boolean roomSpikeRuntimeConfirmed;
	private static boolean gameplayReached;
	private static AdapterBootstrap adapter;
	private ChoirBootstrap() { }
	public static synchronized void scriptCallback(String callback) {
		ChoirDiagnostics.beginValidationSession();
		invocationCount++;
		ChoirDiagnostics.info("SCRIPT callback=" + callback + " invocation=" + invocationCount);
	}
	public static synchronized void initializeOnce() {
		if (initialized) {
			ChoirDiagnostics.info("PROCESS bootstrap already verified");
			return;
		}
		initialized = true;
		ChoirDiagnostics.logBuildIdentity();
		try {
			ChoirRuntimeServices.install(ApiRuntimeService.INSTANCE);
			ChoirDiagnostics.info("API runtime-bridge installed=true service=" + ApiRuntimeService.class.getName());
			adapter = (AdapterBootstrap) Class.forName("choir.adapter.v71_44.V7144Adapter").getDeclaredConstructor().newInstance();
			roomSpike = adapter.initialize();
			ChoirDiagnostics.info("OPTIONS module registration=" + OptionsCapabilities.registrationAvailable()
					+ " persistence=" + OptionsCapabilities.globalPersistenceAvailable()
					+ " mainMenuUi=" + OptionsCapabilities.mainMenuUiAvailable()
					+ " inGameUi=" + OptionsCapabilities.inGameUiAvailable()
					+ " detail=" + OptionsCapabilities.uiDetail());
			ChoirDiagnostics.info("Choir " + choir.api.Choir.VERSION + ", adapter " + adapter.adapterVersion() + ", room hook=" + roomSpike);
		} catch (Throwable t) {
			ChoirDiagnostics.error("Bootstrap failed closed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
		}
	}
	public static synchronized void roomSpikeRuntimeConfirmed() {
		roomSpikeRuntimeConfirmed = true;
		ChoirDiagnostics.info("CAPABILITY room-registration-spike=RUNTIME_CONFIRMED");
	}
	public static synchronized void beforeGameCreated() {
		CorePlatformRuntime.beforeGameCreated(adapter == null ? "<unavailable>" : adapter.gameVersion());
	}
	public static synchronized void beforeGameInited() {
		if (!initialized || !roomSpike || adapter == null) {
			ChoirDiagnostics.info("GAMEPLAY probe-not-installed processVerified=" + initialized + " roomHook=" + roomSpike);
			return;
		}
		try { adapter.beforeGameInited(); }
		catch (Throwable t) { ChoirDiagnostics.error("GAMEPLAY probe-install-failed " + t.getClass().getSimpleName() + ": " + t.getMessage()); }
		CorePlatformRuntime.gameInitialized(adapter.gameVersion());
	}
	public static synchronized void instanceCreated() { CorePlatformRuntime.instanceCreated(adapter == null ? "<unavailable>" : adapter.gameVersion()); }
	public static synchronized void gameplayReached(String marker, String gameVersion) {
		if (gameplayReached) return;
		gameplayReached = true;
		ChoirDiagnostics.info("GAMEPLAY reached marker=" + marker + " gameVersion=" + gameVersion);
		CorePlatformRuntime.gameplayReached(gameVersion, marker);
	}
	public static boolean hasCapability(Capability capability) {
		if (capability == Capability.ROOM_REGISTRATION_SPIKE) return roomSpike && roomSpikeRuntimeConfirmed;
		if (capability == Capability.ROOM_REGISTRATION_V1 || capability == Capability.ROOM_PASSIVE_DECORATION_REGISTRATION
				|| capability == Capability.ROOM_PROVIDER_REQUIRED_SAVE_POLICY)
			return initialized && roomSpike && RoomRegistry.capabilityAvailable();
		if (capability == Capability.OPTIONS_REGISTRATION) return OptionsCapabilities.registrationAvailable();
		if (capability == Capability.OPTIONS_GLOBAL_PERSISTENCE) return OptionsCapabilities.globalPersistenceAvailable();
		if (capability == Capability.OPTIONS_MAIN_MENU_UI) return OptionsCapabilities.mainMenuUiAvailable();
		if (capability == Capability.OPTIONS_IN_GAME_UI) return OptionsCapabilities.inGameUiAvailable();
		if (capability == Capability.CORE_MANIFESTS || capability == Capability.CORE_DEPENDENCY_GRAPH
				|| capability == Capability.CORE_LIFECYCLE_EVENTS || capability == Capability.CORE_PATCH_COMPOSITION
				|| capability == Capability.RACE_DATA_BACKED_REGISTRATION || capability == Capability.RACE_BOOST_PATCHING) return initialized && roomSpike;
		if (capability == Capability.RACE_FOOD_PREFERENCE_PATCHING || capability == Capability.RACE_DRINK_PREFERENCE_PATCHING)
			return initialized && roomSpike && RaceRegistry.preferenceCapabilityReady();
		return false;
	}
}
