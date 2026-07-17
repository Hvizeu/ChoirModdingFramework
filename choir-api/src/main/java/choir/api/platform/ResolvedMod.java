package choir.api.platform;

import java.util.List;

/** One deterministic dependency-graph result. */
public final class ResolvedMod {
	private final ModManifest manifest;
	private final PlatformModState state;
	private final int loadOrder;
	private final List<String> diagnostics;

	public ResolvedMod(ModManifest manifest, PlatformModState state, int loadOrder, List<String> diagnostics) {
		this.manifest = manifest;
		this.state = state;
		this.loadOrder = loadOrder;
		this.diagnostics = List.copyOf(diagnostics);
	}
	public ModManifest manifest() { return manifest; }
	public PlatformModState state() { return state; }
	public int loadOrder() { return loadOrder; }
	public List<String> diagnostics() { return diagnostics; }
}
