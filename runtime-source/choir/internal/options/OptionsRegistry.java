package choir.internal.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import choir.api.options.OptionChangeListener;
import choir.api.options.OptionListenerRegistrationResult;
import choir.api.options.OptionRegistrationResult;
import choir.api.options.OptionSchema;
import choir.api.options.OptionSetting;
import choir.api.options.OptionScope;
import choir.api.options.OptionType;

public final class OptionsRegistry {

	private static final Map<String, OptionSchema> schemas = new LinkedHashMap<String, OptionSchema>();
	private static final Map<String, OptionsProviderCatalogEntry> catalog = new LinkedHashMap<String, OptionsProviderCatalogEntry>();
	private static final Map<String, Map<String, Object>> values = new LinkedHashMap<String, Map<String, Object>>();
	private static final Map<String, Map<String, Object>> drafts = new LinkedHashMap<String, Map<String, Object>>();
	private static final Map<String, List<OptionChangeListener>> listeners = new HashMap<String, List<OptionChangeListener>>();
	private static boolean editorOpen;

	private OptionsRegistry() {
	}

	public static synchronized OptionRegistrationResult register(OptionSchema schema) {
		String problem = validate(schema);
		if (problem != null) {
			OptionsDiagnostics.warn("REGISTRATION rejected reason=INVALID detail=" + problem);
			return OptionRegistrationResult.REJECTED_INVALID;
		}
		OptionSchema existing = schemas.get(schema.providerId());
		if (existing != null) {
			if (existing.equals(schema)) {
				OptionsDiagnostics.info("REGISTRATION idempotent provider=" + schema.providerId());
				return OptionRegistrationResult.IDEMPOTENT;
			}
			OptionsDiagnostics.warn("REGISTRATION rejected reason=CONFLICT provider=" + schema.providerId());
			return OptionRegistrationResult.REJECTED_CONFLICT;
		}
		LinkedHashMap<String, Object> initialized = defaultsFor(schema);
		Map<String, Object> loaded = OptionsStore.loadGlobal(schema);
		for (OptionSetting setting : schema.settings()) {
			if (setting.scope() != OptionScope.GLOBAL || !isValueSetting(setting)) {
				continue;
			}
			if (loaded.containsKey(setting.key())) {
				Object normalized = normalize(schema.providerId(), setting, loaded.get(setting.key()));
				initialized.put(setting.key(), normalized);
			}
		}
		schemas.put(schema.providerId(), schema);
		values.put(schema.providerId(), initialized);
		// A compatible script can register after the screen has been opened. Give
		// that schema a complete draft instead of showing null/minimum values.
		if (editorOpen) {
			drafts.put(schema.providerId(), new LinkedHashMap<String, Object>(initialized));
		}
		OptionsDiagnostics.info("REGISTRATION accepted provider=" + schema.providerId() + " settings=" + schema.settings().size()
				+ " schemaVersion=" + schema.schemaVersion());
		return OptionRegistrationResult.ACCEPTED;
	}

	public static synchronized OptionListenerRegistrationResult subscribe(String providerId, OptionChangeListener listener) {
		if (!validId(providerId) || listener == null) {
			return OptionListenerRegistrationResult.REJECTED_INVALID;
		}
		List<OptionChangeListener> list = listeners.get(providerId);
		if (list == null) {
			list = new ArrayList<OptionChangeListener>();
			listeners.put(providerId, list);
		}
		for (OptionChangeListener existing : list) {
			if (existing == listener) return OptionListenerRegistrationResult.IDEMPOTENT;
		}
		list.add(listener);
		OptionsDiagnostics.info("LISTENER accepted provider=" + providerId + " listeners=" + list.size());
		return OptionListenerRegistrationResult.ACCEPTED;
	}

	public static synchronized List<OptionSchema> schemasSorted() {
		ArrayList<OptionSchema> list = new ArrayList<OptionSchema>(schemas.values());
		Collections.sort(list, new Comparator<OptionSchema>() {
			@Override
			public int compare(OptionSchema a, OptionSchema b) {
				int byName = a.displayName().compareToIgnoreCase(b.displayName());
				return byName != 0 ? byName : a.providerId().compareToIgnoreCase(b.providerId());
			}
		});
		return list;
	}

	public static synchronized void replaceCatalog(List<OptionsProviderCatalogEntry> declarations) {
		LinkedHashMap<String, OptionsProviderCatalogEntry> next = new LinkedHashMap<String, OptionsProviderCatalogEntry>();
		HashMap<String, Boolean> conflicted = new HashMap<String, Boolean>();
		if (declarations != null) {
			for (OptionsProviderCatalogEntry declaration : declarations) {
				String problem = validateCatalog(declaration);
				if (problem != null) {
					OptionsDiagnostics.warn("CATALOG rejected reason=INVALID detail=" + problem);
					continue;
				}
				OptionsProviderCatalogEntry existing = next.get(declaration.providerId());
				if (existing == null) {
					next.put(declaration.providerId(), declaration);
				} else if (!existing.equals(declaration)) {
					conflicted.put(declaration.providerId(), Boolean.TRUE);
				}
			}
		}
		for (String providerId : conflicted.keySet()) {
			next.remove(providerId);
			OptionsDiagnostics.warn("CATALOG rejected reason=CONFLICT provider=" + providerId);
		}
		if (!catalog.equals(next)) {
			catalog.clear();
			catalog.putAll(next);
			OptionsDiagnostics.info("CATALOG applied providers=" + catalog.size());
		}
	}

	public static synchronized List<OptionsProviderView> providersSorted() {
		HashMap<String, Boolean> ids = new HashMap<String, Boolean>();
		ids.putAll(toPresenceMap(catalog.keySet()));
		ids.putAll(toPresenceMap(schemas.keySet()));
		ArrayList<OptionsProviderView> providers = new ArrayList<OptionsProviderView>();
		for (String providerId : ids.keySet()) {
			providers.add(new OptionsProviderView(catalog.get(providerId), schemas.get(providerId)));
		}
		Collections.sort(providers, new Comparator<OptionsProviderView>() {
			@Override
			public int compare(OptionsProviderView a, OptionsProviderView b) {
				int byName = a.displayName().compareToIgnoreCase(b.displayName());
				return byName != 0 ? byName : a.providerId().compareToIgnoreCase(b.providerId());
			}
		});
		return providers;
	}

	public static synchronized OptionsProviderView provider(String providerId) {
		OptionsProviderCatalogEntry declaration = catalog.get(providerId);
		OptionSchema schema = schemas.get(providerId);
		return declaration == null && schema == null ? null : new OptionsProviderView(declaration, schema);
	}

	public static synchronized OptionSchema schema(String providerId) {
		return schemas.get(providerId);
	}

	public static synchronized void openEditor() {
		if (editorOpen) {
			return;
		}
		drafts.clear();
		for (Map.Entry<String, Map<String, Object>> entry : values.entrySet()) {
			drafts.put(entry.getKey(), new LinkedHashMap<String, Object>(entry.getValue()));
		}
		editorOpen = true;
	}

	public static synchronized void cancelEdits() {
		drafts.clear();
		editorOpen = false;
	}

	public static synchronized boolean hasDraft() {
		return editorOpen;
	}

	public static synchronized Object currentValue(String providerId, String key) {
		Map<String, Object> map = values.get(providerId);
		return map == null ? null : map.get(key);
	}

	public static synchronized Object draftValue(String providerId, String key) {
		if (!editorOpen) {
			openEditor();
		}
		Map<String, Object> map = drafts.get(providerId);
		if (map == null) {
			map = new LinkedHashMap<String, Object>();
			drafts.put(providerId, map);
		}
		return map.get(key);
	}

	public static synchronized void setDraft(String providerId, String key, Object value) {
		if (!editorOpen) {
			openEditor();
		}
		OptionSchema schema = schemas.get(providerId);
		OptionSetting setting = setting(schema, key);
		if (setting == null || !isValueSetting(setting) || setting.readOnly()) {
			return;
		}
		Map<String, Object> map = drafts.get(providerId);
		if (map == null) {
			map = new LinkedHashMap<String, Object>();
			drafts.put(providerId, map);
		}
		map.put(key, normalize(providerId, setting, value));
	}

	public static synchronized void resetDraftToDefaults(String providerId) {
		if (!editorOpen) {
			openEditor();
		}
		OptionSchema schema = schemas.get(providerId);
		if (schema == null) {
			return;
		}
		drafts.put(providerId, defaultsFor(schema));
	}

	public static boolean resetCurrentToDefaults(String providerId) {
		List<Change> changes = new ArrayList<Change>();
		synchronized (OptionsRegistry.class) {
			OptionSchema schema = schemas.get(providerId);
			if (schema == null) return false;
			Map<String, Object> oldValues = values.get(providerId);
			LinkedHashMap<String, Object> defaults = defaultsFor(schema);
			for (OptionSetting setting : schema.settings()) {
				if (!isValueSetting(setting)) continue;
				Object oldValue = oldValues == null ? null : oldValues.get(setting.key());
				Object newValue = defaults.get(setting.key());
				if (!equalsValue(oldValue, newValue)) changes.add(new Change(providerId, setting.key(), oldValue, newValue));
			}
			values.put(providerId, defaults);
			OptionsStore.saveGlobal(schema, defaults);
		}
		for (Change change : changes) notifyChange(change);
		OptionsDiagnostics.info("RESET provider=" + providerId + " changed=" + changes.size());
		return true;
	}

	public static List<String> applyDrafts() {
		ArrayList<String> changed = new ArrayList<String>();
		LinkedHashMap<String, Change> notifications = new LinkedHashMap<String, Change>();
		synchronized (OptionsRegistry.class) {
			if (!editorOpen) return changed;
			for (Map.Entry<String, Map<String, Object>> entry : drafts.entrySet()) {
				String providerId = entry.getKey();
				OptionSchema schema = schemas.get(providerId);
				if (schema == null) {
					continue;
				}
				Map<String, Object> current = values.get(providerId);
				if (current == null) {
					current = new LinkedHashMap<String, Object>();
					values.put(providerId, current);
				}
				for (OptionSetting setting : schema.settings()) {
					if (!isValueSetting(setting) || setting.scope() != OptionScope.GLOBAL) continue;
					Object oldValue = current.get(setting.key());
					Object newValue = entry.getValue().get(setting.key());
					if (!equalsValue(oldValue, newValue)) {
						current.put(setting.key(), newValue);
						changed.add(providerId + "." + setting.key());
						notifications.put(providerId + "\n" + setting.key(), new Change(providerId, setting.key(), oldValue, newValue));
					}
				}
				OptionsStore.saveGlobal(schema, current);
			}
			drafts.clear();
			editorOpen = false;
		}
		for (Change change : notifications.values()) notifyChange(change);
		OptionsDiagnostics.info("APPLY changed=" + changed.size());
		return changed;
	}

	public static boolean getBoolean(String providerId, String key, boolean fallback) {
		Object value = currentValue(providerId, key);
		if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue();
		}
		diagnoseBadRead(providerId, key, "boolean", value);
		return fallback;
	}

	public static int getInt(String providerId, String key, int fallback) {
		Object value = currentValue(providerId, key);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		diagnoseBadRead(providerId, key, "int", value);
		return fallback;
	}

	public static double getDouble(String providerId, String key, double fallback) {
		Object value = currentValue(providerId, key);
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		diagnoseBadRead(providerId, key, "double", value);
		return fallback;
	}

	public static String getString(String providerId, String key, String fallback) {
		Object value = currentValue(providerId, key);
		if (value instanceof String) {
			return (String) value;
		}
		if (value instanceof Boolean || value instanceof Number) {
			return String.valueOf(value);
		}
		diagnoseBadRead(providerId, key, "string", value);
		return fallback;
	}

	private static void notifyChange(Change change) {
		List<OptionChangeListener> list;
		synchronized (OptionsRegistry.class) {
			List<OptionChangeListener> registered = listeners.get(change.providerId);
			list = registered == null ? null : new ArrayList<OptionChangeListener>(registered);
		}
		if (list == null) {
			return;
		}
		for (OptionChangeListener listener : list) {
			try {
				listener.onOptionChanged(change.providerId, change.key, change.oldValue, change.newValue);
			} catch (Throwable t) {
				OptionsDiagnostics.error("Subscriber failed for " + change.providerId + "." + change.key, t);
			}
		}
	}

	private static void diagnoseBadRead(String providerId, String key, String type, Object value) {
		if (!schemas.containsKey(providerId) || setting(schemas.get(providerId), key) == null) {
			OptionsDiagnostics.warn("Missing config value read as " + type + ": " + providerId + "." + key);
		} else if (value != null) {
			OptionsDiagnostics.warn("Type mismatch reading " + providerId + "." + key + " as " + type + ": " + value.getClass().getSimpleName());
		}
	}

	private static LinkedHashMap<String, Object> defaultsFor(OptionSchema schema) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		for (OptionSetting setting : schema.settings()) {
			if (isValueSetting(setting) && setting.scope() == OptionScope.GLOBAL) {
				map.put(setting.key(), normalize(schema.providerId(), setting, setting.defaultValue()));
			}
		}
		return map;
	}

	private static boolean equalsValue(Object a, Object b) {
		return a == b || (a != null && a.equals(b));
	}

	private static OptionSetting setting(OptionSchema schema, String key) {
		if (schema == null || key == null) {
			return null;
		}
		for (OptionSetting setting : schema.settings()) {
			if (key.equals(setting.key())) {
				return setting;
			}
		}
		return null;
	}

	private static boolean isValueSetting(OptionSetting setting) {
		return setting.type() == OptionType.BOOLEAN || setting.type() == OptionType.INT || setting.type() == OptionType.FLOAT
				|| setting.type() == OptionType.STRING || setting.type() == OptionType.ENUM;
	}

	private static Object normalize(String providerId, OptionSetting setting, Object raw) {
		try {
			switch (setting.type()) {
			case BOOLEAN:
				if (raw instanceof Boolean) {
					return raw;
				}
				if (raw instanceof String) {
					return Boolean.valueOf(Boolean.parseBoolean((String) raw));
				}
				return Boolean.valueOf(Boolean.TRUE.equals(setting.defaultValue()));
			case INT:
				return Integer.valueOf((int) Math.round(clamp(number(raw, number(setting.defaultValue(), 0)), setting)));
			case FLOAT:
				return Double.valueOf(clamp(number(raw, number(setting.defaultValue(), 0)), setting));
			case STRING:
				return trim(String.valueOf(raw == null ? setting.defaultValue() : raw), 1000);
			case ENUM:
				String value = String.valueOf(raw == null ? setting.defaultValue() : raw);
				if (setting.enumValues().contains(value)) {
					return value;
				}
				OptionsDiagnostics.warn("Invalid enum value for " + providerId + "." + setting.key() + ": " + value);
				return setting.defaultValue();
			default:
				return setting.defaultValue();
			}
		} catch (Exception e) {
			OptionsDiagnostics.error("Invalid value for " + providerId + "." + setting.key() + " fell back to default", e);
			return setting.defaultValue();
		}
	}

	private static double number(Object value, double fallback) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		if (value instanceof String) {
			return Double.parseDouble((String) value);
		}
		return fallback;
	}

	private static double clamp(double value, OptionSetting setting) {
		double out = value;
		if (setting.minimum() != null && out < setting.minimum().doubleValue()) {
			out = setting.minimum().doubleValue();
		}
		if (setting.maximum() != null && out > setting.maximum().doubleValue()) {
			out = setting.maximum().doubleValue();
		}
		return out;
	}

	private static String trim(String value, int max) {
		return value.length() > max ? value.substring(0, max) : value;
	}

	private static String validate(OptionSchema schema) {
		if (schema == null) {
			return "schema is null";
		}
		if (!validId(schema.providerId())) {
			return "invalid provider id " + schema.providerId();
		}
		if (schema.displayName() == null || schema.displayName().isBlank() || schema.displayName().length() > 200)
			return "invalid display name for " + schema.providerId();
		if (schema.description().length() > 1000) return "description is longer than 1000 characters " + schema.providerId();
		if (schema.schemaVersion() < 1) return "invalid schema version " + schema.providerId();
		if (schema.settings().size() > 512) return "too many settings " + schema.providerId();
		HashMap<String, Boolean> keys = new HashMap<String, Boolean>();
		for (OptionSetting setting : schema.settings()) {
			String problem = validateSetting(schema.providerId(), setting);
			if (problem != null) {
				return problem;
			}
			if (keys.containsKey(setting.key())) {
				return "duplicate setting key " + schema.providerId() + "." + setting.key();
			}
			keys.put(setting.key(), Boolean.TRUE);
		}
		return null;
	}

	private static String validateCatalog(OptionsProviderCatalogEntry declaration) {
		if (declaration == null) return "declaration is null";
		if (!validId(declaration.modId())) return "invalid mod id " + declaration.modId();
		if (!validId(declaration.providerId())) return "invalid provider id " + declaration.providerId();
		if (declaration.displayName().isBlank() || declaration.displayName().length() > 200)
			return "invalid display name for " + declaration.providerId();
		if (declaration.description().length() > 1000)
			return "description is longer than 1000 characters " + declaration.providerId();
		return null;
	}

	private static Map<String, Boolean> toPresenceMap(Iterable<String> values) {
		HashMap<String, Boolean> out = new HashMap<String, Boolean>();
		for (String value : values) out.put(value, Boolean.TRUE);
		return out;
	}

	private static String validateSetting(String providerId, OptionSetting setting) {
		if (setting == null) {
			return "null setting in " + providerId;
		}
		if (!validId(setting.key())) {
			return "invalid setting key " + providerId + "." + setting.key();
		}
		if (setting.label() == null || setting.label().isBlank() || setting.label().length() > 200)
			return "invalid setting label " + providerId + "." + setting.key();
		if (setting.description().length() > 1000) return "setting description is longer than 1000 characters " + providerId + "." + setting.key();
		if (setting.schemaVersion() < 1) return "invalid setting schema version " + providerId + "." + setting.key();
		if (setting.scope() != OptionScope.GLOBAL) {
			return "unsupported save-specific setting scope " + providerId + "." + setting.key();
		}
		if ((setting.type() == OptionType.INT || setting.type() == OptionType.FLOAT) && setting.minimum() != null && setting.maximum() != null
				&& setting.minimum().doubleValue() > setting.maximum().doubleValue()) {
			return "invalid numeric range " + providerId + "." + setting.key();
		}
		if ((setting.type() == OptionType.INT || setting.type() == OptionType.FLOAT) && setting.step() != null && setting.step().doubleValue() <= 0) {
			return "invalid numeric step " + providerId + "." + setting.key();
		}
		if (setting.type() == OptionType.INT || setting.type() == OptionType.FLOAT) {
			if (!(setting.defaultValue() instanceof Number)) {
				return "numeric default is not a number " + providerId + "." + setting.key();
			}
			double value = ((Number) setting.defaultValue()).doubleValue();
			if (Double.isNaN(value) || Double.isInfinite(value)) {
				return "numeric default is not finite " + providerId + "." + setting.key();
			}
			if (setting.minimum() != null && value < setting.minimum().doubleValue()) {
				return "numeric default is below minimum " + providerId + "." + setting.key();
			}
			if (setting.maximum() != null && value > setting.maximum().doubleValue()) {
				return "numeric default is above maximum " + providerId + "." + setting.key();
			}
		}
		if (setting.type() == OptionType.ENUM) {
			if (setting.enumValues().isEmpty()) {
				return "enum has no values " + providerId + "." + setting.key();
			}
			if (!setting.enumValues().contains(String.valueOf(setting.defaultValue()))) {
				return "enum default is not allowed " + providerId + "." + setting.key();
			}
			HashMap<String, Boolean> values = new HashMap<String, Boolean>();
			for (String value : setting.enumValues()) {
				if (value == null || value.isEmpty() || value.length() > 200) return "invalid enum value " + providerId + "." + setting.key();
				if (values.put(value, Boolean.TRUE) != null) return "duplicate enum value " + providerId + "." + setting.key() + "=" + value;
			}
		}
		if (setting.type() == OptionType.BOOLEAN && !(setting.defaultValue() instanceof Boolean)) {
			return "boolean default is not a boolean " + providerId + "." + setting.key();
		}
		if ((setting.type() == OptionType.STRING || setting.type() == OptionType.INFO || setting.type() == OptionType.READ_ONLY)
				&& String.valueOf(setting.defaultValue()).length() > 1000) {
			return "string default is longer than 1000 characters " + providerId + "." + setting.key();
		}
		return null;
	}

	private static boolean validId(String id) {
		if (id == null || id.isEmpty() || id.length() > 128) {
			return false;
		}
		for (int i = 0; i < id.length(); i++) {
			char c = id.charAt(i);
			if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.')) {
				return false;
			}
		}
		return true;
	}

	private static final class Change {
		final String providerId;
		final String key;
		final Object oldValue;
		final Object newValue;

		Change(String providerId, String key, Object oldValue, Object newValue) {
			this.providerId = providerId;
			this.key = key;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}
	}
}
