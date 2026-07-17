package choir.internal;
public interface AdapterBootstrap {
	boolean initialize();
	void beforeGameInited();
	String adapterVersion();
	String gameVersion();
}
