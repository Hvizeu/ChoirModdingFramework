package choir.adapter.v71_44;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarFile;

/** Exact V71.44 fingerprints for every class currently owned by the storage replacement. */
final class V7144StorageTargetFingerprint {
	private static final Map<String, String> EXPECTED = expected();
	private V7144StorageTargetFingerprint() { }
	static int targetCount() { return EXPECTED.size(); }

	static Result verify() {
		try {
			for (String item : System.getProperty("java.class.path", "").split(java.io.File.pathSeparator)) {
				Path path = Paths.get(item);
				if (path.getFileName() == null || !path.getFileName().toString().equalsIgnoreCase("SongsOfSyx.jar")
						|| !Files.isRegularFile(path)) continue;
				try (JarFile jar = new JarFile(path.toFile())) {
					StringBuilder detail = new StringBuilder();
					boolean matches = true;
					for (Map.Entry<String, String> expected : EXPECTED.entrySet()) {
						if (jar.getJarEntry(expected.getKey()) == null) {
							matches = false; detail.append(expected.getKey()).append("=<missing>;"); continue;
						}
						try (InputStream in = jar.getInputStream(jar.getJarEntry(expected.getKey()))) {
							String actual = hex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
							if (!expected.getValue().equals(actual)) matches = false;
							detail.append(expected.getKey()).append('=').append(actual).append(';');
						}
					}
					return new Result(path.toAbsolutePath().toString(), matches, detail.toString());
				}
			}
		} catch (Exception failure) {
			return new Result("<error>", false, failure.getClass().getSimpleName() + ':' + failure.getMessage());
		}
		return new Result("<not-found>", false, "SongsOfSyx.jar unavailable");
	}

	private static Map<String, String> expected() {
		LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
		values.put("settlement/misc/util/RESOURCE_TILE.class", "3e14a044f906cd2bf7809136834c04bc6023939a0de5897b0bde20fa0f743045");
		values.put("settlement/misc/util/RESOURCE_TILE$Getter.class", "c0f411ade232bad4fe866be1115b2234a79f4e0b3a48598476d197677523cd4c");
		values.put("settlement/misc/util/RESOURCE_TILE$RESOURCE_TILE_HASER.class", "f793297c026efe16990046893fe453f77d9de612122669112e3d383198b4b370");
		values.put("settlement/misc/util/TILE_STORAGE.class", "5b232528fd4724c3de5eec0a8aa3d513de431a00e7253dc214f9bf1069d570d7");
		values.put("settlement/path/components/FindableDatas.class", "27b29a0aa48e380abded41b0840cdf5ce16e3f9d734dabf2e5308342258570f6");
		values.put("settlement/path/components/FindableDatas$1.class", "a42a746271a8857e97bc58a10784ef8bdfa88d2958af57d0927f8ec4c3156839");
		values.put("settlement/path/finders/SFinderResourceStorage.class", "70c35becd2c00054d15287182d5ed24c1f7412e0acb69b105301f52b92025e92");
		values.put("settlement/path/finders/SFinderResourceStorage$1.class", "1ae857f1ffe4c6a209a4056fc1a26f372a04ae0b0fe162189f3de649d3425311");
		values.put("settlement/path/finders/SFinderResourceStorage$2.class", "9c2888f6323faf6adcece2245d29e5de0c9acca211c132f84487e4383619320d");
		values.put("settlement/path/finders/SFinderResourceStore.class", "5ffd3d02b608aa37612c829e2474b19ee6c15cb2109692cf2305ec631c4d7bf1");
		values.put("settlement/path/finders/SFinderResourceStore$1.class", "9687b422da190d2121604d126a2fae4371efdeac29f4b08d6db4e4b9d2aee647");
		values.put("settlement/path/finders/SFinderResourceStore$2.class", "6a522d76d4cae26be9f45ba8e57b98ccc7e2ceb953dc8793d30398f5cfd53eb4");
		values.put("settlement/path/finders/SFinderResourceStore$3.class", "d3e84f4749dcddf37b563a1d27d97007a67633fd7c665700149460acced96ceb");
		values.put("settlement/path/finders/SFinderResourceStore$Updater.class", "17ece17bd20878d5762e2124edfd370660d7a661c0b80019ab3d8f93e05e3cf5");
		values.put("settlement/room/main/job/StorageCrate.class", "d66ba707614fc568c17651ff4936b59a31c661cb580499f8792622782a6e678b");
		values.put("settlement/room/main/job/StorageCrate$StorageData.class", "d875ae677b45e70ad88e4f6304d3f5784cab158576f663a6695fca889a9b7437");
		values.put("settlement/room/industry/workshop/Job.class", "0460de666491a1282c9799e3da2cbde983473ed3a80ba5cdc14fb90b3c3429c0");
		values.put("settlement/room/industry/workshop/Job$1.class", "7e4771a11e0763068df3eed7711a466d59f5b6ab74091d7f0579eda2b22a4660");
		values.put("settlement/room/industry/workshop/Job$2.class", "cd8eb91826e5e12ce69081d667fd7de38ac7fb1b8d2031becdd5671883198cfe");
		values.put("settlement/room/industry/workshop/WorkshopInstance.class", "a7e4366272f593e29f3de6274ad1ff01f8c4cd9eb1981b524fb3eac34d9acf74");
		values.put("settlement/room/industry/workshop/WorkshopInstance$Jobs.class", "58aefe5285612a00a4d589200ba72d55db7e8c40b86332cfecf37d156ee8091f");
		values.put("settlement/room/industry/refiner/Job.class", "ce1478b2d47b0156c48938d9d33a742e3133e67437f55c7f467b488037c47524");
		values.put("settlement/room/industry/refiner/Job$1.class", "7386af24c6cd9ec06315962049189bedd36ed326475eee9b466a794531e73166");
		values.put("settlement/room/industry/refiner/Job$2.class", "58dfb475e7a93a58ff2ea158d18d854cc23f7452e9306bc131e2f0383af44d3f");
		values.put("settlement/room/industry/refiner/RefinerInstance.class", "218176c7e749fef1b51e59f6e1c5bad1f9e84db74c2071a96a71d853306757a1");
		values.put("settlement/room/industry/refiner/RefinerInstance$Jobs.class", "2074b61c9e3146f2be570c813fb07d8aa92fc81b795bb80378ba938cacc071dd");
		values.put("settlement/room/infra/logistics/MoveJob.class", "cef6df2ae7540874d4df2f0d2fe61f144abc79cc9f8244abebf0e40497bfb910");
		values.put("settlement/room/infra/logistics/MoveJob$ROOM_MOVE_DEST.class", "0dae1651eee5a328dd52bcd586ce28c4c1a4e6ff5285380556cedc4909697bdf");
		values.put("settlement/room/infra/logistics/MoveJob$ROOM_MOVE_SOURCE.class", "206df17cc5cdc1c964f829c9e0d535711d379ed66214411295943c12f063bd2e");
		values.put("settlement/room/infra/logistics/MoveJob$ROOM_MOVEJOBBER.class", "b9568a97ca23971cea3d30141c5b57a107f964f2cfbb93a521d3875660c9f503");
		values.put("settlement/room/infra/logistics/MoveOrderPull.class", "aca0800c2f6a0197be1ff88d9563c1047c70bdf8d70ebd5be97ce6de83ff0b30");
		values.put("settlement/room/infra/logistics/MoveOrderPull$MoveOrderPullInstance.class", "a67e4483e3c3cb20c6345fc85842f5bde990118c7696f3a0d73fb9b46499885f");
		values.put("settlement/room/infra/logistics/MoveOrderPush.class", "60b2ecb421afa028d15c0036887f959bf1fb842a2dfdb640305c7b5241e47c1b");
		values.put("settlement/room/infra/logistics/MoveOrderPush$MoveOrderPushInstance.class", "969c58581863c558177bf37bf637ea35af33be0db10047590b23afba51ce9161");
		values.put("settlement/entity/humanoid/ai/work/PlanOddjobber$Crate.class", "157ad7fd6bcb90a938d850033be65f8e5e6deb2b3c08d9e2415714f0613d8915");
		values.put("settlement/entity/humanoid/ai/work/PlanOddjobber$Crate$1.class", "7cb5559452de01125ec4e23789630e18d3171c2fde730c4e453a6674e370c424");
		values.put("settlement/entity/humanoid/ai/work/PlanOddjobber$Crate$2.class", "bba9c8582ce679d4ac90bd53dd295480872bdaf2a6009565d4ef032a5daae580");
		values.put("settlement/entity/humanoid/ai/work/WorkDeliveryman.class", "706e5460cf7dc03b8e68b3f0ddf9708788f42669c2a78ff428dec0b6e6934bf0");
		values.put("settlement/entity/humanoid/ai/work/WorkDeliveryman$1.class", "03ea1bb53036f8872c83fa3283b8cd70d5dbc82f6c401d3140b800724184aec6");
		values.put("settlement/entity/humanoid/ai/work/WorkDeliveryman$2.class", "2f53142e9fedcb7d76b9791d5ea6e73a45ed1641422cc2a5613f0b2ce5e427fe");
		values.put("settlement/entity/humanoid/ai/work/WorkDeliveryman$3.class", "8d9b5dea2b3a1074c945827aeb7b0424ae829d871837a5499fe60f1a03c285ec");
		values.put("settlement/room/infra/stockpile/StockpileTally.class", "5acaade1d3b2b18d28ff09c9c26843116d4316b350b524cf3a8795c128e91387");
		values.put("settlement/room/infra/stockpile/StockpileTally$1.class", "70e853f17e723619dde1cac5723cd753134ee50a3f17de530f6a1a9c06fff3e2");
		values.put("settlement/room/infra/stockpile/StockpileTally$2.class", "ccd1dbfc159a545111e7e0a263309f56015b3adcb9dc0bcecc9362768ffa0dde");
		values.put("settlement/room/infra/stockpile/StockpileTally$3.class", "d72d9d327250d75eab1f406ea275bee0c1eb10823bf4ab2b630bea26149c8451");
		values.put("settlement/room/infra/stockpile/StockpileTally$4.class", "2058cfd9aa252865b633ab0c7225545a352f3757a96783f65fe3ba3e77e78527");
		values.put("settlement/room/infra/stockpile/StockpileTally$5.class", "0d069660a687b73a219e8b8aff67f22b30a0ece82a19dfbda123476157b403b0");
		values.put("settlement/room/infra/stockpile/StockpileTally$TallyData.class", "ebfec2a0b610353874307d69d228e52b88509ad7420d72d1edbe91c144d70f1d");
		values.put("settlement/room/infra/stockpile/Crate.class", "e07ee364ff8ecbb50945fba5810b320ad4c2aa39f7206e20124380dcc3972c40");
		values.put("settlement/room/infra/stockpile/Constructor.class", "47a63e3e8b5af681a99e130786a9be078b5781e9ebfa7527c9f3536726bcd35e");
		values.put("settlement/room/infra/stockpile/Constructor$1.class", "2cfc8f3730a714831fd8205ab2832c2c74cfc49970122b3eef212a0f0e5d0fc6");
		values.put("settlement/room/infra/stockpile/Constructor$2.class", "daeca7d3832fe61ea958518e4f373758f02fd5d93212873444a24dadf2c2eeff");
		values.put("settlement/room/infra/stockpile/StockpileInstance.class", "f672dee65f5a821508d41a13c4bd6ffe55c8afbe6da8eb57f57c98b8a51f0839");
		values.put("settlement/room/infra/stockpile/StockpileInstance$State.class", "7cad0c05d7ef27af0331df3f8240c4d004138bb116ff09907115713a95176428");
		return Map.copyOf(values);
	}

	private static String hex(byte[] bytes) {
		StringBuilder out = new StringBuilder();
		for (byte b : bytes) out.append(String.format("%02x", b & 255));
		return out.toString();
	}

	static final class Result {
		final String jar;
		final boolean matches;
		final String detail;
		Result(String jar, boolean matches, String detail) { this.jar = jar; this.matches = matches; this.detail = detail; }
	}
}
