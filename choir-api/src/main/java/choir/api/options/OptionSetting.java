package choir.api.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable declarative option row. No Songs of Syx UI type is exposed. */
public final class OptionSetting {
	private final String key;
	private final OptionType type;
	private final String label;
	private final String description;
	private final Object defaultValue;
	private final Double minimum;
	private final Double maximum;
	private final Double step;
	private final List<String> enumValues;
	private final OptionApplyMode applyMode;
	private final OptionScope scope;
	private final int schemaVersion;
	private final boolean readOnly;

	private OptionSetting(Builder builder) {
		key = builder.key;
		type = builder.type;
		label = builder.label;
		description = builder.description;
		defaultValue = builder.defaultValue;
		minimum = builder.minimum;
		maximum = builder.maximum;
		step = builder.step;
		enumValues = Collections.unmodifiableList(new ArrayList<String>(builder.enumValues));
		applyMode = builder.applyMode;
		scope = builder.scope;
		schemaVersion = builder.schemaVersion;
		readOnly = builder.readOnly;
	}

	public static Builder bool(String key, String label, boolean defaultValue) {
		return new Builder(key, OptionType.BOOLEAN, label, Boolean.valueOf(defaultValue));
	}

	public static Builder integer(String key, String label, int defaultValue, int minimum, int maximum) {
		return new Builder(key, OptionType.INT, label, Integer.valueOf(defaultValue)).range(minimum, maximum).step(1);
	}

	public static Builder floating(String key, String label, double defaultValue, double minimum, double maximum, double step) {
		return new Builder(key, OptionType.FLOAT, label, Double.valueOf(defaultValue)).range(minimum, maximum).step(step);
	}

	public static Builder text(String key, String label, String defaultValue) {
		return new Builder(key, OptionType.STRING, label, defaultValue == null ? "" : defaultValue);
	}

	public static Builder choice(String key, String label, String defaultValue, String... values) {
		Builder builder = new Builder(key, OptionType.ENUM, label, defaultValue);
		if (values != null) for (String value : values) builder.enumValue(value);
		return builder;
	}

	public static OptionSetting section(String key, String label) {
		return new Builder(key, OptionType.SECTION, label, "").readOnly(true).build();
	}

	public static OptionSetting info(String key, String label, String text) {
		return new Builder(key, OptionType.INFO, label, text == null ? "" : text).readOnly(true).build();
	}

	public static OptionSetting readOnly(String key, String label, String value) {
		return new Builder(key, OptionType.READ_ONLY, label, value == null ? "" : value).readOnly(true).build();
	}

	public String key() { return key; }
	public OptionType type() { return type; }
	public String label() { return label; }
	public String description() { return description; }
	public Object defaultValue() { return defaultValue; }
	public Double minimum() { return minimum; }
	public Double maximum() { return maximum; }
	public Double step() { return step; }
	public List<String> enumValues() { return enumValues; }
	public OptionApplyMode applyMode() { return applyMode; }
	public OptionScope scope() { return scope; }
	public int schemaVersion() { return schemaVersion; }
	public boolean readOnly() { return readOnly; }

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof OptionSetting)) return false;
		OptionSetting other = (OptionSetting) object;
		return schemaVersion == other.schemaVersion && readOnly == other.readOnly
				&& Objects.equals(key, other.key) && type == other.type && Objects.equals(label, other.label)
				&& Objects.equals(description, other.description) && Objects.equals(defaultValue, other.defaultValue)
				&& Objects.equals(minimum, other.minimum) && Objects.equals(maximum, other.maximum)
				&& Objects.equals(step, other.step) && Objects.equals(enumValues, other.enumValues)
				&& applyMode == other.applyMode && scope == other.scope;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, type, label, description, defaultValue, minimum, maximum, step,
				enumValues, applyMode, scope, Integer.valueOf(schemaVersion), Boolean.valueOf(readOnly));
	}

	public static final class Builder {
		private final String key;
		private final OptionType type;
		private final String label;
		private final Object defaultValue;
		private String description = "";
		private Double minimum;
		private Double maximum;
		private Double step;
		private final List<String> enumValues = new ArrayList<String>();
		private OptionApplyMode applyMode = OptionApplyMode.IMMEDIATE;
		private OptionScope scope = OptionScope.GLOBAL;
		private int schemaVersion = 1;
		private boolean readOnly;

		private Builder(String key, OptionType type, String label, Object defaultValue) {
			this.key = key;
			this.type = type;
			this.label = label == null ? key : label;
			this.defaultValue = defaultValue;
			readOnly = type == OptionType.SECTION || type == OptionType.INFO || type == OptionType.READ_ONLY;
		}

		public Builder description(String value) { description = value == null ? "" : value; return this; }
		public Builder range(double min, double max) { minimum = Double.valueOf(min); maximum = Double.valueOf(max); return this; }
		public Builder step(double value) { step = Double.valueOf(value); return this; }
		public Builder enumValue(String value) { if (value != null && !value.isEmpty()) enumValues.add(value); return this; }
		public Builder applyMode(OptionApplyMode value) { applyMode = value == null ? OptionApplyMode.IMMEDIATE : value; return this; }
		public Builder scope(OptionScope value) { scope = value == null ? OptionScope.GLOBAL : value; return this; }
		public Builder schemaVersion(int value) { schemaVersion = value; return this; }
		public Builder readOnly(boolean value) { readOnly = value; return this; }
		public OptionSetting build() { return new OptionSetting(this); }
	}
}
