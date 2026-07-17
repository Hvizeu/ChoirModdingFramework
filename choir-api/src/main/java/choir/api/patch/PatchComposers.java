package choir.api.patch;

public final class PatchComposers {
	private PatchComposers() { }
	public static final PatchComposer<Double> DOUBLE_ADD = (current, value) -> current + value;
	public static final PatchComposer<Double> DOUBLE_MULTIPLY = (current, value) -> current * value;
	public static final PatchComposer<Integer> INTEGER_ADD = (current, value) -> current + value;
	public static final PatchComposer<Boolean> BOOLEAN_AND = (current, value) -> current && value;
	public static final PatchComposer<Boolean> BOOLEAN_OR = (current, value) -> current || value;
	private static final PatchComposer<Object> REPLACE = (current, value) -> value;
	@SuppressWarnings("unchecked") public static <T> PatchComposer<T> replace() { return (PatchComposer<T>) REPLACE; }
	static String stableId(PatchComposer<?> composer) {
		if (composer == DOUBLE_ADD) return "choir.double-add.v1";
		if (composer == DOUBLE_MULTIPLY) return "choir.double-multiply.v1";
		if (composer == INTEGER_ADD) return "choir.integer-add.v1";
		if (composer == BOOLEAN_AND) return "choir.boolean-and.v1";
		if (composer == BOOLEAN_OR) return "choir.boolean-or.v1";
		if (composer == REPLACE) return "choir.replace.v1";
		return null;
	}
}
