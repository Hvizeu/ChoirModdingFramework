package choir.adapter.v71_44;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarFile;

/** Reads original target bytes directly from SongsOfSyx.jar, never from Choir's shadows. */
final class V7144RaceTargetFingerprint {
	private static final Map<String,String> EXPECTED = Map.ofEntries(
		Map.entry("init/race/RACES.class", "11b508bbd3bed6fb377d0e07ba1b4940b39ff9b6fd3dd850295629807907b1a0"),
		Map.entry("init/race/RaceInfo.class", "197ac1b4c68e0da24845e07ac8c009493ad24bb52882476ec09da21b534fdc3e"),
		Map.entry("util/info/INFO.class", "4cff8eca342c846fa4f3030dd1eca19d7c72cc0a11a66a6f892b3c1933d4b0e8"),
		Map.entry("init/race/RacePopulation.class", "ba6b99cd8c5cc08b128b5b27445afc9a24f4437d9a253f8f7dbac1d574c243be"),
		Map.entry("init/race/RacePreferrence.class", "8ce9dfc0f8693feaca48f1cf4b79d8ccc49d930c6dfc29af9f2b37a8d6e8f894"),
		Map.entry("init/race/RaceStats.class", "ce4d3c199d936ec1622d8ca54dc243bb8aa999e1f58dde5c4ea16d1dd65242d1"),
		Map.entry("init/race/RaceServiceSorter.class", "49f52f994b6954ef6dd97fd5e01a7a3f470f73dcaa8bc1c9f84569cb7e7435d0"),
		Map.entry("init/race/bio/Bio.class", "1a083009a6fef23e728ff0a7349050589a4abd75f1a2784d6b68eb6b9b1fd46e"),
		Map.entry("init/race/bio/BioOpinion.class", "ecaf3e346e8056499de959ef0f2a26f03f05989421716412602d7a5cfc7724f3"),
		Map.entry("settlement/stats/standing/StatStanding$StandingDef.class", "e0a624f19864fb207a2dc5de55b36caf9d80aa3f7f242bfcfd0e78264216a314"),
		Map.entry("settlement/stats/standing/StatStanding$StandingDef$StandingData.class", "7045eaebc50e642107b4999697802b249fe3d05114e47ad40d575f4572e35752"),
		Map.entry("init/race/home/RaceHome.class", "f9edd1c8f631f2d7817a41152bf7130cd9d4ee807cad004a3a56207bad3855b7"),
		Map.entry("init/race/home/RaceHomeClass.class", "6368d753a6b0bcc7b49722d4a765c99430e3152b75fb589f1cd9bf1c7d1af84d"),
		Map.entry("init/race/RaceResources.class", "a46cb6daa221a64d000cde7e02e31ecb4554880b8d58a07e0bc9eac493b24d95"),
		Map.entry("settlement/stats/colls/StatsHome.class", "8a9cb3c7fbb496420e6eb72d3b9ec01ffb1d0b3ca742d05206c1e3da403f498b"),
		Map.entry("settlement/stats/colls/StatsHome$StatFurniture.class", "e859051df3a65e50eca6f4a0819cec6ec32c70423bf57eabfabafa453ecc8632"),
		Map.entry("settlement/room/home/house/HomeInstance.class", "321c2e606854cc1786a41d9e6d3c9cec40a3805db0934172890e91edb97a030d"),
		Map.entry("settlement/room/home/chamber/ChamberInstance.class", "956f1e4638d2e590cbfe93ede70d2c5efdbf26c8083b940b699720ab080cc9dc")
	);
	static Result verify() {
		LinkedHashMap<String,String> actual = new LinkedHashMap<String,String>();
		try {
			for (String item : System.getProperty("java.class.path", "").split(java.io.File.pathSeparator)) {
				Path path = Paths.get(item); if (!path.getFileName().toString().equalsIgnoreCase("SongsOfSyx.jar") || !Files.isRegularFile(path)) continue;
				try (JarFile jar = new JarFile(path.toFile())) {
					for (String entry : EXPECTED.keySet()) try (InputStream in = jar.getInputStream(jar.getJarEntry(entry))) {
						actual.put(entry, hex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes())));
					}
				}
				return new Result(path.toAbsolutePath().toString(), actual, actual.equals(EXPECTED));
			}
		} catch (Exception failure) { actual.put("<error>", failure.getClass().getSimpleName() + ":" + failure.getMessage()); }
		return new Result("<not-found>", actual, false);
	}
	private static String hex(byte[] bytes){StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format("%02x",b&255));return out.toString();}
	static final class Result { final String jar; final Map<String,String> actual; final boolean matches; Result(String j,Map<String,String>a,boolean m){jar=j;actual=Map.copyOf(a);matches=m;} }
}
