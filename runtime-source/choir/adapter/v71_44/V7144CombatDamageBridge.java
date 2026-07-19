package choir.adapter.v71_44;

import java.util.HashMap;
import java.util.Map;

import choir.api.combat.CombatDamageCategory;
import choir.internal.ChoirDiagnostics;
import choir.internal.combat.CombatDamageRegistry;
import game.battle.Army;
import settlement.entity.ECollision;
import settlement.entity.ENTITY;
import settlement.entity.humanoid.Humanoid;
import settlement.main.SETT;
import settlement.room.main.RoomInstance;
import settlement.room.military.artillery.ArtilleryInstance;
import settlement.stats.STATS;
import settlement.stats.equip.EquipBattle;

/** Version-sensitive bridge. No vanilla type crosses into Choir's public API. */
public final class V7144CombatDamageBridge {
	private static final Map<Integer, Provenance> projectiles = new HashMap<Integer, Provenance>();
	private static final ThreadLocal<Provenance> currentProjectile = new ThreadLocal<Provenance>();
	private static volatile boolean ready;

	private V7144CombatDamageBridge() { }

	public static synchronized void initialize() {
		V7144CombatTargetFingerprint.Result result = V7144CombatTargetFingerprint.verify();
		ready = result.matches;
		CombatDamageRegistry.adapterReady(ready, result.detail());
		ChoirDiagnostics.info("COMBAT-DAMAGE compatibility-fingerprints matched=" + result.matches
				+ " gameJar=" + result.jar + " targets=" + result.actual);
	}

	public static void projectileLaunched(int index, int x, int y, ENTITY shooter) {
		if (!ready || index < 0) return;
		Provenance provenance = provenanceAtLaunch(x, y, shooter);
		if (provenance != null) projectiles.put(Integer.valueOf(index), provenance);
		else projectiles.remove(Integer.valueOf(index));
	}

	public static void beginProjectileUpdate(int index, ENTITY liveShooter) {
		if (!ready) return;
		Provenance provenance = projectiles.get(Integer.valueOf(index));
		if (provenance == null && liveShooter instanceof Humanoid)
			provenance = humanoidProjectile((Humanoid) liveShooter);
		currentProjectile.set(provenance);
	}

	/** Mirrors PData.remove's last-entry compaction without retaining any game object. */
	public static void endProjectileUpdate(int index, int beforeLast, int afterLast) {
		try {
			if (!ready || afterLast >= beforeLast) return;
			int movedFrom = beforeLast - 1;
			Provenance moved = projectiles.remove(Integer.valueOf(movedFrom));
			projectiles.remove(Integer.valueOf(index));
			if (index < afterLast && moved != null) projectiles.put(Integer.valueOf(index), moved);
		} finally { currentProjectile.remove(); }
	}

	public static double applyResolvedDamage(ECollision collision, Humanoid victim, double vanillaDamage) {
		if (!ready || collision == null || victim == null) return vanillaDamage;
		try {
			Provenance provenance;
			if (collision.other instanceof Humanoid) {
				Humanoid attacker = (Humanoid) collision.other;
				provenance = new Provenance(isMounted(attacker) ? CombatDamageCategory.MOUNTED : CombatDamageCategory.MELEE,
						Boolean.valueOf(attacker.indu().army().player()));
			} else provenance = currentProjectile.get();
			if (provenance == null) return vanillaDamage;
			return CombatDamageRegistry.apply(provenance.category, provenance.attackerPlayer,
					victim.indu().army().player(), vanillaDamage);
		} catch (Throwable failure) {
			ChoirDiagnostics.error("COMBAT-DAMAGE adapter-failure type=" + failure.getClass().getSimpleName()
					+ " message=" + failure.getMessage() + " action=preserve-vanilla");
			return vanillaDamage;
		}
	}

	public static void disposed() {
		projectiles.clear(); currentProjectile.remove(); CombatDamageRegistry.disposeRuntime();
	}
	public static void projectilesCleared() {
		projectiles.clear(); currentProjectile.remove();
	}

	private static Provenance provenanceAtLaunch(int x, int y, ENTITY shooter) {
		if (shooter instanceof Humanoid) return humanoidProjectile((Humanoid) shooter);
		try {
			RoomInstance room = SETT.ROOMS().map.instance.get(x >> 6, y >> 6);
			if (room instanceof ArtilleryInstance) {
				Army army = ((ArtilleryInstance) room).army();
				return new Provenance(CombatDamageCategory.ARTILLERY,
						army == null ? null : Boolean.valueOf(army.player()));
			}
		} catch (Throwable ignored) { }
		return null;
	}

	private static Provenance humanoidProjectile(Humanoid shooter) {
		return new Provenance(isMounted(shooter) ? CombatDamageCategory.MOUNTED : CombatDamageCategory.RANGED,
				Boolean.valueOf(shooter.indu().army().player()));
	}

	private static boolean isMounted(Humanoid humanoid) {
		for (int i = 0; i < STATS.EQUIP().mounts.size(); i++) {
			EquipBattle mount = STATS.EQUIP().mounts.get(i);
			if (mount.get(humanoid.indu()) > 0) return true;
		}
		return false;
	}

	private static final class Provenance {
		final CombatDamageCategory category; final Boolean attackerPlayer;
		Provenance(CombatDamageCategory category, Boolean attackerPlayer) {
			this.category = category; this.attackerPlayer = attackerPlayer;
		}
	}
}
