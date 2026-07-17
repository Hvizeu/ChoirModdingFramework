package choir.adapter.v71_44;

import java.io.IOException;

import choir.api.room.RoomDeclaration;
import choir.api.room.RoomFamily;
import choir.internal.ChoirDiagnostics;
import game.audio.AUDIO;
import game.audio.Ambiance;
import game.audio.SoundRace;
import settlement.path.AVAILABILITY;
import settlement.path.finders.SFinderFindable;
import settlement.room.main.Room;
import settlement.room.main.RoomBlueprintIns;
import settlement.room.main.RoomInstance;
import settlement.room.main.TmpArea;
import settlement.room.main.category.RoomCategorySub;
import settlement.room.main.furnisher.Furnisher;
import settlement.room.main.furnisher.FurnisherItem;
import settlement.room.main.furnisher.FurnisherItemTile;
import settlement.room.main.util.RoomInit;
import settlement.room.main.util.RoomInitData;
import settlement.room.sprite.RoomSprite;
import settlement.room.sprite.RoomSprite1x1;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;

/** V71.44 implementation of the stable passive-room family. */
final class StablePassiveRoomBlueprint extends RoomBlueprintIns<StablePassiveRoomBlueprint.Instance> {
	private final Constructor constructor;

	static int materialize(RoomInitData init, RoomDeclaration declaration) throws IOException {
		if (declaration.family() != RoomFamily.PASSIVE_DECORATION)
			throw new IllegalArgumentException("Unsupported stable room family: " + declaration.family());
		validateSoundContract(declaration);
		init.setType("CHOIR_PASSIVE_DECORATION");
		return new StablePassiveRoomBlueprint(init, declaration, init.m.CATS.DECOR).index();
	}

	private static void validateSoundContract(RoomDeclaration declaration) {
		String ambianceKey = "ROOM_" + declaration.roomKey();
		String clickKey = "ROOM_CLICK_" + declaration.roomKey();
		Ambiance ambiance = AUDIO.AMBI().get(ambianceKey);
		if (ambiance == null) throw new IllegalArgumentException("Stable room sound contract missing ambiance key: " + ambianceKey);
		SoundRace click = AUDIO.race(clickKey);
		if (click == null || !clickKey.equals(click.key()))
			throw new IllegalArgumentException("Stable room sound contract missing click key: " + clickKey);
		ChoirDiagnostics.info("ROOM-REGISTRATION sound-contract provider=" + declaration.providerId() + " key="
				+ declaration.roomKey() + " ambiance=" + ambianceKey + " click=" + clickKey + " validated=true"
				+ " missingProviderPolicy=" + declaration.missingProviderPolicy());
	}

	private StablePassiveRoomBlueprint(RoomInitData init, RoomDeclaration declaration, RoomCategorySub category) throws IOException {
		super(0, init, declaration.roomKey(), category);
		constructor = new Constructor(this, init, declaration.spriteKey());
	}

	@Override protected void saveP(FilePutter file) { }
	@Override protected void loadP(FileGetter file) throws IOException { }
	@Override protected void clearP() { }
	@Override protected void update(double ds) { }
	@Override public SFinderFindable service(int tx, int ty) { return null; }
	@Override public Furnisher constructor() { return constructor; }
	@Override public boolean degrades() { return false; }

	static final class Instance extends RoomInstance {
		private static final long serialVersionUID = 1L;

		Instance(StablePassiveRoomBlueprint blueprint, TmpArea area, RoomInit init) {
			super(blueprint, area, init);
			activate();
		}

		@Override public StablePassiveRoomBlueprint blueprintI() { return (StablePassiveRoomBlueprint) blueprint(); }
		@Override protected void activateAction() { }
		@Override protected void deactivateAction() { }
		@Override protected void dispose() { }
	}

	private static final class Constructor extends Furnisher {
		private final StablePassiveRoomBlueprint blueprint;

		Constructor(StablePassiveRoomBlueprint blueprint, RoomInitData init, String spriteKey) throws IOException {
			super(init, 0, 0);
			this.blueprint = blueprint;
			RoomSprite sprite = new RoomSprite1x1(init.data().json("SPRITES"), spriteKey);
			FurnisherItemTile tile = new FurnisherItemTile(this, true, sprite, AVAILABILITY.ROOM, false);
			new FurnisherItem(new FurnisherItemTile[][] { { tile } }, 1);
			flushSingle(blueprint.info);
		}

		@Override public boolean usesArea() { return false; }
		@Override public boolean mustBeIndoors() { return false; }
		@Override public Room create(TmpArea area, RoomInit init) { return new Instance(blueprint, area, init); }
		@Override public StablePassiveRoomBlueprint blue() { return blueprint; }
	}
}
