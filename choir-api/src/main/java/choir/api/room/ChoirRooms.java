package choir.api.room;

import choir.api.spi.ChoirRuntimeServices;

/** Stable, vanilla-type-free entry point for data-backed room declarations. */
public final class ChoirRooms {
	public static final int API_VERSION = 1;
	private ChoirRooms() { }

	public static RoomRegistrationResult register(RoomDeclaration declaration) {
		return ChoirRuntimeServices.require().registerRoom(declaration);
	}

	public static RoomRegistrationSnapshot snapshot() { return ChoirRuntimeServices.require().roomSnapshot(); }
}
