package choir.internal.platform;

import java.util.ArrayList;
import java.util.List;

final class SemanticVersion implements Comparable<SemanticVersion> {
	private final List<Integer> parts;
	private SemanticVersion(List<Integer> parts) { this.parts = parts; }
	static SemanticVersion parse(String value) {
		if (value == null) throw new IllegalArgumentException("Version is missing.");
		String core = value.trim().split("[-+]", 2)[0];
		String[] tokens = core.split("\\.");
		ArrayList<Integer> parts = new ArrayList<Integer>();
		for (String token : tokens) {
			if (!token.matches("[0-9]+")) throw new IllegalArgumentException("Unsupported semantic version: " + value);
			parts.add(Integer.valueOf(token));
		}
		while (parts.size() < 3) parts.add(Integer.valueOf(0));
		return new SemanticVersion(parts);
	}
	@Override public int compareTo(SemanticVersion other) {
		int count = Math.max(parts.size(), other.parts.size());
		for (int i = 0; i < count; i++) {
			int a = i < parts.size() ? parts.get(i).intValue() : 0;
			int b = i < other.parts.size() ? other.parts.get(i).intValue() : 0;
			if (a != b) return Integer.compare(a, b);
		}
		return 0;
	}
}
