package choir.adapter.v71_44;
import choir.internal.AdapterBootstrap;
import choir.internal.ChoirDiagnostics;
import choir.internal.resources.ResourceDisplayRuntime;
import choir.adapter.v71_44.options.V7144OptionsUiBridge;
import game.VERSION;
import game.GameDisposable;
import choir.internal.platform.CorePlatformRuntime;
import choir.internal.room.RoomRegistry;
public final class V7144Adapter implements AdapterBootstrap {
	private final V7144ResourceDisplayResolver resourceDisplayResolver = new V7144ResourceDisplayResolver();
	private boolean gameDisposableInstalled;
	public boolean initialize() {
		ChoirDiagnostics.info("ADAPTER selected=v71_44 detectedGame=" + VERSION.VERSION_STRING);
		ChoirDiagnostics.info("GAME version=" + VERSION.VERSION_STRING + " source=game.VERSION");
		if (VERSION.VERSION_MAJOR != 71 || VERSION.VERSION_MINOR != 44) { RoomRegistrationBridge.processFailed(); ResourceDisplayRuntime.adapterUnavailable("Unsupported game version " + VERSION.VERSION_STRING); ChoirDiagnostics.error("Unsupported game version " + VERSION.VERSION_STRING + "; expected 0.71.44."); return false; }
		GameJarFingerprint.Result fingerprint = GameJarFingerprint.inspect();
		ChoirDiagnostics.info("FINGERPRINT gameJar=" + fingerprint.path + " gameJarSha256=" + fingerprint.jarHash + " RoomsCreator.expected=" + fingerprint.expectedRoomsCreatorHash + " RoomsCreator.actual=" + fingerprint.actualRoomsCreatorHash);
		if (!fingerprint.matches()) { RoomRegistrationBridge.processFailed(); ResourceDisplayRuntime.adapterUnavailable("RoomsCreator compatibility fingerprint mismatch"); ChoirDiagnostics.error("RoomsCreator fingerprint mismatch; refusing hook."); return false; }
		V7144RoomTargetFingerprint.Result roomTargets = V7144RoomTargetFingerprint.verify();
		RoomRegistry.adapterReady(roomTargets.matches, roomTargets.detail);
		ChoirDiagnostics.info("ROOM-REGISTRATION compatibility-fingerprints matched=" + roomTargets.matches
				+ " signature=" + roomTargets.signature + " detail=" + roomTargets.detail);
		V7144PlatformManifestCatalog.refresh();
		V7144RaceBridge.initialize();
		V7144RaceAttributeBridge.initialize();
		V7144RaceHomeResourceBridge.initialize();
		V7144CombatDamageBridge.initialize();
		V7144MultiOutputProductionBridge.initialize();
		V7144MultiResourceStorage.initialize();
		UiTargetFingerprintVerifier.Result uiTargets = UiTargetFingerprintVerifier.verify();
		ChoirDiagnostics.info("RESOURCE-DISPLAY compatibility-fingerprints matched=" + uiTargets.matches
				+ " signature=" + uiTargets.signature + " gameJar=" + uiTargets.gameJar + " detail=" + uiTargets.detail);
		ResourceDisplayRuntime.adapterReady("v71_44", uiTargets.matches, uiTargets.signature);
		V7144ResourceDisplayRightPanelBridge.initialize(resourceDisplayResolver, uiTargets.matches);
		V7144ResourceDisplayStockpileBridge.initialize(resourceDisplayResolver, uiTargets.matches);
		V7144OptionsUiBridge.initialize();
		V7144RuntimeEvidence.logEnabledModInventory();
		ChoirDiagnostics.info("HOOK state-before-arm=" + RoomRegistrationBridge.state());
		RoomRegistrationBridge.arm();
		ChoirDiagnostics.info("HOOK state-after-arm=" + RoomRegistrationBridge.state());
		return true;
	}
	public void beforeGameInited() {
		ResourceDisplayRuntime.observeAndRebuild(resourceDisplayResolver, "initBeforeGameInited");
		if (!gameDisposableInstalled) {
			gameDisposableInstalled = true;
			new GameDisposable() {
				@Override protected void dispose() {
					CorePlatformRuntime.gameDisposing(VERSION.VERSION_STRING, "GameDisposable.disposeAll");
					V7144RaceBridge.disposed();
					V7144RaceAttributeBridge.disposed();
					V7144RaceHomeResourceBridge.disposed();
					V7144CombatDamageBridge.disposed();
					V7144MultiOutputProductionBridge.disposed();
					V7144MultiResourceStorage.disposed();
					ResourceDisplayRuntime.registryDisposed("GameDisposable.disposeAll");
				}
			};
		}
		V7144RuntimeEvidence.installGameplayLifecycleProbe();
	}
	public String adapterVersion() { return "v71_44"; }
	public String gameVersion() { return VERSION.VERSION_STRING; }
}
