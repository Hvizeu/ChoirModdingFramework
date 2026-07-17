package choir.internal;
import java.util.ArrayList;
import java.util.List;
import choir.api.RoomRegistrationContext;
import choir.api.RoomRegistrationProbe;
public final class RoomRegistrationRegistry {
	private static final List<RoomRegistrationProbe> probes = new ArrayList<RoomRegistrationProbe>();
	private RoomRegistrationRegistry() { }
	public static synchronized boolean register(RoomRegistrationProbe probe) {
		if (probe == null) return false;
		probes.add(probe); return true;
	}
	public static synchronized void openVerifiedWindow(long cycleId, List<String> beforeKeys) {
		RoomRegistrationContext context = new RoomRegistrationContext(new ArrayList<String>(beforeKeys));
		for (RoomRegistrationProbe probe : new ArrayList<RoomRegistrationProbe>(probes)) {
			try { probe.onRegistrationWindow(context); } catch (Throwable t) { ChoirDiagnostics.error("Probe failed: " + t.getClass().getSimpleName()); }
		}
	}
}
