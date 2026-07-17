package choir.api.options;

@FunctionalInterface
public interface OptionChangeListener {
	void onOptionChanged(String providerId, String optionKey, Object oldValue, Object newValue);
}
