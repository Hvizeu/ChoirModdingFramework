package choir.api;
import java.util.Collections;
import java.util.List;
public final class RoomRegistrationContext {
	private final List<String> blueprintKeys;
	public RoomRegistrationContext(List<String> blueprintKeys) { this.blueprintKeys = Collections.unmodifiableList(blueprintKeys); }
	public int vanillaBlueprintCount() { return blueprintKeys.size(); }
	public List<String> blueprintKeys() { return blueprintKeys; }
}
