package choir.adapter.v71_44;

import choir.internal.ChoirBootstrap;
import choir.internal.ChoirDiagnostics;
import game.GAME;
import game.VERSION;
import init.paths.ModInfo;
import init.paths.PATHS;
import snake2d.util.misc.ACTION;
import snake2d.util.sets.LIST;
import view.main.VIEW;

/** Version-pinned evidence hooks. No gameplay data or state is changed. */
final class V7144RuntimeEvidence {
	private static final String CHOIR_MOD_FOLDER = "ChoirModdingFramework";
	private V7144RuntimeEvidence() { }

	static void logEnabledModInventory() {
		LIST<ModInfo> mods = PATHS.currentMods();
		int choirEntries = 0;
		ChoirDiagnostics.info("MOD-INVENTORY count=" + mods.size() + " source=PATHS.currentMods");
		for (int i = 0; i < mods.size(); i++) {
			ModInfo mod = mods.get(i);
			boolean choir = CHOIR_MOD_FOLDER.equals(mod.path);
			if (choir) choirEntries++;
			ChoirDiagnostics.info("MOD-INVENTORY entry index=" + i + " id=" + quoted(mod.path) + " name=" + quoted(mod.name) + " version=" + quoted(mod.version) + " majorVersion=" + mod.majorVersion + " folder=" + quoted(mod.absolutePath));
		}
		ChoirDiagnostics.info("MOD-INVENTORY complete count=" + mods.size() + " choirEnabled=" + (choirEntries == 1) + " choirEntryCount=" + choirEntries + " otherEnabledCount=" + (mods.size() - choirEntries) + " order=launcher-settings");
	}

	static void installGameplayLifecycleProbe() {
		GAME.addBeforeGameStarts(new ACTION() {
			@Override
			public void exe() {
				if (VIEW.s().isActive()) {
					ChoirBootstrap.gameplayReached("GAME.setGameStart.after-SettViewStart.activate", VERSION.VERSION_STRING);
				} else {
					ChoirDiagnostics.info("GAMEPLAY marker=GAME.setGameStart settlementViewActive=false");
				}
			}
		});
		ChoirDiagnostics.info("GAMEPLAY probe-installed hook=GAME.addBeforeGameStarts");
	}

	private static String quoted(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n") + "\"";
	}
}
