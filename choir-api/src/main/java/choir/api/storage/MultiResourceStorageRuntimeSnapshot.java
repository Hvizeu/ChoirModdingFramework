package choir.api.storage;

/** Immutable diagnostics for Choir's opt-in advanced multi-resource storage subsystem. */
public final class MultiResourceStorageRuntimeSnapshot {
	private final long registrationGeneration;
	private final int registeredRoomPolicies;
	private final boolean adapterReady;
	private final String planSignature;
	private final int defaultMaxResourceKinds;
	private final int hardMaxResourceKinds;
	private final long liveCells;
	private final long storedUnits;
	private final long pickupReservations;
	private final long incomingReservations;
	private final long failures;

	public MultiResourceStorageRuntimeSnapshot(long registrationGeneration, int registeredRoomPolicies,
			boolean adapterReady, String planSignature, int defaultMaxResourceKinds,
			int hardMaxResourceKinds, long liveCells, long storedUnits,
			long pickupReservations, long incomingReservations, long failures) {
		this.registrationGeneration = registrationGeneration;
		this.registeredRoomPolicies = registeredRoomPolicies;
		this.adapterReady = adapterReady;
		this.planSignature = planSignature == null ? "" : planSignature;
		this.defaultMaxResourceKinds = defaultMaxResourceKinds;
		this.hardMaxResourceKinds = hardMaxResourceKinds;
		this.liveCells = liveCells;
		this.storedUnits = storedUnits;
		this.pickupReservations = pickupReservations;
		this.incomingReservations = incomingReservations;
		this.failures = failures;
	}

	public long registrationGeneration() { return registrationGeneration; }
	public int registeredRoomPolicies() { return registeredRoomPolicies; }
	public boolean adapterReady() { return adapterReady; }
	public String planSignature() { return planSignature; }
	public int defaultMaxResourceKinds() { return defaultMaxResourceKinds; }
	public int hardMaxResourceKinds() { return hardMaxResourceKinds; }
	public long liveCells() { return liveCells; }
	public long storedUnits() { return storedUnits; }
	public long pickupReservations() { return pickupReservations; }
	public long incomingReservations() { return incomingReservations; }
	public long failures() { return failures; }
}
