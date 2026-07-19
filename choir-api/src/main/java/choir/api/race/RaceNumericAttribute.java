package choir.api.race;

/** Stable user-facing numeric race attributes supported by the V71 adapter. */
public enum RaceNumericAttribute {
	POPULATION_MAX(false, 0.0, 1.0),
	POPULATION_GROWTH(false, 0.0001, 1.0),
	CLIMATE_PREFERENCE(true, 0.0, 1_000_000.0),
	TERRAIN_PREFERENCE(true, 0.0, 1_000_000.0),
	WORK_PREFERENCE(true, -10_000.0, 10_000.0),
	STRUCTURE_PREFERENCE(true, 0.0, 1.0),
	POOL_PREFERENCE(true, 0.0, 1.0),
	ROAD_PREFERENCE(true, 0.0, 1.0),
	CRIME_FREEDOM(true, 0.0, 1.0),
	CRIME_LAW(true, 0.0, 1.0),
	PUNISHMENT_PREFERENCE(true, 0.0, 1.0),
	RESOURCE_PRICE_MULTIPLIER(true, 0.0, 100.0),
	RESOURCE_PRICE_CAP(true, 0.0, 1.0);

	private final boolean subjectRequired;
	private final double minimum, maximum;
	RaceNumericAttribute(boolean subjectRequired, double minimum, double maximum) {
		this.subjectRequired = subjectRequired; this.minimum = minimum; this.maximum = maximum;
	}
	public boolean subjectRequired() { return subjectRequired; }
	public double minimum() { return minimum; }
	public double maximum() { return maximum; }
}
