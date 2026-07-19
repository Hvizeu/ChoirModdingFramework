package choir.api.combat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Immutable, provider-owned multiplier for resolved tactical damage.
 *
 * <p>Matching contributions compose multiplicatively in deterministic order.
 * Choir applies the resulting factor once, after vanilla has resolved the hit
 * and mitigation and immediately before the target receives damage.</p>
 */
public final class CombatDamageModifier {
	private final String providerId;
	private final String modifierId;
	private final Set<CombatDamageCategory> categories;
	private final CombatParticipantSide attackerSide;
	private final CombatParticipantSide defenderSide;
	private final CombatExecutionMode executionMode;
	private final int priority;
	private final double multiplier;

	public CombatDamageModifier(String providerId, String modifierId,
			Set<CombatDamageCategory> categories, CombatParticipantSide attackerSide,
			CombatParticipantSide defenderSide, CombatExecutionMode executionMode,
			int priority, double multiplier) {
		this.providerId = stableId(providerId, "provider");
		this.modifierId = stableId(modifierId, "modifier");
		if (categories == null || categories.isEmpty())
			throw new IllegalArgumentException("At least one combat damage category is required.");
		if (categories.contains(null)) throw new IllegalArgumentException("Combat damage categories cannot contain null.");
		this.categories = Collections.unmodifiableSet(EnumSet.copyOf(categories));
		if (attackerSide == null || defenderSide == null || executionMode == null)
			throw new IllegalArgumentException("Attacker side, defender side, and execution mode are required.");
		if (!Double.isFinite(multiplier) || multiplier < 0.0)
			throw new IllegalArgumentException("Combat damage multiplier must be finite and non-negative.");
		this.attackerSide = attackerSide;
		this.defenderSide = defenderSide;
		this.executionMode = executionMode;
		this.priority = priority;
		this.multiplier = multiplier;
	}

	public static CombatDamageModifier playerOutgoing(String providerId, String modifierId,
			Set<CombatDamageCategory> categories, int priority, double multiplier) {
		return new CombatDamageModifier(providerId, modifierId, categories, CombatParticipantSide.PLAYER,
				CombatParticipantSide.ANY, CombatExecutionMode.TACTICAL_SETTLEMENT, priority, multiplier);
	}

	public static CombatDamageModifier playerIncoming(String providerId, String modifierId,
			Set<CombatDamageCategory> categories, int priority, double multiplier) {
		return new CombatDamageModifier(providerId, modifierId, categories, CombatParticipantSide.ANY,
				CombatParticipantSide.PLAYER, CombatExecutionMode.TACTICAL_SETTLEMENT, priority, multiplier);
	}

	public String providerId() { return providerId; }
	public String modifierId() { return modifierId; }
	public Set<CombatDamageCategory> categories() { return categories; }
	public CombatParticipantSide attackerSide() { return attackerSide; }
	public CombatParticipantSide defenderSide() { return defenderSide; }
	public CombatExecutionMode executionMode() { return executionMode; }
	public int priority() { return priority; }
	public double multiplier() { return multiplier; }

	private static String stableId(String value, String label) {
		if (value == null) throw new IllegalArgumentException("Combat " + label + " ID is required.");
		String id = value.trim();
		if (!id.matches("[a-z0-9][a-z0-9._-]*(?::[A-Za-z0-9][A-Za-z0-9._-]*)*"))
			throw new IllegalArgumentException("Invalid stable combat " + label + " ID: " + value);
		return id;
	}
}
