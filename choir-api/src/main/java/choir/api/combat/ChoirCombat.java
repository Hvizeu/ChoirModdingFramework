package choir.api.combat;

import choir.api.spi.ChoirRuntimeServices;

/** Public, vanilla-type-free facade for deterministic combat-damage composition. */
public final class ChoirCombat {
	public static final int API_VERSION = 1;
	private ChoirCombat() { }
	public static CombatDamageRegistrationResult registerDamageModifier(CombatDamageModifier modifier) {
		return ChoirRuntimeServices.require().registerCombatDamageModifier(modifier);
	}
	public static CombatDamageRuntimeSnapshot runtimeSnapshot() {
		return ChoirRuntimeServices.require().combatDamageRuntimeSnapshot();
	}
}
