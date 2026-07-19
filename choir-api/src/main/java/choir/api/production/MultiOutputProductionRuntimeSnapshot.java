package choir.api.production;

/** Immutable process diagnostics for multi-output production. */
public final class MultiOutputProductionRuntimeSnapshot {
	private final long registrationGeneration;
	private final int registeredDeclarations;
	private final boolean adapterReady;
	private final String planSignature;
	private final long completedCycles;
	private final long emittedStacks;
	private final long emittedUnits;
	private final long failures;

	public MultiOutputProductionRuntimeSnapshot(long registrationGeneration, int registeredDeclarations,
			boolean adapterReady, String planSignature, long completedCycles, long emittedStacks,
			long emittedUnits, long failures) {
		this.registrationGeneration = registrationGeneration;
		this.registeredDeclarations = registeredDeclarations;
		this.adapterReady = adapterReady;
		this.planSignature = planSignature == null ? "" : planSignature;
		this.completedCycles = completedCycles;
		this.emittedStacks = emittedStacks;
		this.emittedUnits = emittedUnits;
		this.failures = failures;
	}
	public long registrationGeneration() { return registrationGeneration; }
	public int registeredDeclarations() { return registeredDeclarations; }
	public boolean adapterReady() { return adapterReady; }
	public String planSignature() { return planSignature; }
	public long completedCycles() { return completedCycles; }
	public long emittedStacks() { return emittedStacks; }
	public long emittedUnits() { return emittedUnits; }
	public long failures() { return failures; }
}
