package choir.adapter.v71_44.options.ui;

/** Presentation-only title casing for translated menu labels. */
public final class V7144MenuText {
	private V7144MenuText() { }

	public static String title(CharSequence value) {
		if (value == null) return "";
		String input = value.toString();
		StringBuilder out = new StringBuilder(input.length());
		boolean firstLetter = true;
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (Character.isLetter(c)) {
				out.append(firstLetter ? Character.toTitleCase(c) : Character.toLowerCase(c));
				firstLetter = false;
			} else {
				out.append(c);
				if (Character.isWhitespace(c) || c == '-' || c == '/' || c == '\\') firstLetter = true;
			}
		}
		return out.toString();
	}
}
