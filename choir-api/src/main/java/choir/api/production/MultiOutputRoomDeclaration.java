package choir.api.production;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Immutable opt-in for one normal-data-backed production recipe.
 *
 * <p>The provider owns the room and INDUSTRIES data. Choir verifies that the
 * selected recipe has exactly one input and the declared two-to-five outputs,
 * then physically emits secondary outputs at the verified V71.44 job hook. An
 * active declaration also opts only this exact room/industry target into advanced
 * internal storage, independently of the player-wide storage option.</p>
 *
 * <p>This is a stable-ID descriptor, not a base class for a custom building. It
 * exposes no vanilla room, industry, or resource types.</p>
 */
public final class MultiOutputRoomDeclaration {
	public static final int MAX_OUTPUTS = 5;
	private final String providerId;
	private final String declarationId;
	private final String roomKey;
	private final ProductionRoomFamily family;
	private final int industryIndex;
	private final String inputResourceId;
	private final List<String> outputResourceIds;

	public MultiOutputRoomDeclaration(String providerId, String declarationId, String roomKey,
			ProductionRoomFamily family, int industryIndex, String inputResourceId,
			List<String> outputResourceIds) {
		this.providerId = lowerId(providerId, "provider ID");
		this.declarationId = lowerId(declarationId, "declaration ID");
		this.roomKey = upperId(roomKey, "room key");
		this.family = Objects.requireNonNull(family, "family");
		if (industryIndex < 0 || industryIndex > 127)
			throw new IllegalArgumentException("Industry index must be between 0 and 127: " + industryIndex);
		this.industryIndex = industryIndex;
		this.inputResourceId = upperId(inputResourceId, "input resource ID");
		if (outputResourceIds == null || outputResourceIds.size() < 2 || outputResourceIds.size() > MAX_OUTPUTS)
			throw new IllegalArgumentException("A multi-output recipe must declare between 2 and " + MAX_OUTPUTS + " outputs.");
		ArrayList<String> outputs = new ArrayList<String>(outputResourceIds.size());
		HashSet<String> unique = new HashSet<String>();
		for (String output : outputResourceIds) {
			String stable = upperId(output, "output resource ID");
			if (!unique.add(stable)) throw new IllegalArgumentException("Duplicate output resource ID: " + stable);
			outputs.add(stable);
		}
		this.outputResourceIds = Collections.unmodifiableList(outputs);
	}

	public static MultiOutputRoomDeclaration dataBacked(String providerId, String declarationId,
			String roomKey, ProductionRoomFamily family, int industryIndex,
			String inputResourceId, String... outputResourceIds) {
		if (outputResourceIds == null) throw new IllegalArgumentException("Output resource IDs are required.");
		return new MultiOutputRoomDeclaration(providerId, declarationId, roomKey, family,
				industryIndex, inputResourceId, List.of(outputResourceIds));
	}

	public String providerId() { return providerId; }
	public String declarationId() { return declarationId; }
	public String roomKey() { return roomKey; }
	public ProductionRoomFamily family() { return family; }
	public int industryIndex() { return industryIndex; }
	public String inputResourceId() { return inputResourceId; }
	public List<String> outputResourceIds() { return outputResourceIds; }
	public String qualifiedId() { return providerId + ":" + declarationId; }
	public String targetId() { return roomKey + "#" + industryIndex; }

	@Override public boolean equals(Object value) {
		if (this == value) return true;
		if (!(value instanceof MultiOutputRoomDeclaration)) return false;
		MultiOutputRoomDeclaration other = (MultiOutputRoomDeclaration) value;
		return industryIndex == other.industryIndex && providerId.equals(other.providerId)
				&& declarationId.equals(other.declarationId) && roomKey.equals(other.roomKey)
				&& family == other.family && inputResourceId.equals(other.inputResourceId)
				&& outputResourceIds.equals(other.outputResourceIds);
	}
	@Override public int hashCode() { return Objects.hash(providerId, declarationId, roomKey, family,
			Integer.valueOf(industryIndex), inputResourceId, outputResourceIds); }
	@Override public String toString() { return qualifiedId() + "[target=" + targetId() + ",family=" + family
			+ ",input=" + inputResourceId + ",outputs=" + outputResourceIds + "]"; }

	private static String lowerId(String value, String label) {
		if (value == null || !value.matches("[a-z][a-z0-9._-]*"))
			throw new IllegalArgumentException(label + " must be a stable lowercase ID: " + value);
		return value;
	}
	private static String upperId(String value, String label) {
		if (value == null || !value.matches("[A-Z][A-Z0-9_]*"))
			throw new IllegalArgumentException(label + " must match [A-Z][A-Z0-9_]*: " + value);
		return value;
	}
}
