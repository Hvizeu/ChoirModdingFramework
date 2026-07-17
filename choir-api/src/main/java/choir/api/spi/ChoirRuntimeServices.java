package choir.api.spi;

/**
 * Internal service locator used by Choir's public facades.
 *
 * <p>This is public only because the V71.44 runtime is packaged separately
 * from {@code choir-api}. It is not a consumer-mod extension point.</p>
 */
public final class ChoirRuntimeServices {
	private static volatile ChoirRuntimeService current;

	private ChoirRuntimeServices() { }

	/** Installed by the matching Choir runtime exactly once per classloader. */
	public static synchronized void install(ChoirRuntimeService service) {
		if (service == null) throw new IllegalArgumentException("Choir runtime service must not be null.");
		if (current == null) {
			current = service;
			return;
		}
		if (current != service)
			throw new IllegalStateException("A different Choir runtime service is already installed for this classloader.");
	}

	/** True only when the matching Choir runtime has installed its bridge. */
	public static boolean isInstalled() { return current != null; }

	/** Returns the matching runtime bridge or fails with an actionable classpath diagnostic. */
	public static ChoirRuntimeService require() {
		ChoirRuntimeService service = current;
		if (service == null) {
			throw new IllegalStateException("Choir API is present but no matching Choir runtime bridge is installed. "
					+ "Install and enable Choir Modding Framework as a separate mod; do not embed choir-api.");
		}
		return service;
	}
}
