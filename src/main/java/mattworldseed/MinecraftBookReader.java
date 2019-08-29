package mattworldseed;

import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.*;
import net.minecraft.world.level.chunk.storage.RegionFile;
import org.json.*;

import java.io.*;
import java.util.ArrayList;

public class MinecraftBookReader {

	static ArrayList<Integer> bookHashes = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		final String itemOrigin = args[0];
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));
		readBooksAnvil(itemOrigin, writer);
	}

	private static class LocationInfo {
		public final String itemOrigin, region, containerType;
		public final int cx, cz, bx, by, bz;

		private LocationInfo(String itemOrigin, String region, String containerType, int cx, int cz, int bx, int by, int bz) {
			this.itemOrigin = itemOrigin;
			this.region = region;
			this.containerType = containerType;
			this.cx = cx;
			this.cz = cz;
			this.bx = bx;
			this.by = by;
			this.bz = bz;
		}

		@Override
		public String toString() {
			return "itemOrigin[" + itemOrigin
					+ "].Region[" + region + "].Chunk[" + cx + "," + cz
					+ "].Block[" + bx + "," + by + "," + bz + "]."
					+ containerType;
		}
	}

	public static void readBooksAnvil(String itemOrigin, BufferedWriter writer) throws IOException {
		System.err.println("item_origin = " + itemOrigin);
		File folder = new File("region");
		File[] regionFiles = folder.listFiles();
		if (regionFiles == null) {
			System.err.println("Could not find " + folder);
			return;
		}
		System.err.println("Found " + regionFiles.length + " region files");

		final long startTime = System.currentTimeMillis();
		long prevProgressPrint = System.currentTimeMillis();

		for (int regionNr = 0; regionNr < regionFiles.length; regionNr++) {
			final File regionFile = regionFiles[regionNr];
			try {
				final long now = System.currentTimeMillis();
				if (prevProgressPrint + 3000 < now) {
					prevProgressPrint = now;
					final int progress = regionNr * 100 / regionFiles.length;
					final long msSoFar = now - startTime;
					final long remSecTotal = msSoFar * (regionFiles.length - regionNr) / (regionNr + 1) / 1000;
					final long remSec = remSecTotal % 60;
					final long remMin = (remSecTotal - remSec) / 60;
					System.err.println("Progress: " + progress + "% "
							+ regionNr + " / " + regionFiles.length + " regions"
							+ " after " + msSoFar / 1000 + "s"
							+ ", approx. " + remMin + ":" + (remSec < 10 ? "0" + remSec : remSec) + " remaining."
							+ " Found " + bookHashes.size() + " books so far.");
				}
			} catch (Exception ignored) {
			}
			try {
				final RegionFile region = new RegionFile(regionFile);
				writer.flush();
				for (int cx = 0; cx < 32; cx++) {
					for (int cz = 0; cz < 32; cz++) {
						try {
							DataInputStream dataInputStream = region.getChunkDataInputStream(cx, cz);

							if (dataInputStream == null)
								continue; // no chunk at these coords

							boolean littleEndian = false; // TODO check
							final CompoundTag regionTag = (CompoundTag) NBTIO.readTag(dataInputStream, littleEndian);
							CompoundTag levelTag = regionTag.get("Level");

							ListTag tileEntities = levelTag.get("TileEntities");
							for (int teNr = 0; teNr < tileEntities.size(); teNr++) {
								final CompoundTag tileEntity = tileEntities.get(teNr);
								ListTag chestItems = tileEntity.get("Items");
								if (chestItems != null) {
									try {
										final String tePath = "TileEntities[" + teNr + "]" + safeValue(tileEntity.get("id"));
										for (int n = 0; n < chestItems.size(); n++) {
											try {
												CompoundTag item = chestItems.get(n);
												parseItem(item, writer, new LocationInfo(
														itemOrigin,
														regionFile.getName(),
														tePath + ".Items[" + n + "]",
														cx, cz,
														Integer.parseInt(safeValue(tileEntity.get("x")).toString()),
														Integer.parseInt(safeValue(tileEntity.get("y")).toString()),
														Integer.parseInt(safeValue(tileEntity.get("z")).toString())
												));
											} catch (IOException e) {
												throw e;
											} catch (Exception e) {
												System.err.println("Error extracting books from TileEntities[" + teNr + "].Items[" + n + "] in " + regionFile.getName());
												e.printStackTrace();
											}
										}
									} catch (IOException e) {
										throw e;
									} catch (Exception e) {
										System.err.println("Error extracting books from TileEntities[" + teNr + "] in " + regionFile.getName());
										e.printStackTrace();
									}
								}
							}

							ListTag entities = levelTag.get("Entities");
							for (int eNr = 0; eNr < entities.size(); eNr++) {
								final CompoundTag entity = entities.get(eNr);

								final String ePath = "Entity[" + eNr + "]" + safeValue(entity.get("id"));
								ListTag entityPos = entity.get("Pos");
								int x = (int) Double.parseDouble(safeValue(entityPos.get(0)).toString());
								int y = (int) Double.parseDouble(safeValue(entityPos.get(1)).toString());
								int z = (int) Double.parseDouble(safeValue(entityPos.get(2)).toString());

								// Chest Cart, Donkey, llama etc.
								ListTag entityItems = entity.get("Items");
								if (entityItems != null) {
									try {
										for (int n = 0; n < entityItems.size(); n++) {
											try {
												CompoundTag item = entityItems.get(n);
												parseItem(item, writer, new LocationInfo(
														itemOrigin,
														regionFile.getName(),
														ePath + ".Items[" + n + "]",
														cx, cz, x, y, z
												));
											} catch (IOException e) {
												throw e;
											} catch (Exception e) {
												System.err.println("Error extracting books from Entities[" + eNr + ".Items[" + n + "] in " + regionFile.getName());
												e.printStackTrace();
											}
										}
									} catch (IOException e) {
										throw e;
									} catch (Exception e) {
										System.err.println("Error extracting books from Entities[" + eNr + "].Item in " + regionFile.getName());
										e.printStackTrace();
									}
								}

								// Item frame or item on the ground
								CompoundTag item = entity.get("Item");
								if (item != null) {
									try {
										parseItem(item, writer, new LocationInfo(
												itemOrigin,
												regionFile.getName(),
												ePath + ".Item",
												cx, cz, x, y, z
										));
									} catch (IOException e) {
										throw e;
									} catch (Exception e) {
										System.err.println("Error extracting books from " + ePath + ".Item in " + regionFile.getName());
										e.printStackTrace();
									}
								}
							}
						} catch (IOException e) {
							throw e;
						} catch (Exception e) {
							System.err.println("Error in chunk " + cx + "," + cz + " in region " + regionFile.getName());
							e.printStackTrace();
						}
					}
				}
			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				System.err.println("Error in region " + regionFile.getName());
				e.printStackTrace();
			}
		}
		writer.flush();
		long elapsed = System.currentTimeMillis() - startTime;
		System.err.println("Done. Took " + elapsed / 1000 + " seconds to complete."
				+ " Found " + bookHashes.size() + " books.");
	}

	public static void parseItem(CompoundTag item, BufferedWriter writer, LocationInfo locationInfo) throws IOException {
		final Object idVal = safeValue(item.get("id"));
		final int idInt = idVal instanceof Number ? ((Number) idVal).intValue() : -1;
		if ("minecraft:written_book".equals(idVal.toString()) || 387 == idInt) {
			readWrittenBook(item.get("tag"), writer, locationInfo);
		}
		if ("minecraft:writable_book".equals(idVal.toString()) || 386 == idInt) {
			readWritableBook(item.get("tag"), writer, locationInfo);
		}
		if (idVal.toString().contains("shulker_box")
				|| (idInt >= 219 && idInt <= 234)) {
			CompoundTag shulkerTag = item.get("tag");
			CompoundTag shulkerBETag = shulkerTag == null ? null : shulkerTag.get("BlockEntityTag");
			ListTag shulkerItems = shulkerBETag == null ? null : shulkerBETag.get("Items");
			if (shulkerItems != null) {
				for (int i = 0; i < shulkerItems.size(); i++) {
					try {
						CompoundTag shulkerItem = shulkerItems.get(i);
						parseItem(shulkerItem, writer, new LocationInfo(locationInfo.itemOrigin, locationInfo.region,
								locationInfo.containerType + ".Shulker.Items[" + i + "]",
								locationInfo.cx, locationInfo.cz,
								locationInfo.bx, locationInfo.by, locationInfo.bz
						));
					} catch (IOException e) {
						throw e;
					} catch (Exception e) {
						System.err.println("Error extracting books from "
								+ locationInfo.containerType + ".Shulker.Items[" + i + "]");
						e.printStackTrace();
					}
				}
			}
		}
	}

	private static void readWrittenBook(CompoundTag tag, BufferedWriter writer, LocationInfo locationInfo) throws IOException {
		if (tag == null) return;
		ListTag pages = tag.get("pages");
		if (pages == null || pages.size() == 0) {
			return;
		}
		if (bookHashes.contains(pages.hashCode())) {
			System.err.println("Skipping duplicate written book at " + locationInfo);
			return;
		}
		bookHashes.add(pages.hashCode());

		JSONArray pagesJson = pagesToTextArray(pages);

		final JSONObject bookJson = new JSONObject();
		bookJson.put("item_origin", locationInfo.itemOrigin);
		bookJson.put("entry_source_name", locationInfo.toString());
		bookJson.put("item_title", safeValue(tag.get("title")));
		bookJson.put("signee", safeValue(tag.get("author")));
		bookJson.put("generation", safeValue(tag.get("generation")));
		bookJson.put("pages", pagesJson);
		writer.write(bookJson.toString() + '\n');
	}

	private static void readWritableBook(CompoundTag tag, BufferedWriter writer, LocationInfo locationInfo) throws IOException {
		if (tag == null) return;
		ListTag pages = tag.get("pages");
		if (pages == null || pages.size() == 0) {
			return;
		}
		if (bookHashes.contains(pages.hashCode())) {
			System.err.println("Skipping duplicate writable book at " + locationInfo);
			return;
		}
		bookHashes.add(pages.hashCode());

		JSONArray pagesJson = pagesToTextArray(pages);

		final JSONObject bookJson = new JSONObject();
		bookJson.put("item_origin", locationInfo.itemOrigin);
		bookJson.put("entry_source_name", locationInfo.toString());
		bookJson.put("pages", pagesJson);
		writer.write(bookJson.toString() + '\n');
	}

	private static JSONArray pagesToTextArray(ListTag pages) {
		JSONArray pagesJson = new JSONArray();
		for (Tag page : pages) {
			pagesJson.put(safeValue(page).toString());
		}
		return pagesJson;
	}

	private static JSONArray pagesToJsonArray(ListTag pages) {
		JSONArray pagesJson = new JSONArray();
		for (Tag page : pages) {
			final String pageText = safeValue(page).toString();
			if (pageText.startsWith("{\"")) {
				try {
					JSONObject pageJson = new JSONObject(pageText);
					pagesJson.put(pageJson);
				} catch (JSONException e) {
					pagesJson.put(pageText);
				}
			}
		}
		return pagesJson;
	}

	private static Object safeValue(Tag tag) {
		if (tag == null) return null;
		return tag.getValue();
	}
}
