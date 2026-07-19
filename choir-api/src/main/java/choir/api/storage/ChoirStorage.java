package choir.api.storage;

import choir.api.spi.ChoirRuntimeServices;

/**
 * Public, vanilla-type-free facade for Choir's opt-in advanced-storage policies.
 *
 * <p>Ordinary supported rooms remain on vanilla storage unless the player enables
 * Choir's world-load option. A room policy registered here opts the whole named
 * room blueprint in independently of that option. Consumers register immutable
 * descriptors; they do not subclass Choir or vanilla room implementations.</p>
 */
public final class ChoirStorage {
	public static final int API_VERSION = 1;
	/** Singular vanilla storage exposes one resource kind per physical tile. */
	public static final int VANILLA_MAX_RESOURCE_KINDS = 1;
	public static final int DEFAULT_MAX_RESOURCE_KINDS = 8;
	public static final int HARD_MAX_RESOURCE_KINDS = 16;

	private ChoirStorage() { }

	/**
	 * Opts one room blueprint into advanced storage and selects its resource-kind limit.
	 *
	 * <p>The policy applies to every supported storage tile owned by the named room,
	 * even when the player's global advanced-storage extension is disabled.
	 * Production-room tiles share their physical capacity dynamically. Stockpile shelves expose the
	 * configured number of user-assigned sections; each resource receives capacity in proportion to
	 * its section count, and the sum of every section never exceeds the shelf's physical capacity.</p>
	 *
	 * @param declaration immutable stable-ID room descriptor owned by the provider
	 * @return deterministic acceptance, idempotence, or conflict result
	 */
	public static MultiResourceStorageRegistrationResult registerRoomPolicy(
			MultiResourceStorageDeclaration declaration) {
		return ChoirRuntimeServices.require().registerMultiResourceStorage(declaration);
	}

	/** Returns process diagnostics without exposing V71.44 room or resource objects. */
	public static MultiResourceStorageRuntimeSnapshot runtimeSnapshot() {
		return ChoirRuntimeServices.require().multiResourceStorageRuntimeSnapshot();
	}
}
