package choir.internal.platform;

final class DependencyRequirement {
	final String modId;
	final String operator;
	final SemanticVersion version;
	private DependencyRequirement(String modId, String operator, SemanticVersion version) {
		this.modId = modId; this.operator = operator; this.version = version;
	}
	static DependencyRequirement parse(String text) {
		String[] pair = text.trim().split("@", 2);
		String id = pair[0].trim();
		if (!id.matches("[a-z][a-z0-9._-]*")) throw new IllegalArgumentException("Invalid dependency ID: " + id);
		if (pair.length == 1 || pair[1].trim().isEmpty() || "*".equals(pair[1].trim())) return new DependencyRequirement(id, "*", null);
		String constraint = pair[1].trim();
		for (String operator : new String[] { ">=", "<=", ">", "<", "=" }) {
			if (constraint.startsWith(operator)) return new DependencyRequirement(id, operator, SemanticVersion.parse(constraint.substring(operator.length()).trim()));
		}
		return new DependencyRequirement(id, "=", SemanticVersion.parse(constraint));
	}
	boolean accepts(String actual) {
		if ("*".equals(operator)) return true;
		int comparison = SemanticVersion.parse(actual).compareTo(version);
		return switch (operator) { case ">=" -> comparison >= 0; case "<=" -> comparison <= 0; case ">" -> comparison > 0; case "<" -> comparison < 0; default -> comparison == 0; };
	}
}
