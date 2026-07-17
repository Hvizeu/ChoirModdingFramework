package choir.internal.resources;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable presentation result. It stores stable IDs, not game resources or indices. */
public final class ResourceDisplayModel {
	private final long modelGeneration;
	private final long registrationGeneration;
	private final List<String> canonicalResourceIds;
	private final List<Group> groups;
	private final List<FallbackSection> fallbackSections;
	private final List<Diagnostic> diagnostics;
	private final Map<String, String> effectiveAssignments;
	private final String contentSignature;

	ResourceDisplayModel(long modelGeneration, long registrationGeneration, List<String> canonicalResourceIds,
			List<Group> groups, List<FallbackSection> fallbackSections, List<Diagnostic> diagnostics,
			Map<String, String> effectiveAssignments, String contentSignature) {
		this.modelGeneration = modelGeneration;
		this.registrationGeneration = registrationGeneration;
		this.canonicalResourceIds = List.copyOf(canonicalResourceIds);
		this.groups = List.copyOf(groups);
		this.fallbackSections = List.copyOf(fallbackSections);
		this.diagnostics = List.copyOf(diagnostics);
		this.effectiveAssignments = Map.copyOf(effectiveAssignments);
		this.contentSignature = contentSignature;
	}

	public long modelGeneration() { return modelGeneration; }
	public long registrationGeneration() { return registrationGeneration; }
	public List<String> canonicalResourceIds() { return canonicalResourceIds; }
	public List<Group> groups() { return groups; }
	public List<FallbackSection> fallbackSections() { return fallbackSections; }
	public List<Diagnostic> diagnostics() { return diagnostics; }
	public Optional<String> effectiveGroupId(String resourceId) { return Optional.ofNullable(effectiveAssignments.get(resourceId)); }
	public String contentSignature() { return contentSignature; }

	public int fallbackResourceCount() {
		int count = 0;
		for (FallbackSection section : fallbackSections) count += section.resourceIds().size();
		return count;
	}

	public static final class Group {
		private final String groupId;
		private final String label;
		private final String localizationKey;
		private final String winningProviderId;
		private final List<String> contributorIds;
		private final List<String> resourceIds;
		private final int sortOrder;

		Group(String groupId, String label, String localizationKey, String winningProviderId,
				List<String> contributorIds, List<String> resourceIds, int sortOrder) {
			this.groupId = groupId;
			this.label = label;
			this.localizationKey = localizationKey;
			this.winningProviderId = winningProviderId;
			this.contributorIds = List.copyOf(contributorIds);
			this.resourceIds = List.copyOf(resourceIds);
			this.sortOrder = sortOrder;
		}

		public String groupId() { return groupId; }
		public String label() { return label; }
		public String localizationKey() { return localizationKey; }
		public String winningProviderId() { return winningProviderId; }
		public List<String> contributorIds() { return contributorIds; }
		public List<String> resourceIds() { return resourceIds; }
		public int sortOrder() { return sortOrder; }
	}

	public static final class FallbackSection {
		private final String groupId;
		private final String label;
		private final int nativeCategory;
		private final List<String> resourceIds;

		FallbackSection(String groupId, String label, int nativeCategory, List<String> resourceIds) {
			this.groupId = groupId;
			this.label = label;
			this.nativeCategory = nativeCategory;
			this.resourceIds = List.copyOf(resourceIds);
		}

		public String groupId() { return groupId; }
		public String label() { return label; }
		public int nativeCategory() { return nativeCategory; }
		public List<String> resourceIds() { return resourceIds; }
	}

	public static final class Diagnostic {
		private final String code;
		private final String detail;

		Diagnostic(String code, String detail) { this.code = code; this.detail = detail; }
		public String code() { return code; }
		public String detail() { return detail; }
	}
}
