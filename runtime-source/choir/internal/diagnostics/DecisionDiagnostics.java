package choir.internal.diagnostics;

import java.util.LinkedHashMap;
import java.util.Map;

import choir.internal.ChoirDiagnostics;
import choir.internal.options.OptionsRegistry;
import choir.internal.storage.AdvancedStoragePolicy;

/**
 * Bounded, world-scoped diagnostics for gameplay decisions.
 *
 * <p>Hot game objects are converted to stable strings and numbers before they
 * reach this class. Repeated events are counted and summarized instead of
 * synchronously writing one line for every worker attempt.</p>
 */
public final class DecisionDiagnostics {
	public static final String DETAILED_OPTION_KEY = "detailed_framework_diagnostics";

	private static final int MAX_KEYS = 512;
	private static final int MAX_EMISSIONS_PER_SECOND = 25;
	private static final long PROBLEM_INTERVAL_MS = 10_000L;
	private static final long DETAIL_INTERVAL_MS = 5_000L;
	private static final Map<String, EventState> events = new LinkedHashMap<String, EventState>();
	private static long worldGeneration;
	private static long droppedKeys;
	private static long rateLimitedEmissions;
	private static long emissionWindowStartedMs;
	private static int emissionsInWindow;

	private DecisionDiagnostics() { }

	/** Starts a clean, non-serialized diagnostic generation for a new world. */
	public static synchronized void beginWorld(long generation) {
		events.clear();
		droppedKeys = 0;
		rateLimitedEmissions = 0;
		emissionWindowStartedMs = 0;
		emissionsInWindow = 0;
		worldGeneration = generation;
	}

	/** Flushes one bounded summary and releases every world-scoped key. */
	public static synchronized void endWorld() {
		long suppressed = 0;
		for (EventState state : events.values()) suppressed += state.suppressed;
		if (suppressed > 0 || droppedKeys > 0 || rateLimitedEmissions > 0)
			ChoirDiagnostics.info("DECISION-DIAGNOSTICS world-summary generation=" + worldGeneration
					+ " tracked.keys=" + events.size() + " suppressed.events=" + suppressed
					+ " dropped.keys=" + droppedKeys
					+ " rate-limited.emissions=" + rateLimitedEmissions);
		events.clear();
		droppedKeys = 0;
		rateLimitedEmissions = 0;
		emissionWindowStartedMs = 0;
		emissionsInWindow = 0;
	}

	/** Returns the number of attempts represented by an allowed problem line, or zero. */
	public static int permitProblem(String stableKey) {
		return permit("problem/" + stableKey, PROBLEM_INTERVAL_MS);
	}

	/** Returns the number of attempts represented by an allowed detailed line, or zero. */
	public static int permitDetail(String stableKey) {
		if (!detailedEnabled()) return 0;
		return permit("detail/" + stableKey, DETAIL_INTERVAL_MS);
	}

	public static void problem(String eventCode, int attempts, String detail) {
		if (attempts <= 0) return;
		ChoirDiagnostics.info("DECISION problem=" + eventCode + " generation=" + generation()
				+ " attempts=" + attempts + " " + detail);
	}

	public static void detail(String eventCode, int attempts, String detail) {
		if (attempts <= 0) return;
		ChoirDiagnostics.info("DECISION detail=" + eventCode + " generation=" + generation()
				+ " attempts=" + attempts + " " + detail);
	}

	public static boolean detailedEnabled() {
		return OptionsRegistry.getBoolean(AdvancedStoragePolicy.PROVIDER_ID,
				DETAILED_OPTION_KEY, false);
	}

	private static synchronized int permit(String key, long intervalMs) {
		EventState state = events.get(key);
		if (state == null) {
			if (events.size() >= MAX_KEYS) {
				droppedKeys++;
				return 0;
			}
			state = new EventState();
			events.put(key, state);
		}
		state.attempts++;
		long now = System.currentTimeMillis();
		if (state.lastEmission != 0 && now-state.lastEmission < intervalMs) {
			state.suppressed++;
			return 0;
		}
		if (!emissionAllowed(now)) {
			state.suppressed++;
			rateLimitedEmissions++;
			return 0;
		}
		int represented = state.attempts;
		state.attempts = 0;
		state.suppressed = 0;
		state.lastEmission = now;
		return represented;
	}

	private static boolean emissionAllowed(long now) {
		if (emissionWindowStartedMs == 0 || now-emissionWindowStartedMs >= 1_000L
				|| now < emissionWindowStartedMs) {
			emissionWindowStartedMs = now;
			emissionsInWindow = 0;
		}
		if (emissionsInWindow >= MAX_EMISSIONS_PER_SECOND) return false;
		emissionsInWindow++;
		return true;
	}

	private static synchronized long generation() { return worldGeneration; }

	private static final class EventState {
		long lastEmission;
		int attempts;
		int suppressed;
	}
}
