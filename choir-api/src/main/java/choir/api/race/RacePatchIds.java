package choir.api.race;

final class RacePatchIds {
	private RacePatchIds() { }
	static String patch(String id) {
		if (id == null || !id.matches("[a-z][a-z0-9._-]*")) throw new IllegalArgumentException("Invalid patch ID: " + id);
		return id;
	}
	static String subject(String id) {
		if (id == null || !id.matches("[A-Z][A-Z0-9_.*:-]*")) throw new IllegalArgumentException("Invalid stable subject ID: " + id);
		return id;
	}
}
