package choir.internal.options;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonUtil {

	private JsonUtil() {
	}

	static String stringify(Object value) {
		StringBuilder out = new StringBuilder();
		write(out, value);
		return out.toString();
	}

	@SuppressWarnings("unchecked")
	private static void write(StringBuilder out, Object value) {
		if (value == null) {
			out.append("null");
		} else if (value instanceof String) {
			writeString(out, (String) value);
		} else if (value instanceof Number || value instanceof Boolean) {
			out.append(String.valueOf(value));
		} else if (value instanceof Map<?, ?>) {
			out.append("{");
			boolean first = true;
			for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
				if (!first) {
					out.append(",");
				}
				first = false;
				writeString(out, entry.getKey());
				out.append(":");
				write(out, entry.getValue());
			}
			out.append("}");
		} else if (value instanceof Iterable<?>) {
			out.append("[");
			boolean first = true;
			for (Object item : (Iterable<?>) value) {
				if (!first) {
					out.append(",");
				}
				first = false;
				write(out, item);
			}
			out.append("]");
		} else {
			writeString(out, String.valueOf(value));
		}
	}

	private static void writeString(StringBuilder out, String value) {
		out.append('"');
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
			case '"':
				out.append("\\\"");
				break;
			case '\\':
				out.append("\\\\");
				break;
			case '\n':
				out.append("\\n");
				break;
			case '\r':
				out.append("\\r");
				break;
			case '\t':
				out.append("\\t");
				break;
			default:
				if (c < 32) {
					out.append("\\u");
					String hex = Integer.toHexString(c);
					for (int j = hex.length(); j < 4; j++) {
						out.append('0');
					}
					out.append(hex);
				} else {
					out.append(c);
				}
			}
		}
		out.append('"');
	}

	static Object parse(String text) {
		Parser parser = new Parser(text);
		Object value = parser.value();
		parser.skip();
		if (!parser.end()) {
			throw new IllegalArgumentException("Unexpected trailing JSON at character " + parser.pos);
		}
		return value;
	}

	private static final class Parser {
		private final String text;
		private int pos;

		Parser(String text) {
			this.text = text == null ? "" : text;
		}

		boolean end() {
			return pos >= text.length();
		}

		void skip() {
			while (!end()) {
				char c = text.charAt(pos);
				if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
					pos++;
				} else {
					return;
				}
			}
		}

		Object value() {
			skip();
			if (end()) {
				throw new IllegalArgumentException("Unexpected end of JSON");
			}
			char c = text.charAt(pos);
			if (c == '"') {
				return string();
			}
			if (c == '{') {
				return object();
			}
			if (c == '[') {
				return array();
			}
			if (c == 't' && match("true")) {
				return Boolean.TRUE;
			}
			if (c == 'f' && match("false")) {
				return Boolean.FALSE;
			}
			if (c == 'n' && match("null")) {
				return null;
			}
			if (c == '-' || (c >= '0' && c <= '9')) {
				return number();
			}
			throw new IllegalArgumentException("Unexpected JSON token at character " + pos);
		}

		private boolean match(String value) {
			if (text.regionMatches(pos, value, 0, value.length())) {
				pos += value.length();
				return true;
			}
			return false;
		}

		private Map<String, Object> object() {
			LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
			pos++;
			skip();
			if (!end() && text.charAt(pos) == '}') {
				pos++;
				return map;
			}
			while (true) {
				skip();
				String key = string();
				skip();
				expect(':');
				map.put(key, value());
				skip();
				if (!end() && text.charAt(pos) == '}') {
					pos++;
					return map;
				}
				expect(',');
			}
		}

		private List<Object> array() {
			ArrayList<Object> list = new ArrayList<Object>();
			pos++;
			skip();
			if (!end() && text.charAt(pos) == ']') {
				pos++;
				return list;
			}
			while (true) {
				list.add(value());
				skip();
				if (!end() && text.charAt(pos) == ']') {
					pos++;
					return list;
				}
				expect(',');
			}
		}

		private String string() {
			expect('"');
			StringBuilder out = new StringBuilder();
			while (!end()) {
				char c = text.charAt(pos++);
				if (c == '"') {
					return out.toString();
				}
				if (c == '\\') {
					if (end()) {
						throw new IllegalArgumentException("Unterminated JSON escape");
					}
					char e = text.charAt(pos++);
					switch (e) {
					case '"':
					case '\\':
					case '/':
						out.append(e);
						break;
					case 'b':
						out.append('\b');
						break;
					case 'f':
						out.append('\f');
						break;
					case 'n':
						out.append('\n');
						break;
					case 'r':
						out.append('\r');
						break;
					case 't':
						out.append('\t');
						break;
					case 'u':
						if (pos + 4 > text.length()) {
							throw new IllegalArgumentException("Short unicode escape");
						}
						out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
						pos += 4;
						break;
					default:
						throw new IllegalArgumentException("Bad JSON escape: " + e);
					}
				} else {
					out.append(c);
				}
			}
			throw new IllegalArgumentException("Unterminated JSON string");
		}

		private Number number() {
			int start = pos;
			if (text.charAt(pos) == '-') {
				pos++;
			}
			while (!end() && Character.isDigit(text.charAt(pos))) {
				pos++;
			}
			boolean decimal = false;
			if (!end() && text.charAt(pos) == '.') {
				decimal = true;
				pos++;
				while (!end() && Character.isDigit(text.charAt(pos))) {
					pos++;
				}
			}
			if (!end() && (text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
				decimal = true;
				pos++;
				if (!end() && (text.charAt(pos) == '+' || text.charAt(pos) == '-')) {
					pos++;
				}
				while (!end() && Character.isDigit(text.charAt(pos))) {
					pos++;
				}
			}
			String raw = text.substring(start, pos);
			return decimal ? Double.valueOf(raw) : Long.valueOf(raw);
		}

		private void expect(char c) {
			if (end() || text.charAt(pos) != c) {
				throw new IllegalArgumentException("Expected '" + c + "' at character " + pos);
			}
			pos++;
		}
	}
}
