package choir.internal.platform;

import choir.api.lifecycle.LifecycleContext;
import choir.api.lifecycle.LifecycleEvent;
import choir.api.lifecycle.LifecycleEvents;
import choir.api.lifecycle.LifecyclePhase;
import choir.internal.ChoirDiagnostics;
import choir.internal.diagnostics.DecisionDiagnostics;
import choir.internal.lifecycle.LifecycleEventBus;
import choir.internal.race.RaceRegistry;

public final class CorePlatformRuntime {
	private static long sequence;
	private static long runtimeGeneration;
	private static long disposedGeneration;
	private CorePlatformRuntime() { }
	public static synchronized void beforeGameCreated(String gameVersion) {
		runtimeGeneration++;
		DecisionDiagnostics.beginWorld(runtimeGeneration);
		publish(LifecycleEvents.BEFORE_GAME_CREATED, LifecyclePhase.BEFORE_GAME_CREATED, gameVersion, "SCRIPT.initBeforeGameCreated");
	}
	public static synchronized void gameInitialized(String gameVersion) { publish(LifecycleEvents.GAME_INITIALIZED, LifecyclePhase.GAME_INITIALIZED, gameVersion, "SCRIPT.initBeforeGameInited"); }
	public static synchronized void instanceCreated(String gameVersion) { publish(LifecycleEvents.INSTANCE_CREATED, LifecyclePhase.INSTANCE_CREATED, gameVersion, "SCRIPT.createInstance"); }
	public static synchronized void gameplayReached(String gameVersion, String marker) { publish(LifecycleEvents.GAMEPLAY_REACHED, LifecyclePhase.GAMEPLAY_REACHED, gameVersion, marker); }
	public static synchronized void gameDisposing(String gameVersion, String marker) {
		if (runtimeGeneration == 0 || disposedGeneration == runtimeGeneration) return;
		disposedGeneration = runtimeGeneration;
		publish(LifecycleEvents.GAME_DISPOSING, LifecyclePhase.GAME_DISPOSING, gameVersion, marker);
		DecisionDiagnostics.endWorld();
		RaceRegistry.disposeRuntime();
	}
	public static synchronized long runtimeGeneration() { return runtimeGeneration; }
	static synchronized void resetForTests() { sequence = 0; runtimeGeneration = 0; disposedGeneration = 0; }
	private static void publish(LifecycleEvent<LifecycleContext> event, LifecyclePhase phase, String gameVersion, String marker) {
		sequence++;
		LifecycleContext context = new LifecycleContext(phase, sequence, runtimeGeneration,
				ChoirDiagnostics.validationSessionId(), gameVersion, marker);
		LifecycleEventBus.publish(event, context);
	}
}
