package choir.api.options;

import choir.api.spi.ChoirRuntimeServices;

/** Stable, declarative entry point for mod-owned option pages. */
public final class ChoirOptions {
	public static final int API_VERSION = 1;

	private ChoirOptions() { }

	public static boolean isAvailable() { return ChoirRuntimeServices.isInstalled(); }
	public static String frameworkVersion() { return choir.api.Choir.VERSION; }
	public static OptionRegistrationResult register(OptionSchema schema) { return ChoirRuntimeServices.require().registerOptions(schema); }
	public static OptionListenerRegistrationResult subscribe(String providerId, OptionChangeListener listener) {
		return ChoirRuntimeServices.require().subscribeOptions(providerId, listener);
	}
	public static boolean getBoolean(String providerId, String key, boolean fallback) { return ChoirRuntimeServices.require().getBooleanOption(providerId, key, fallback); }
	public static int getInt(String providerId, String key, int fallback) { return ChoirRuntimeServices.require().getIntOption(providerId, key, fallback); }
	public static double getDouble(String providerId, String key, double fallback) { return ChoirRuntimeServices.require().getDoubleOption(providerId, key, fallback); }
	public static String getString(String providerId, String key, String fallback) { return ChoirRuntimeServices.require().getStringOption(providerId, key, fallback); }
	public static boolean resetToDefaults(String providerId) { return ChoirRuntimeServices.require().resetOptionsToDefaults(providerId); }
}
