package choir.api;

import choir.api.spi.ChoirRuntimeServices;

public final class Choir {
	public static final String VERSION = "0.9.0";
	private Choir() { }
	public static String version() { return VERSION; }
	public static boolean hasCapability(Capability capability) {
		return ChoirRuntimeServices.isInstalled() && ChoirRuntimeServices.require().hasCapability(capability);
	}
	public static boolean registerRoomRegistrationProbe(RoomRegistrationProbe probe) {
		return ChoirRuntimeServices.isInstalled() && ChoirRuntimeServices.require().registerRoomRegistrationProbe(probe);
	}
}
