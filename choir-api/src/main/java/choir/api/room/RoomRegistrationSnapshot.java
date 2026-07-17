package choir.api.room;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Process declarations plus current-registry diagnostic state. */
public final class RoomRegistrationSnapshot {
	private final long registrationGeneration;
	private final long currentRegistryCycleId;
	private final int materializedThisCycle;
	private final int totalMaterializations;
	private final List<RoomRegistrationView> rooms;

	public RoomRegistrationSnapshot(long registrationGeneration, long currentRegistryCycleId,
			int materializedThisCycle, int totalMaterializations, List<RoomRegistrationView> rooms) {
		this.registrationGeneration = registrationGeneration;
		this.currentRegistryCycleId = currentRegistryCycleId;
		this.materializedThisCycle = materializedThisCycle;
		this.totalMaterializations = totalMaterializations;
		this.rooms = Collections.unmodifiableList(new ArrayList<RoomRegistrationView>(rooms));
	}

	public long registrationGeneration() { return registrationGeneration; }
	public long currentRegistryCycleId() { return currentRegistryCycleId; }
	public int registeredCount() { return rooms.size(); }
	public int materializedThisCycle() { return materializedThisCycle; }
	public int totalMaterializations() { return totalMaterializations; }
	public List<RoomRegistrationView> rooms() { return rooms; }
}
