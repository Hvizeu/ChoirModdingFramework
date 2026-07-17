package choir.api.patch;

public final class PatchTarget<T> {
	private final String ownerId;
	private final String targetId;
	private final Class<T> valueType;
	private final T baseValue;
	private final String composerId;
	private final PatchComposer<T> composer;
	public PatchTarget(String ownerId, String targetId, Class<T> valueType, T baseValue, PatchComposer<T> composer) {
		this(ownerId, targetId, valueType, baseValue, builtInId(composer), composer);
	}
	public PatchTarget(String ownerId, String targetId, Class<T> valueType, T baseValue, String composerId, PatchComposer<T> composer) {
		if (ownerId == null || !ownerId.matches("[a-z][a-z0-9._-]*")) throw new IllegalArgumentException("Invalid target owner: " + ownerId);
		if (targetId == null || !targetId.matches("[A-Za-z0-9][A-Za-z0-9._:/-]*")) throw new IllegalArgumentException("Invalid target ID: " + targetId);
		if (valueType == null || baseValue == null || composer == null || !valueType.isInstance(baseValue)) throw new IllegalArgumentException("Invalid typed patch target.");
		if (composerId == null || !composerId.matches("[a-z][a-z0-9._-]*")) throw new IllegalArgumentException("A stable composer ID is required.");
		this.ownerId = ownerId; this.targetId = targetId; this.valueType = valueType; this.baseValue = baseValue; this.composerId = composerId; this.composer = composer;
	}
	public String ownerId() { return ownerId; }
	public String targetId() { return targetId; }
	public Class<T> valueType() { return valueType; }
	public T baseValue() { return baseValue; }
	public String composerId() { return composerId; }
	public PatchComposer<T> composer() { return composer; }
	private static String builtInId(PatchComposer<?> composer) {
		String id = PatchComposers.stableId(composer);
		if (id == null) throw new IllegalArgumentException("Custom patch composers require the constructor with an explicit stable composer ID.");
		return id;
	}
}
