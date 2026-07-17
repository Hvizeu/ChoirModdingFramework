package choir.api.room;

/** Immutable result of one process-scoped room declaration attempt. */
public final class RoomRegistrationResult {
	private final RoomRegistrationStatus status;
	private final String providerId;
	private final String roomKey;
	private final String detail;

	public RoomRegistrationResult(RoomRegistrationStatus status, String providerId, String roomKey, String detail) {
		this.status = status;
		this.providerId = providerId;
		this.roomKey = roomKey;
		this.detail = detail;
	}

	public RoomRegistrationStatus status() { return status; }
	public String providerId() { return providerId; }
	public String roomKey() { return roomKey; }
	public String detail() { return detail; }
	public boolean accepted() { return status == RoomRegistrationStatus.ACCEPTED || status == RoomRegistrationStatus.IDEMPOTENT; }
}
