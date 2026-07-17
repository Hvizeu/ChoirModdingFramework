package choir.adapter.v71_44;

final class RegistryCycleStateMachine {
	enum CycleState { NO_ACTIVE_CYCLE, CYCLE_BUILDING, CYCLE_FINALIZING, CYCLE_CONFIRMED, CYCLE_FAILED }
	private long nextId;
	private long activeId;
	private CycleState state = CycleState.NO_ACTIVE_CYCLE;
	private int attempts, accepted, rejected;

	synchronized long creatorConstructed(String family) {
		if ("MONUMENT".equals(family)) {
			if (state == CycleState.CYCLE_BUILDING || state == CycleState.CYCLE_FINALIZING) fail("nested MONUMENT while cycle=" + activeId + " is active");
			activeId = ++nextId; attempts = accepted = rejected = 0; state = CycleState.CYCLE_BUILDING;
			return activeId;
		}
		if (state == CycleState.CYCLE_BUILDING) return activeId;
		if (state == CycleState.CYCLE_CONFIRMED || state == CycleState.NO_ACTIVE_CYCLE) return 0;
		fail("creator=" + family + " arrived in state=" + state); return 0;
	}

	synchronized long beginFinalization(String family) {
		attempts++;
		if (!"BREEDER".equals(family) || state != CycleState.CYCLE_BUILDING || activeId == 0) { rejected++; fail("final=" + family + " without valid active cycle"); }
		state = CycleState.CYCLE_FINALIZING; accepted++; return activeId;
	}

	synchronized void complete(long cycleId) {
		if (state != CycleState.CYCLE_FINALIZING || cycleId != activeId) fail("completion mismatch cycle=" + cycleId + " active=" + activeId);
		state = CycleState.CYCLE_CONFIRMED;
	}

	synchronized void failFinalization(long cycleId) {
		if (activeId == cycleId && state != CycleState.CYCLE_FAILED) state = CycleState.CYCLE_FAILED;
	}

	synchronized String state() { return state + " active=" + activeId + " totalCycles=" + nextId + " attempts=" + attempts + " accepted=" + accepted + " rejected=" + rejected; }
	synchronized void disposeCycle() { if (state == CycleState.CYCLE_BUILDING || state == CycleState.CYCLE_FINALIZING) fail("game disposed during incomplete cycle=" + activeId); }
	private void fail(String message) { state = CycleState.CYCLE_FAILED; throw new IllegalStateException("Registry-cycle violation: " + message); }
}
