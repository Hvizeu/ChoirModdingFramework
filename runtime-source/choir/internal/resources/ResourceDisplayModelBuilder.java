package choir.internal.resources;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import choir.api.experimental.resources.ResourceDisplayAssignment;
import choir.api.experimental.resources.ResourceDisplayGroupDefinition;
import choir.internal.resources.ResourceDisplayModel.Diagnostic;
import choir.internal.resources.ResourceDisplayModel.FallbackSection;
import choir.internal.resources.ResourceDisplayModel.Group;
import choir.internal.resources.ResourceDisplayRegistry.Snapshot;

/** Pure deterministic builder. V71 adapters supply copied current-registry entries. */
public final class ResourceDisplayModelBuilder {
	private static final Comparator<ResourceDisplayGroupDefinition> DEFINITION_WINNER =
			Comparator.comparingInt(ResourceDisplayGroupDefinition::definitionPriority).reversed()
					.thenComparing(ResourceDisplayGroupDefinition::providerId)
					.thenComparing(ResourceDisplayGroupDefinition::groupId);
	private static final Comparator<ResourceDisplayAssignment> ASSIGNMENT_WINNER =
			Comparator.comparingInt(ResourceDisplayAssignment::assignmentPriority).reversed()
					.thenComparing(ResourceDisplayAssignment::providerId)
					.thenComparing(ResourceDisplayAssignment::groupId)
					.thenComparing(ResourceDisplayAssignment::resourceId);
	private ResourceDisplayModelBuilder() { }

	public static ResourceDisplayModel build(long modelGeneration, Snapshot snapshot,
			List<ResourceEntry> canonicalResources) {
		if (snapshot == null) throw new IllegalArgumentException("Resource display snapshot must not be null.");
		if (canonicalResources == null) throw new IllegalArgumentException("Canonical resource entries must not be null.");

		List<ResourceEntry> canonical = List.copyOf(canonicalResources);
		List<String> canonicalIds = new ArrayList<String>(canonical.size());
		Map<String, ResourceEntry> currentById = new LinkedHashMap<String, ResourceEntry>();
		Map<String, Integer> canonicalPositions = new HashMap<String, Integer>();
		for (int position = 0; position < canonical.size(); position++) {
			ResourceEntry entry = canonical.get(position);
			if (entry == null) throw new IllegalArgumentException("Canonical resource entry must not be null.");
			if (entry.canonicalPosition() != position)
				throw new IllegalArgumentException("Canonical position mismatch for " + entry.resourceId()
						+ ": expected=" + position + " observed=" + entry.canonicalPosition());
			if (currentById.put(entry.resourceId(), entry) != null)
				throw new IllegalArgumentException("Duplicate canonical resource ID: " + entry.resourceId());
			canonicalIds.add(entry.resourceId());
			canonicalPositions.put(entry.resourceId(), position);
		}

		List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
		Map<String, EffectiveDefinition> definitions = resolveDefinitions(snapshot.groups(), diagnostics);
		Map<String, ResourceDisplayAssignment> assignments = resolveAssignments(snapshot.assignments(),
				currentById.keySet(), definitions.keySet(), diagnostics);

		Map<String, List<ResourceDisplayAssignment>> resourcesByGroup = new HashMap<String, List<ResourceDisplayAssignment>>();
		for (ResourceDisplayAssignment assignment : assignments.values())
			resourcesByGroup.computeIfAbsent(assignment.groupId(), ignored -> new ArrayList<ResourceDisplayAssignment>()).add(assignment);

		List<EffectiveDefinition> orderedDefinitions = new ArrayList<EffectiveDefinition>(definitions.values());
		orderedDefinitions.sort(Comparator.comparingInt((EffectiveDefinition value) -> value.winner.sortOrder())
				.thenComparing(value -> value.winner.groupId()));
		List<Group> groups = new ArrayList<Group>();
		for (EffectiveDefinition definition : orderedDefinitions) {
			List<ResourceDisplayAssignment> values = resourcesByGroup.get(definition.winner.groupId());
			if (values == null || values.isEmpty()) continue;
			values.sort(Comparator.comparingInt(ResourceDisplayAssignment::resourceSortOrder)
					.thenComparingInt(value -> canonicalPositions.get(value.resourceId()))
					.thenComparing(ResourceDisplayAssignment::resourceId)
					.thenComparing(ResourceDisplayAssignment::providerId));
			List<String> resourceIds = new ArrayList<String>(values.size());
			for (ResourceDisplayAssignment value : values) resourceIds.add(value.resourceId());
			groups.add(new Group(definition.winner.groupId(), definition.label, definition.winner.localizationKey(),
					definition.winner.providerId(), definition.contributors, resourceIds, definition.winner.sortOrder()));
		}

		List<FallbackSection> fallback = fallback(canonical, assignments.keySet());
		Map<String, String> effectiveAssignments = new TreeMap<String, String>();
		for (ResourceDisplayAssignment assignment : assignments.values())
			effectiveAssignments.put(assignment.resourceId(), assignment.groupId());

		String signature = signature(groups, fallback, diagnostics, effectiveAssignments);
		return new ResourceDisplayModel(modelGeneration, snapshot.registrationGeneration(), canonicalIds,
				groups, fallback, diagnostics, effectiveAssignments, signature);
	}

	private static Map<String, EffectiveDefinition> resolveDefinitions(List<ResourceDisplayGroupDefinition> input,
			List<Diagnostic> diagnostics) {
		Map<String, List<ResourceDisplayGroupDefinition>> candidates = new TreeMap<String, List<ResourceDisplayGroupDefinition>>();
		for (ResourceDisplayGroupDefinition value : input)
			candidates.computeIfAbsent(value.groupId(), ignored -> new ArrayList<ResourceDisplayGroupDefinition>()).add(value);

		Map<String, EffectiveDefinition> result = new TreeMap<String, EffectiveDefinition>();
		for (Map.Entry<String, List<ResourceDisplayGroupDefinition>> entry : candidates.entrySet()) {
			List<ResourceDisplayGroupDefinition> values = entry.getValue();
			values.sort(DEFINITION_WINNER);
			ResourceDisplayGroupDefinition winner = values.get(0);
			TreeSet<String> contributors = new TreeSet<String>();
			for (ResourceDisplayGroupDefinition value : values) {
				contributors.add(value.providerId());
				if (value != winner && !sameMetadata(winner, value))
					diagnostics.add(new Diagnostic("GROUP_METADATA_CONFLICT", "group=" + entry.getKey()
							+ " winner=" + winner.providerId() + " loser=" + value.providerId()));
			}
			String label = resolveLabel(winner, diagnostics);
			result.put(entry.getKey(), new EffectiveDefinition(winner, List.copyOf(contributors), label));
		}
		return result;
	}

	private static Map<String, ResourceDisplayAssignment> resolveAssignments(List<ResourceDisplayAssignment> input,
			Set<String> currentResources, Set<String> definedGroups, List<Diagnostic> diagnostics) {
		List<ResourceDisplayAssignment> ordered = new ArrayList<ResourceDisplayAssignment>(input);
		ordered.sort(Comparator.comparing(ResourceDisplayAssignment::providerId)
				.thenComparing(ResourceDisplayAssignment::resourceId)
				.thenComparing(ResourceDisplayAssignment::groupId));
		Map<String, List<ResourceDisplayAssignment>> candidates = new TreeMap<String, List<ResourceDisplayAssignment>>();
		for (ResourceDisplayAssignment value : ordered) {
			if (!currentResources.contains(value.resourceId())) {
				diagnostics.add(new Diagnostic("MISSING_RESOURCE", "provider=" + value.providerId()
						+ " resource=" + value.resourceId() + " group=" + value.groupId()));
				continue;
			}
			if (!definedGroups.contains(value.groupId())) {
				diagnostics.add(new Diagnostic("MISSING_GROUP", "provider=" + value.providerId()
						+ " resource=" + value.resourceId() + " group=" + value.groupId()));
				continue;
			}
			candidates.computeIfAbsent(value.resourceId(), ignored -> new ArrayList<ResourceDisplayAssignment>()).add(value);
		}

		Map<String, ResourceDisplayAssignment> result = new TreeMap<String, ResourceDisplayAssignment>();
		for (Map.Entry<String, List<ResourceDisplayAssignment>> entry : candidates.entrySet()) {
			List<ResourceDisplayAssignment> values = entry.getValue();
			values.sort(ASSIGNMENT_WINNER);
			ResourceDisplayAssignment winner = values.get(0);
			Set<String> losingGroups = new TreeSet<String>();
			for (int i = 1; i < values.size(); i++)
				if (!winner.groupId().equals(values.get(i).groupId())) losingGroups.add(values.get(i).groupId());
			if (!losingGroups.isEmpty()) diagnostics.add(new Diagnostic("ASSIGNMENT_CONFLICT",
					"resource=" + entry.getKey() + " winner.provider=" + winner.providerId()
							+ " winner.group=" + winner.groupId() + " losing.groups=" + String.join(",", losingGroups)));
			result.put(entry.getKey(), winner);
		}
		return result;
	}

	private static List<FallbackSection> fallback(List<ResourceEntry> canonical, Set<String> assigned) {
		Map<Integer, List<String>> byCategory = new TreeMap<Integer, List<String>>();
		for (ResourceEntry entry : canonical) {
			if (assigned.contains(entry.resourceId())) continue;
			byCategory.computeIfAbsent(entry.nativeCategory(), ignored -> new ArrayList<String>()).add(entry.resourceId());
		}
		List<FallbackSection> immutable = new ArrayList<FallbackSection>(byCategory.size());
		for (Map.Entry<Integer, List<String>> entry : byCategory.entrySet()) {
			int category = entry.getKey();
			immutable.add(new FallbackSection("choir.fallback.native_category." + category,
					"Native category " + category, category, entry.getValue()));
		}
		return immutable;
	}

	private static boolean sameMetadata(ResourceDisplayGroupDefinition a, ResourceDisplayGroupDefinition b) {
		return a.groupId().equals(b.groupId()) && a.localizationKey().equals(b.localizationKey())
				&& a.fallbackLabel().equals(b.fallbackLabel())
				&& a.definitionPriority() == b.definitionPriority() && a.sortOrder() == b.sortOrder();
	}

	private static String resolveLabel(ResourceDisplayGroupDefinition value, List<Diagnostic> diagnostics) {
		try {
			CharSequence resolved = value.labelResolver().get();
			if (resolved != null && !resolved.toString().trim().isEmpty()
					&& !resolved.toString().equals(value.localizationKey())) return resolved.toString();
		} catch (Throwable ignored) { }
		diagnostics.add(new Diagnostic("LABEL_FALLBACK", "provider=" + value.providerId()
				+ " group=" + value.groupId() + " localization=" + value.localizationKey()));
		return value.fallbackLabel();
	}

	private static String signature(List<Group> groups, List<FallbackSection> fallback,
			List<Diagnostic> diagnostics, Map<String, String> assignments) {
		StringBuilder value = new StringBuilder();
		for (Group group : groups) value.append("G|").append(group.groupId()).append('|').append(group.label())
				.append('|').append(group.winningProviderId()).append('|').append(group.sortOrder())
				.append('|').append(String.join(",", group.contributorIds())).append('|')
				.append(String.join(",", group.resourceIds())).append('\n');
		for (FallbackSection section : fallback) value.append("F|").append(section.groupId()).append('|')
				.append(section.label()).append('|').append(section.nativeCategory()).append('|')
				.append(String.join(",", section.resourceIds())).append('\n');
		for (Map.Entry<String, String> entry : assignments.entrySet()) value.append("A|").append(entry.getKey())
				.append('|').append(entry.getValue()).append('\n');
		for (Diagnostic diagnostic : diagnostics) value.append("D|").append(diagnostic.code()).append('|')
				.append(diagnostic.detail()).append('\n');
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8));
			StringBuilder out = new StringBuilder();
			for (byte b : hash) out.append(String.format("%02x", b & 255));
			return out.toString();
		} catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable", e); }
	}

	private static final class EffectiveDefinition {
		final ResourceDisplayGroupDefinition winner;
		final List<String> contributors;
		final String label;
		EffectiveDefinition(ResourceDisplayGroupDefinition winner, List<String> contributors, String label) {
			this.winner = winner;
			this.contributors = contributors;
			this.label = label;
		}
	}

	public static final class ResourceEntry {
		private final String resourceId;
		private final int canonicalPosition;
		private final int nativeCategory;

		public ResourceEntry(String resourceId, int canonicalPosition, int nativeCategory) {
			if (resourceId == null || resourceId.isBlank()) throw new IllegalArgumentException("Resource ID must not be blank.");
			this.resourceId = resourceId;
			this.canonicalPosition = canonicalPosition;
			this.nativeCategory = nativeCategory;
		}

		public String resourceId() { return resourceId; }
		public int canonicalPosition() { return canonicalPosition; }
		public int nativeCategory() { return nativeCategory; }
	}
}
