package choir.internal;
import java.io.IOException;
import choir.api.spi.ChoirRuntimeServices;
import script.SCRIPT;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
public final class ChoirScript implements SCRIPT {
	public ChoirScript() { ChoirRuntimeServices.install(ApiRuntimeService.INSTANCE); }
	public CharSequence name() { return "Choir Modding Framework"; }
	public CharSequence desc() { return "Content-free v71.44 compatibility bootstrap."; }
	public void initBeforeGameCreated() { ChoirBootstrap.scriptCallback("initBeforeGameCreated"); ChoirBootstrap.initializeOnce(); ChoirBootstrap.beforeGameCreated(); }
	public void initBeforeGameInited() { ChoirBootstrap.scriptCallback("initBeforeGameInited"); ChoirBootstrap.beforeGameInited(); }
	public boolean isSelectable() { return false; }
	public boolean forceInit() { return true; }
	public SCRIPT_INSTANCE createInstance() { ChoirBootstrap.scriptCallback("createInstance"); ChoirBootstrap.instanceCreated(); return new Instance(); }
	private static final class Instance implements SCRIPT_INSTANCE {
		public void update(double ds) { }
		public void save(FilePutter file) { file.mark("ChoirPhase1"); }
		public void load(FileGetter file) throws IOException { file.check("ChoirPhase1"); }
		public boolean handleBrokenSavedState() { return true; }
	}
}
