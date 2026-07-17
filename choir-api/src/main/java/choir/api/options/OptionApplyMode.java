package choir.api.options;

/** Describes when a changed option is expected to take full effect. */
public enum OptionApplyMode {
	IMMEDIATE,
	WORLD_RELOAD,
	RESTART
}
