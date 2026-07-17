package choir.api.race;

/** Multiplicative patch for one existing or data-backed race boostable. */
public final class RaceBoostPatch {
	private final String providerId;
	private final String patchId;
	private final String raceId;
	private final String boostableId;
	private final int priority;
	private final double multiplier;
	public RaceBoostPatch(String providerId, String patchId, String raceId, String boostableId, int priority, double multiplier) {
		this.providerId = RaceDeclaration.provider(providerId);
		if (patchId == null || !patchId.matches("[a-z][a-z0-9._-]*")) throw new IllegalArgumentException("Invalid patch ID: " + patchId);
		this.patchId = patchId; this.raceId = RaceDeclaration.race(raceId);
		if (boostableId == null || !boostableId.matches("[A-Z][A-Z0-9_*]*")) throw new IllegalArgumentException("Invalid boostable ID: " + boostableId);
		if (!Double.isFinite(multiplier) || multiplier <= 0.0) throw new IllegalArgumentException("Race boost multiplier must be finite and positive.");
		this.boostableId = boostableId; this.priority = priority; this.multiplier = multiplier;
	}
	public String providerId() { return providerId; }
	public String patchId() { return patchId; }
	public String raceId() { return raceId; }
	public String boostableId() { return boostableId; }
	public int priority() { return priority; }
	public double multiplier() { return multiplier; }
}
