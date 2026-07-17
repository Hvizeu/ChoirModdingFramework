package choir.api.room;

public enum RoomRegistrationStatus {
	ACCEPTED,
	IDEMPOTENT,
	CONFLICT,
	REJECTED_LATE
}
