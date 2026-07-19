package choir.internal.storage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;

import choir.api.storage.ChoirStorage;

/**
 * Version-independent serialized state for one physical storage tile.
 *
 * <p>Pickup and incoming reservations are resource-specific. Production rooms
 * use the shared-capacity methods; stockpiles additionally assign fixed sections
 * and enforce the resulting per-resource quotas. Only one resource may own
 * incoming reservations at a time; this preserves exact re-resolution for legacy
 * V71.44 work plans that retain a coordinate but not a destination resource ID.</p>
 */
public final class MultiResourceCell implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final int FORMAT_VERSION = 2;

	private int formatVersion = FORMAT_VERSION;
	private int capacity;
	private int maxResourceKinds;
	private String[] resourceIds;
	private int[] amounts;
	private int[] pickupReserved;
	private int[] incomingReserved;
	/** Retained for format-1 save migration and non-section singular rooms. */
	private boolean[] pinnedAssignments;
	/** Physical stockpile sections assigned to each resource entry. */
	private int[] assignedSections;
	private int entryCount;
	private int totalAmount;
	private int totalIncomingReserved;

	public MultiResourceCell(int capacity, int maxResourceKinds) {
		validateLimits(capacity, maxResourceKinds);
		this.capacity = capacity;
		this.maxResourceKinds = maxResourceKinds;
		resourceIds = new String[maxResourceKinds];
		amounts = new int[maxResourceKinds];
		pickupReserved = new int[maxResourceKinds];
		incomingReserved = new int[maxResourceKinds];
		pinnedAssignments = new boolean[maxResourceKinds];
		assignedSections = new int[maxResourceKinds];
	}

	public int formatVersion() { return formatVersion; }
	public int capacity() { return capacity; }
	public int maxResourceKinds() { return maxResourceKinds; }
	public int entryCount() { return entryCount; }
	public int totalAmount() { return totalAmount; }
	public int totalIncomingReserved() { return totalIncomingReserved; }
	public int totalPickupReserved() {
		int total = 0;
		for (int i = 0; i < entryCount; i++) total += pickupReserved[i];
		return total;
	}
	public int sharedReservable() { return capacity - totalAmount - totalIncomingReserved; }
	public int totalAssignedSections() {
		int total = 0;
		for (int i = 0; i < entryCount; i++) total += assignedSections[i];
		return total;
	}

	/** Updates room-derived limits without silently discarding saved contents. */
	public void configure(int requestedCapacity, int requestedMaxResourceKinds) {
		validateLimits(requestedCapacity, requestedMaxResourceKinds);
		if (requestedMaxResourceKinds < entryCount)
			throw new IllegalStateException("Cannot reduce resource-kind limit below active entries: active="
					+ entryCount + " requested=" + requestedMaxResourceKinds);
		if (requestedCapacity < totalAmount + totalIncomingReserved)
			throw new IllegalStateException("Cannot reduce shared capacity below stored and reserved units: used="
					+ (totalAmount + totalIncomingReserved) + " requested=" + requestedCapacity);
		if (totalAssignedSections() > requestedMaxResourceKinds)
			throw new IllegalStateException("Cannot reduce section count below active assignments: active="
					+ totalAssignedSections() + " requested=" + requestedMaxResourceKinds);
		if (resourceIds.length != requestedMaxResourceKinds) resize(requestedMaxResourceKinds);
		capacity = requestedCapacity;
		maxResourceKinds = requestedMaxResourceKinds;
	}

	public int indexOf(String resourceId) {
		String id = requireResourceId(resourceId);
		for (int i = 0; i < entryCount; i++) if (id.equals(resourceIds[i])) return i;
		return -1;
	}

	public String resourceIdAt(int index) { checkEntry(index); return resourceIds[index]; }
	public int amountAt(int index) { checkEntry(index); return amounts[index]; }
	public int pickupReservedAt(int index) { checkEntry(index); return pickupReserved[index]; }
	public int incomingReservedAt(int index) { checkEntry(index); return incomingReserved[index]; }
	public boolean pinnedAt(int index) { checkEntry(index); return pinnedAssignments[index]; }
	public int assignedSectionsAt(int index) { checkEntry(index); return assignedSections[index]; }
	public int assignedSections(String resourceId) {
		int index = indexOf(resourceId);
		return index < 0 ? 0 : assignedSections[index];
	}

	/**
	 * Capacity owned by one resource's assigned sections. Proportional integer
	 * division keeps every resource independent of entry order and guarantees
	 * that the sum of all quotas never exceeds the real physical capacity.
	 */
	public int assignedCapacityAt(int index) {
		checkEntry(index);
		int sections = assignedSections[index];
		if (sections <= 0) return 0;
		return (int) (((long) capacity * sections) / maxResourceKinds);
	}
	public int assignedCapacity(String resourceId) {
		int index = indexOf(resourceId);
		return index < 0 ? 0 : assignedCapacityAt(index);
	}
	public int assignedReservable(String resourceId) {
		int index = indexOf(resourceId);
		if (index < 0 || assignedSections[index] <= 0) return 0;
		return Math.max(0, Math.min(sharedReservable(),
				assignedCapacityAt(index) - amounts[index] - incomingReserved[index]));
	}
	public int pickupReservableAt(int index) { checkEntry(index); return amounts[index] - pickupReserved[index]; }
	public boolean contains(String resourceId) { return indexOf(resourceId) >= 0; }
	public int firstEntryIndex() { return entryCount == 0 ? -1 : 0; }

	public int firstIncomingReservationIndex() {
		for (int i = 0; i < entryCount; i++) if (incomingReserved[i] > 0) return i;
		return -1;
	}

	public int ensureEntry(String resourceId) { return ensureEntry(resourceId, false); }

	public int ensureEntry(String resourceId, boolean pinAssignment) {
		String id = requireResourceId(resourceId);
		int old = indexOf(id);
		if (old >= 0) {
			if (pinAssignment) {
				if (assignedSections[old] == 0) {
					if (totalAssignedSections() >= maxResourceKinds)
						throw new IllegalStateException("Storage tile has no free assignment sections.");
					assignedSections[old] = 1;
				}
				pinnedAssignments[old] = true;
			}
			return old;
		}
		if (entryCount >= maxResourceKinds)
			throw new IllegalStateException("Storage tile reached its resource-kind limit of " + maxResourceKinds);
		if (pinAssignment && totalAssignedSections() >= maxResourceKinds)
			throw new IllegalStateException("Storage tile has no free assignment sections.");
		resourceIds[entryCount] = id;
		pinnedAssignments[entryCount] = pinAssignment;
		if (pinAssignment) assignedSections[entryCount] = 1;
		return entryCount++;
	}

	public boolean addAssignedSection(String resourceId) {
		if (totalAssignedSections() >= maxResourceKinds) return false;
		int index = indexOf(resourceId);
		if (index < 0) {
			if (entryCount >= maxResourceKinds) return false;
			index = ensureEntry(resourceId);
		}
		assignedSections[index]++;
		pinnedAssignments[index] = true;
		return true;
	}

	public boolean removeAssignedSection(String resourceId) {
		int index = indexOf(resourceId);
		if (index < 0 || assignedSections[index] <= 0) return false;
		assignedSections[index]--;
		pinnedAssignments[index] = assignedSections[index] > 0;
		return true;
	}

	/**
	 * Converts pre-section mixed cells without discarding contents. Every used
	 * resource receives one section first; remaining sections go to the largest
	 * capacity deficit. Fragmented legacy contents may temporarily exceed an
	 * individual quota, but cannot receive more until the player reallocates.
	 */
	public void initializeSectionAssignmentsForStoredEntries() {
		for (int i = 0; i < entryCount && totalAssignedSections() < maxResourceKinds; i++) {
			if ((pinnedAssignments[i] || amounts[i] + incomingReserved[i] > 0) && assignedSections[i] == 0) {
				assignedSections[i] = 1;
				pinnedAssignments[i] = true;
			}
		}
		while (totalAssignedSections() < maxResourceKinds) {
			int best = -1;
			int deficit = 0;
			for (int i = 0; i < entryCount; i++) {
				int d = amounts[i] + incomingReserved[i] - assignedCapacityAt(i);
				if (d > deficit) { best = i; deficit = d; }
			}
			if (best < 0) break;
			assignedSections[best]++;
			pinnedAssignments[best] = true;
		}
	}

	/** Imports an old singular record exactly once during save migration. */
	public int importEntry(String resourceId, int amount, int pickupReservation, int incomingReservation) {
		if (amount < 0 || pickupReservation < 0 || pickupReservation > amount || incomingReservation < 0)
			throw new IllegalArgumentException("Invalid legacy storage record.");
		if (totalAmount + totalIncomingReserved + amount + incomingReservation > capacity)
			throw new IllegalStateException("Legacy storage record exceeds shared capacity.");
		if (incomingReservation > 0 && firstIncomingReservationIndex() >= 0)
			throw new IllegalStateException("Only one incoming resource may be reserved per tile.");
		int index = ensureEntry(resourceId, true);
		if (amounts[index] != 0 || pickupReserved[index] != 0 || incomingReserved[index] != 0)
			throw new IllegalStateException("Legacy resource was imported more than once: " + resourceId);
		amounts[index] = amount;
		pickupReserved[index] = pickupReservation;
		incomingReserved[index] = incomingReservation;
		totalAmount += amount;
		totalIncomingReserved += incomingReservation;
		return index;
	}

	public boolean canReserveIncoming(String resourceId, int amount, boolean allowNewEntry) {
		if (amount < 0) return false;
		int index = indexOf(resourceId);
		if (index < 0 && (!allowNewEntry || entryCount >= maxResourceKinds)) return false;
		int incoming = firstIncomingReservationIndex();
		if (incoming >= 0 && incoming != index) return false;
		return amount <= sharedReservable();
	}

	public int reserveIncoming(String resourceId, int amount, boolean allowNewEntry) {
		if (amount <= 0) return 0;
		if (!canReserveIncoming(resourceId, amount, allowNewEntry)) return 0;
		int index = ensureEntry(resourceId);
		incomingReserved[index] += amount;
		totalIncomingReserved += amount;
		return amount;
	}

	public int cancelIncoming(String resourceId, int amount) {
		if (amount <= 0) return 0;
		int index = indexOf(resourceId);
		if (index < 0) return 0;
		int cancelled = Math.min(amount, incomingReserved[index]);
		incomingReserved[index] -= cancelled;
		totalIncomingReserved -= cancelled;
		return cancelled;
	}

	public int depositReserved(String resourceId, int amount) {
		if (amount <= 0) return 0;
		int index = indexOf(resourceId);
		if (index < 0) return 0;
		int deposited = Math.min(amount, incomingReserved[index]);
		incomingReserved[index] -= deposited;
		totalIncomingReserved -= deposited;
		amounts[index] += deposited;
		totalAmount += deposited;
		return deposited;
	}

	/** Used by a room's own production cycle, which does not create an AI delivery reservation. */
	public int depositDirect(String resourceId, int amount, boolean allowNewEntry) {
		if (amount <= 0) return 0;
		int index = indexOf(resourceId);
		if (index < 0 && (!allowNewEntry || entryCount >= maxResourceKinds)) return 0;
		int deposited = Math.min(amount, sharedReservable());
		if (deposited <= 0) return 0;
		index = ensureEntry(resourceId);
		amounts[index] += deposited;
		totalAmount += deposited;
		return deposited;
	}

	public int reservePickup(String resourceId, int amount) {
		if (amount <= 0) return 0;
		int index = indexOf(resourceId);
		if (index < 0) return 0;
		int reserved = Math.min(amount, amounts[index] - pickupReserved[index]);
		pickupReserved[index] += reserved;
		return reserved;
	}

	public int cancelPickup(String resourceId, int amount) {
		if (amount <= 0) return 0;
		int index = indexOf(resourceId);
		if (index < 0) return 0;
		int cancelled = Math.min(amount, pickupReserved[index]);
		pickupReserved[index] -= cancelled;
		return cancelled;
	}

	public int pickupReserved(String resourceId, int amount) {
		if (amount <= 0) return 0;
		int index = indexOf(resourceId);
		if (index < 0) return 0;
		int picked = Math.min(amount, pickupReserved[index]);
		pickupReserved[index] -= picked;
		amounts[index] -= picked;
		totalAmount -= picked;
		return picked;
	}

	public int removeUnreserved(String resourceId, int amount) {
		if (amount <= 0) return 0;
		int index = indexOf(resourceId);
		if (index < 0) return 0;
		int removed = Math.min(amount, amounts[index] - pickupReserved[index]);
		amounts[index] -= removed;
		totalAmount -= removed;
		return removed;
	}

	/** Removes an empty assignment so its slot can be reused. */
	public boolean removeEmptyEntry(String resourceId) {
		int index = indexOf(resourceId);
		if (index < 0) return false;
		if (amounts[index] != 0 || pickupReserved[index] != 0 || incomingReserved[index] != 0) return false;
		int moved = entryCount - index - 1;
		if (moved > 0) {
			System.arraycopy(resourceIds, index + 1, resourceIds, index, moved);
			System.arraycopy(amounts, index + 1, amounts, index, moved);
			System.arraycopy(pickupReserved, index + 1, pickupReserved, index, moved);
			System.arraycopy(incomingReserved, index + 1, incomingReserved, index, moved);
			System.arraycopy(pinnedAssignments, index + 1, pinnedAssignments, index, moved);
			System.arraycopy(assignedSections, index + 1, assignedSections, index, moved);
		}
		entryCount--;
		resourceIds[entryCount] = null;
		amounts[entryCount] = pickupReserved[entryCount] = incomingReserved[entryCount] = 0;
		pinnedAssignments[entryCount] = false;
		assignedSections[entryCount] = 0;
		return true;
	}

	/** Removes an empty dynamic entry while preserving room-pinned assignments. */
	public boolean removeEmptyUnpinnedEntry(String resourceId) {
		int index = indexOf(resourceId);
		return index >= 0 && !pinnedAssignments[index] && removeEmptyEntry(resourceId);
	}

	public void clear() {
		Arrays.fill(resourceIds, null);
		Arrays.fill(amounts, 0);
		Arrays.fill(pickupReserved, 0);
		Arrays.fill(incomingReserved, 0);
		Arrays.fill(pinnedAssignments, false);
		Arrays.fill(assignedSections, 0);
		entryCount = totalAmount = totalIncomingReserved = 0;
	}

	private void resize(int size) {
		resourceIds = Arrays.copyOf(resourceIds, size);
		amounts = Arrays.copyOf(amounts, size);
		pickupReserved = Arrays.copyOf(pickupReserved, size);
		incomingReserved = Arrays.copyOf(incomingReserved, size);
		pinnedAssignments = Arrays.copyOf(pinnedAssignments, size);
		assignedSections = Arrays.copyOf(assignedSections, size);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if (formatVersion != 1 && formatVersion != FORMAT_VERSION)
			throw new IOException("Unsupported Choir multi-resource storage format: " + formatVersion);
		try {
			validateLimits(capacity, maxResourceKinds);
			if (resourceIds == null || amounts == null || pickupReserved == null || incomingReserved == null
					|| pinnedAssignments == null
					|| resourceIds.length != maxResourceKinds || amounts.length != maxResourceKinds
					|| pickupReserved.length != maxResourceKinds || incomingReserved.length != maxResourceKinds
					|| pinnedAssignments.length != maxResourceKinds
					|| entryCount < 0 || entryCount > maxResourceKinds)
				throw new IllegalStateException("Malformed serialized storage arrays.");
			if (formatVersion == 1) {
				assignedSections = new int[maxResourceKinds];
				for (int i = 0; i < entryCount; i++) if (pinnedAssignments[i]) assignedSections[i] = 1;
				formatVersion = FORMAT_VERSION;
			} else if (assignedSections == null || assignedSections.length != maxResourceKinds) {
				throw new IllegalStateException("Malformed serialized section assignments.");
			}
			long rebuiltAmount = 0;
			long rebuiltIncoming = 0;
			int incomingOwners = 0;
			int rebuiltSections = 0;
			for (int i = 0; i < entryCount; i++) {
				requireResourceId(resourceIds[i]);
				if (amounts[i] < 0 || pickupReserved[i] < 0 || pickupReserved[i] > amounts[i]
						|| incomingReserved[i] < 0 || assignedSections[i] < 0)
					throw new IllegalStateException("Negative or inconsistent entry.");
				for (int j = 0; j < i; j++) if (resourceIds[i].equals(resourceIds[j]))
					throw new IllegalStateException("Duplicate resource ID: " + resourceIds[i]);
				rebuiltAmount += amounts[i];
				rebuiltIncoming += incomingReserved[i];
				rebuiltSections += assignedSections[i];
				pinnedAssignments[i] = assignedSections[i] > 0;
				if (incomingReserved[i] > 0) incomingOwners++;
			}
			for (int i = entryCount; i < maxResourceKinds; i++) {
				if (resourceIds[i] != null || amounts[i] != 0 || pickupReserved[i] != 0
						|| incomingReserved[i] != 0 || pinnedAssignments[i] || assignedSections[i] != 0)
					throw new IllegalStateException("Serialized storage has hidden data outside active entries.");
			}
			if (incomingOwners > 1 || rebuiltAmount + rebuiltIncoming > capacity
					|| rebuiltSections > maxResourceKinds)
				throw new IllegalStateException("Serialized storage exceeds its invariants.");
			totalAmount = (int) rebuiltAmount;
			totalIncomingReserved = (int) rebuiltIncoming;
		} catch (RuntimeException invalid) {
			throw new IOException("Invalid Choir multi-resource storage state: " + invalid.getMessage(), invalid);
		}
	}

	private static void validateLimits(int capacity, int maxResourceKinds) {
		if (capacity < 0) throw new IllegalArgumentException("Storage capacity must not be negative: " + capacity);
		if (maxResourceKinds < 1 || maxResourceKinds > ChoirStorage.HARD_MAX_RESOURCE_KINDS)
			throw new IllegalArgumentException("Resource-kind limit must be between 1 and "
					+ ChoirStorage.HARD_MAX_RESOURCE_KINDS + ": " + maxResourceKinds);
	}

	private static String requireResourceId(String value) {
		if (value == null || value.length() == 0) throw new IllegalArgumentException("Stable resource ID is required.");
		return value;
	}

	private void checkEntry(int index) {
		if (index < 0 || index >= entryCount) throw new IndexOutOfBoundsException("Storage entry " + index);
	}
}
