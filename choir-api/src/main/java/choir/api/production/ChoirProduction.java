package choir.api.production;

import choir.api.spi.ChoirRuntimeServices;

/**
 * Public, vanilla-type-free facade for bounded multi-output production targets.
 *
 * <p>Consumers register immutable recipe descriptors rather than inheriting from
 * a Choir or vanilla building class. A valid active registration automatically
 * enables advanced internal storage for only its exact room/industry target.</p>
 */
public final class ChoirProduction {
	public static final int API_VERSION = 1;
	private ChoirProduction() { }
	/**
	 * Registers one normal-data-backed workshop/refiner recipe with two to five outputs.
	 * The declaration is also the exact target's advanced-storage opt-in and does not
	 * depend on the player's global storage extension.
	 */
	public static MultiOutputRegistrationResult registerMultiOutputRoom(MultiOutputRoomDeclaration declaration) {
		return ChoirRuntimeServices.require().registerMultiOutputRoom(declaration);
	}
	public static MultiOutputProductionRuntimeSnapshot runtimeSnapshot() {
		return ChoirRuntimeServices.require().multiOutputProductionRuntimeSnapshot();
	}
}
