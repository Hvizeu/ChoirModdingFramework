package choir.adapter.v71_44;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import choir.api.room.RoomDeclaration;
import choir.internal.ChoirDiagnostics;
import choir.internal.ChoirBootstrap;
import choir.internal.RoomRegistrationRegistry;
import choir.internal.room.RoomRegistry;
import settlement.room.main.RoomBlueprint;
import settlement.room.main.util.RoomInitData;
public final class RoomRegistrationBridge {
	private enum ProcessState { PROCESS_UNINITIALIZED, PROCESS_VERIFIED, PROCESS_FAILED }
	private static ProcessState processState = ProcessState.PROCESS_UNINITIALIZED;
	private static final RegistryCycleStateMachine cycles = new RegistryCycleStateMachine();
	private static int processAttempts, processAccepted, processRejected;
	private RoomRegistrationBridge() { }
	static synchronized void arm() {
		if (processState == ProcessState.PROCESS_VERIFIED) return;
		if (processState != ProcessState.PROCESS_UNINITIALIZED) throw new IllegalStateException("Bridge process cannot arm in " + processState);
		processState = ProcessState.PROCESS_VERIFIED;
		new game.GameDisposable() { @Override protected void dispose() { cycles.disposeCycle(); RoomRegistry.registryDisposed(); } };
	}
	public static synchronized String state() { return processState + " processAttempts=" + processAttempts + " processAccepted=" + processAccepted + " processRejected=" + processRejected + " cycles=" + cycles.state(); }
	public static synchronized void processFailed() { processState = ProcessState.PROCESS_FAILED; }
	public static synchronized void creatorConstructed(String family, int identity) {
		if (processState != ProcessState.PROCESS_VERIFIED) throw new IllegalStateException("Registry creator observed while process state=" + processState);
		long started = cycles.creatorConstructed(family);
		if (started > 0 && "MONUMENT".equals(family)) {
			ChoirDiagnostics.info("REGISTRY-CYCLE begin id=" + started + " trigger=MONUMENT");
			V7144RaceBridge.registryCycleStarted(started);
		}
		if (started > 0) ChoirDiagnostics.info("REGISTRY-CYCLE id=" + started + " creator=" + family + " identity=" + identity);
	}
	public static synchronized void afterFinalVanillaFamily(String family, RoomInitData init) throws IOException {
		processAttempts++;
		long cycleId;
		try { cycleId = cycles.beginFinalization(family); processAccepted++; } catch (RuntimeException e) { processRejected++; throw e; }
		List<String> before = keys();
		ChoirDiagnostics.info("BRIDGE accepted cycle=" + cycleId + " family=" + family + " stable.registered=" + RoomRegistry.pendingCount() + " stable.materialized=" + RoomRegistry.totalMaterializations() + " before.count=" + before.size() + " before.keys.sha256=" + fingerprint(before));
		try {
			RoomRegistrationRegistry.openVerifiedWindow(cycleId, before);
			List<RoomDeclaration> stable = RoomRegistry.snapshotForCycle(cycleId);
			int materializedThisCycle = 0;
			for (RoomDeclaration declaration : stable) {
				if (before.contains(declaration.roomKey())) throw new IllegalStateException("Stable room key already exists before materialization: " + declaration.roomKey());
				int runtimeIndex = StablePassiveRoomBlueprint.materialize(init, declaration);
				RoomRegistry.materialized(cycleId, declaration, runtimeIndex);
				materializedThisCycle++;
			}
			List<String> after = keys();
			ChoirDiagnostics.info("BRIDGE cycle=" + cycleId + " after.count=" + after.size() + " after.keys.sha256=" + fingerprint(after) + " stable.materialized.thisCycle=" + materializedThisCycle);
			if (after.size() != before.size() + materializedThisCycle || !after.subList(0, before.size()).equals(before)) throw new IllegalStateException("RoomBlueprint.ALL changed outside Choir's deterministic append rule.");
			int appended = before.size();
			for (RoomDeclaration declaration : stable) if (!after.get(appended++).equals(declaration.roomKey())) throw new IllegalStateException("Stable room materialization order mismatch.");
			RoomRegistry.completeCycle(cycleId);
			cycles.complete(cycleId);
		} catch (IOException e) {
			RoomRegistry.failCycle(cycleId); cycles.failFinalization(cycleId); ChoirDiagnostics.error("REGISTRY-CYCLE failed id=" + cycleId + " cause=" + e.getClass().getSimpleName()); throw e;
		} catch (RuntimeException e) {
			RoomRegistry.failCycle(cycleId); cycles.failFinalization(cycleId); ChoirDiagnostics.error("REGISTRY-CYCLE failed id=" + cycleId + " cause=" + e.getClass().getSimpleName()); throw e;
		}
		ChoirDiagnostics.info("REGISTRY-CYCLE complete id=" + cycleId + " " + cycles.state() + " process.attempts=" + processAttempts + " process.accepted=" + processAccepted + " process.rejected=" + processRejected);
		ChoirBootstrap.roomSpikeRuntimeConfirmed();
	}
	@SuppressWarnings("unchecked") private static List<String> keys() {
		try { Field all = RoomBlueprint.class.getDeclaredField("ALL"); all.setAccessible(true); Iterable<RoomBlueprint> values = (Iterable<RoomBlueprint>) all.get(null); List<String> out = new ArrayList<String>(); for (RoomBlueprint value : values) out.add(value.key); return out; }
		catch (Exception e) { throw new IllegalStateException("Could not inspect RoomBlueprint.ALL", e); }
	}
	private static String fingerprint(List<String> keys) { try { java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256"); return hex(digest.digest(String.join("\u0000", keys).getBytes(java.nio.charset.StandardCharsets.UTF_8))); } catch (Exception e) { return "<error:" + e.getClass().getSimpleName() + ">"; } }
	private static String hex(byte[] bytes) { StringBuilder out = new StringBuilder(); for (byte b : bytes) out.append(String.format("%02x", b & 255)); return out.toString(); }
}
