package choir.api.room;

/** Immutable diagnostics for one retained declaration. */
public final class RoomRegistrationView {
	private final RoomDeclaration declaration;
	private final long currentRegistryCycleId;
	private final int currentRuntimeIndex;
	private final int totalMaterializations;

	public RoomRegistrationView(RoomDeclaration declaration, long currentRegistryCycleId,
			int currentRuntimeIndex, int totalMaterializations) {
		this.declaration = declaration;
		this.currentRegistryCycleId = currentRegistryCycleId;
		this.currentRuntimeIndex = currentRuntimeIndex;
		this.totalMaterializations = totalMaterializations;
	}

	public RoomDeclaration declaration() { return declaration; }
	public long currentRegistryCycleId() { return currentRegistryCycleId; }
	public int currentRuntimeIndex() { return currentRuntimeIndex; }
	public int totalMaterializations() { return totalMaterializations; }
	public boolean materializedInCurrentRegistry() { return currentRegistryCycleId > 0 && currentRuntimeIndex >= 0; }
}
