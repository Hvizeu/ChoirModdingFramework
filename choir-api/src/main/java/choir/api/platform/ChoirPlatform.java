package choir.api.platform;

import choir.api.spi.ChoirRuntimeServices;

/** Stable Core Platform query facade. Manifests are static files, not runtime registrations. */
public final class ChoirPlatform {
	public static final int API_VERSION = 1;
	private ChoirPlatform() { }
	public static PlatformSnapshot snapshot() { return ChoirRuntimeServices.require().platformSnapshot(); }
	public static boolean isModActive(String modId) { return snapshot().isActive(modId); }
	public static boolean hasCapability(String capability) { return !snapshot().providersOf(capability).isEmpty(); }
	public static String validationSessionId() { return ChoirRuntimeServices.require().validationSessionId(); }
}
