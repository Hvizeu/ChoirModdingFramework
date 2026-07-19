package choir.api.combat;

public enum CombatDamageRegistrationResult {
	ACCEPTED,
	IDEMPOTENT,
	REJECTED_CONFLICT
}
