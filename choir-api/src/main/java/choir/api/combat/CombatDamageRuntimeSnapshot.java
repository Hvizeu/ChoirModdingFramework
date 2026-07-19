package choir.api.combat;

/** Immutable process diagnostics for the combat-damage composition domain. */
public final class CombatDamageRuntimeSnapshot {
	private final long registrationGeneration;
	private final int registeredModifiers;
	private final boolean adapterReady;
	private final String planSignature;
	private final long applications;
	private final long failures;

	public CombatDamageRuntimeSnapshot(long registrationGeneration, int registeredModifiers,
			boolean adapterReady, String planSignature, long applications, long failures) {
		this.registrationGeneration = registrationGeneration;
		this.registeredModifiers = registeredModifiers;
		this.adapterReady = adapterReady;
		this.planSignature = planSignature == null ? "" : planSignature;
		this.applications = applications;
		this.failures = failures;
	}
	public long registrationGeneration() { return registrationGeneration; }
	public int registeredModifiers() { return registeredModifiers; }
	public boolean adapterReady() { return adapterReady; }
	public String planSignature() { return planSignature; }
	public long applications() { return applications; }
	public long failures() { return failures; }
}
