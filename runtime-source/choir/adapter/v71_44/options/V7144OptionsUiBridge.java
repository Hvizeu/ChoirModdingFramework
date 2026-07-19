package choir.adapter.v71_44.options;

import choir.internal.ChoirDiagnostics;
import choir.internal.options.OptionsCapabilities;
import choir.internal.storage.AdvancedStoragePolicy;
import game.VERSION;

/** Lazy because the main-menu shadow may be constructed before Choir's script callback. */
public final class V7144OptionsUiBridge {
	private static boolean verified;
	private static boolean available;

	private V7144OptionsUiBridge() { }

	public static synchronized boolean initialize() {
		// The framework page must exist even when the menu is opened before the
		// non-selectable Choir script receives its first lifecycle callback.
		AdvancedStoragePolicy.registerOptions();
		if (verified) return available;
		verified = true;
		ChoirDiagnostics.beginValidationSession();
		ChoirDiagnostics.logBuildIdentity();
		if (VERSION.VERSION_MAJOR != 71 || VERSION.VERSION_MINOR != 44) {
			String detail = "unsupported game version " + VERSION.VERSION_STRING;
			OptionsCapabilities.uiCompatibility(false, detail);
			ChoirDiagnostics.error("OPTIONS-UI compatibility-fingerprints matched=false detail=" + detail);
			return false;
		}
		OptionsUiTargetFingerprintVerifier.Result result = OptionsUiTargetFingerprintVerifier.verify();
		available = result.matches;
		OptionsCapabilities.uiCompatibility(available, result.detail);
		ChoirDiagnostics.info("OPTIONS-UI compatibility-fingerprints matched=" + result.matches + " signature="
				+ result.signature + " gameJar=" + result.gameJar + " detail=" + result.detail);
		return available;
	}

	public static boolean mainMenuAvailable() { return initialize(); }
	public static boolean inGameAvailable() { return initialize(); }
}
