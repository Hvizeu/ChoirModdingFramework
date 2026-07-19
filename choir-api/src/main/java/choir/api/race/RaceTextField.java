package choir.api.race;

/**
 * Stable names for user-facing race prose and phrase collections.
 * No V71 implementation class is exposed through this enum.
 */
public enum RaceTextField {
	LONG_DESCRIPTION(false),
	INITIAL_CHALLENGE(false),
	PROS(true),
	CONS(true),
	ARMY_NAMES(true),
	RAIDER_NAMES(true),
	HELLO(true),
	GOODBYE(true),
	CURSE(true),
	INSULT(true),
	INSULTING(true),
	LORD(true),
	CITY(true),
	OTHERS(true),
	SELVES(true),
	SELF(true),
	CHILDREN(true);

	private final boolean collection;
	RaceTextField(boolean collection) { this.collection = collection; }
	public boolean isCollection() { return collection; }
}
