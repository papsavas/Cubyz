package io.cubyz.world;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;

import io.cubyz.Settings;
import io.cubyz.api.CubyzRegistries;
import io.cubyz.api.CurrentSurfaceRegistries;
import io.cubyz.blocks.Block;
import io.cubyz.blocks.CustomOre;
import io.cubyz.blocks.Updateable;
import io.cubyz.blocks.Ore;
import io.cubyz.blocks.BlockEntity;
import io.cubyz.entity.Entity;
import io.cubyz.entity.EntityType;
import io.cubyz.entity.ItemEntity;
import io.cubyz.handler.PlaceBlockHandler;
import io.cubyz.handler.RemoveBlockHandler;
import io.cubyz.items.BlockDrop;
import io.cubyz.items.ItemStack;
import io.cubyz.math.Bits;
import io.cubyz.math.CubyzMath;
import io.cubyz.save.BlockChange;
import io.cubyz.save.MissingBlockException;
import io.cubyz.save.TorusIO;
import io.cubyz.util.FastList;
import io.cubyz.world.cubyzgenerators.CrystalCavernGenerator;
import io.cubyz.world.cubyzgenerators.biomes.Biome;
import io.cubyz.world.generator.LifelandGenerator;
import io.cubyz.world.generator.SurfaceGenerator;

import static io.cubyz.CubyzLogger.logger;

public class LocalSurface extends Surface {
	private static Random rnd = new Random();
	
	private MetaChunk[] metaChunks;
	private NormalChunk[] chunks;
	private ReducedChunk[] reducedChunks;
	private int lastX = Integer.MAX_VALUE, lastZ = Integer.MAX_VALUE; // Chunk coordinates of the last chunk update.
	private int lastMetaX = Integer.MAX_VALUE, lastMetaZ = Integer.MAX_VALUE; // MetaChunk coordinates of the last chunk update.
	private int mcDRD; // double renderdistance of MetaChunks.
	private int doubleRD; // Corresponds to the doubled value of the last used render distance.
	private int worldSizeX = 65536, worldSizeZ = 16384;
	private ArrayList<Entity> entities = new ArrayList<>();
	
	private Block[] torusBlocks;
	
	// Stores a reference to the lists of WorldIO.
	public ArrayList<byte[]> blockData;
	public ArrayList<int[]> chunkData;
	
	private static int MAX_QUEUE_SIZE = 40;
	
	private SurfaceGenerator generator;
	
	private TorusIO tio;
	
	private List<ChunkGenerationThread> threads = new ArrayList<>();
	private boolean generated;
	
	float ambientLight = 0f;
	Vector4f clearColor = new Vector4f(0, 0, 0, 1.0f);
	
	private final long localSeed; // Each torus has a different seed for world generation. All those seeds are generated using the main world seed.
	
	// synchronized common list for chunk generation
	private volatile BlockingDeque<NormalChunk> loadList = new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);
	private volatile BlockingDeque<ReducedChunk> reducedLoadList = new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);
	
	boolean liquidUpdate;
	
	BlockEntity[] blockEntities = new BlockEntity[0];
	Integer[] liquids = new Integer[0];
	
	public CurrentSurfaceRegistries registries;

	private ArrayList<CustomOre> customOres = new ArrayList<>();
	
	private void queue(NormalChunk ch) {
		if (!isQueued(ch)) {
			try {
				loadList.put(ch);
			} catch (InterruptedException e) {
				System.err.println("Interrupted while queuing chunk. This is unexpected.");
			}
		}
	}
	
	private boolean isQueued(NormalChunk ch) {
		NormalChunk[] list = loadList.toArray(new NormalChunk[0]);
		for (NormalChunk ch2 : list) {
			if (ch2 == ch) {
				return true;
			}
		}
		return false;
	}
	
	private void queue(ReducedChunk ch) {
		if (!isQueued(ch)) {
			try {
				reducedLoadList.put(ch);
			} catch (InterruptedException e) {
				System.err.println("Interrupted while queuing chunk. This is unexpected.");
			}
		}
	}
	
	private boolean isQueued(ReducedChunk ch) {
		ReducedChunk[] list = reducedLoadList.toArray(new ReducedChunk[0]);
		for (ReducedChunk ch2 : list) {
			if (ch2 == ch) {
				return true;
			}
		}
		return false;
	}
	
	private class ChunkGenerationThread extends Thread {
		public void run() {
			while (true) {
				if(!loadList.isEmpty()) {
					NormalChunk popped = null;
					try {
						popped = loadList.take();
					} catch (InterruptedException e) {
						break;
					}
					try {
						synchronousGenerate(popped);
						popped.load();
					} catch (Exception e) {
						logger.severe("Could not generate chunk " + popped.getX() + ", " + popped.getZ() + " !");
						logger.throwable(e);
					}
				} else if(!reducedLoadList.isEmpty()) {
					ReducedChunk popped = null;
					try {
						popped = reducedLoadList.take();
					} catch (InterruptedException e) {
						break;
					}
					try {
						generator.generate(popped, LocalSurface.this);
						popped.generated = true;
					} catch (Exception e) {
						logger.severe("Could not generate reduced chunk " + popped.cx + ", " + popped.cz + " !");
						logger.throwable(e);
					}
				}
			}
		}
	}
	
	public LocalSurface(LocalStellarTorus torus) {
		registries = new CurrentSurfaceRegistries();
		localSeed = torus.getLocalSeed();
		this.torus = torus;
		metaChunks = new MetaChunk[0];
		chunks = new NormalChunk[0];
		reducedChunks = new ReducedChunk[0];
		
		for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
			ChunkGenerationThread thread = new ChunkGenerationThread();
			thread.setName("Local-Chunk-Thread-" + i);
			thread.setDaemon(true);
			thread.start();
			threads.add(thread);
		}
		generator = registries.worldGeneratorRegistry.getByID("cubyz:lifeland");
		if (generator instanceof LifelandGenerator) {
			((LifelandGenerator) generator).sortGenerators();
		}
		tio = new TorusIO(torus, new File("saves/" + torus.getWorld().getName() + "/" + localSeed)); // use seed in path
		if (tio.hasTorusData()) {
			generated = true;
		} else {
			tio.saveTorusData(this);
		}
		//setChunkQueueSize(torus.world.getRenderDistance() << 2);
	}
	
	public int generate(ArrayList<Block> blockList, ArrayList<Ore> ores, int ID) {
		Random rand = new Random(localSeed);
		int randomAmount = 9 + rand.nextInt(3); // TODO
		int i = 0;
		for(i = 0; i < randomAmount; i++) {
			CustomOre block = CustomOre.random(rand, registries);
			customOres.add(block);
			ores.add(block);
			blockList.add(block);
			block.ID = ID++;
			registries.blockRegistry.register(block);
		}
		
		// Create the crystal ore for the CrystalCaverns:
		CustomOre glowCrystalOre = CustomOre.random(rand, registries);
		glowCrystalOre.makeGlow(); // Make sure it glows.
		customOres.add(glowCrystalOre);
		ores.add(glowCrystalOre);
		blockList.add(glowCrystalOre);
		glowCrystalOre.ID = ID++;
		registries.blockRegistry.register(glowCrystalOre);
		i++;
		// Create the crystal block for the CrystalCaverns:
		CustomOre crystalBlock = new CustomOre(0, 0, 0); // TODO: Add a CustomBlock type or interface because this is no ore.
		crystalBlock.setID(glowCrystalOre.getRegistryID().toString()+"_glow_crystal");
		crystalBlock.setHardness(40);
		crystalBlock.addBlockDrop(new BlockDrop(glowCrystalOre.getBlockDrops()[0].item, 4));
		crystalBlock.setLight(glowCrystalOre.color);
		crystalBlock.color = glowCrystalOre.color;
		crystalBlock.seed = -1; // TODO: Fix crystal block within the new ore texture generation system.
		customOres.add(crystalBlock);
		ores.add(crystalBlock);
		blockList.add(crystalBlock);
		crystalBlock.ID = ID++;
		registries.blockRegistry.register(crystalBlock);
		i++;
		// Init crystal caverns with those two blocks:
		CrystalCavernGenerator.init(crystalBlock, glowCrystalOre);

		tio.link(this);
		tio.loadTorusData(this); // load data here in order for entities to also be loaded.
		
		if(generated) {
			tio.saveTorusData(this);
		}
		generated = true;
		torusBlocks = blockList.toArray(new Block[0]);
		return ID;
	}
	
	public void setChunkQueueSize(int size) {
		synchronized (loadList) {
			loadList.clear();
			MAX_QUEUE_SIZE = size;
			loadList = new LinkedBlockingDeque<>(size);
		}
		System.out.println("max queue size is now " + size);
	}

	
	public void forceSave() {
		tio.saveTorusData(this);
		((LocalWorld) torus.getWorld()).forceSave();
	}
	
	public void addEntity(Entity ent) {
		entities.add(ent);
	}
	
	public void removeEntity(Entity ent) {
		entities.remove(ent);
	}
	
	public void setEntities(Entity[] arr) {
		entities = new ArrayList<>(arr.length);
		for (Entity e : arr) {
			entities.add(e);
		}
	}
	
	@Override
	public void synchronousSeek(int x, int z, int renderDistance) {
		// Transform to chunk coordinates:
		x >>= 4;
		z >>= 4;
		NormalChunk ch = getChunk(x, z);
		if(ch == null) {
			ch = new NormalChunk(x, z, this, transformData(getChunkData(x, z), tio.blockPalette));
			NormalChunk[] newList = new NormalChunk[chunks.length+1];
			newList[chunks.length] = ch;
			chunks = newList;
		}
		if (!ch.isGenerated()) { // TODO actually fix synchronousSeek so we can access blocks generated through it
			synchronousGenerate(ch);
			ch.load();
			int local = x & 15;
			x += renderDistance;
			if(local > 7)
				x++;
			local = z & 15;
			z += renderDistance;
			if(local > 7)
				z++;
			lastX = x;
			lastZ = z;
			doubleRD = renderDistance << 1;
		}
	}
	
	public void synchronousGenerate(NormalChunk ch) {
		ch.generateFrom(generator);
		tio.saveChunk(ch);
	}
	
	public ArrayList<BlockChange> transformData(byte[] data, Map<Block, Integer> blockPalette) {
		int size = Bits.getInt(data, 8);
		ArrayList<BlockChange> list = new ArrayList<BlockChange>(size);
		for (int i = 0; i < size; i++) {
			try {
				list.add(new BlockChange(data, 12 + i*17, blockPalette));
			} catch (MissingBlockException e) {
				// If the block is missing, we replace it by nothing
				int off = 12 + i*17;
				int x = Bits.getInt(data, off + 0);
				int y = Bits.getInt(data, off + 4);
				int z = Bits.getInt(data, off + 8);
				list.add(new BlockChange(-2, -1, x, y, z, (byte)0, (byte)0));
			}
		}
		return list;
	}
	
	private EntityType itemEntityType = CubyzRegistries.ENTITY_REGISTRY.getByID("cubyz:item_stack");
	
	@Override
	public void removeBlock(int x, int y, int z) {
		NormalChunk ch = getChunk(x >> 4, z >> 4);
		if (ch != null) {
			Block b = ch.getBlockAt(x & 15, y, z & 15);
			ch.removeBlockAt(x & 15, y, z & 15, true);
			tio.saveChunk(ch); // TODO: Don't save it every time.
			tio.saveTorusData(this);
			for (RemoveBlockHandler hand : removeBlockHandlers) {
				hand.onBlockRemoved(b, x, y, z);
			}
			// Fetch block drops:
			for(BlockDrop drop : b.getBlockDrops()) {
				int amount = (int)(drop.amount);
				float randomPart = drop.amount - amount;
				if(Math.random() < randomPart) amount++;
				if(amount > 0) {
					ItemEntity ent = new ItemEntity(itemEntityType, this, new ItemStack(drop.item, amount), new Vector3i(x, y, z));
					entities.add(ent);
				}
			}
		}
	}
	
	@Override
	public void placeBlock(int x, int y, int z, Block b, byte data) {
		NormalChunk ch = getChunk(x >> 4, z >> 4);
		if (ch != null) {
			ch.addBlock(b, data, x & 15, y, z & 15, false);
			tio.saveChunk(ch); // TODO: Don't save it every time.
			tio.saveTorusData(this);
			for (PlaceBlockHandler hand : placeBlockHandlers) {
				hand.onBlockPlaced(b, x, y, z);
			}
		}
	}
	
	@Override
	public void drop(ItemStack stack, Vector3f pos) {
		ItemEntity drop = new ItemEntity(itemEntityType, this, stack, pos);
		entities.add(drop);
	}
	
	@Override
	public void updateBlockData(int x, int y, int z, byte data) {
		NormalChunk ch = getChunk(x >> 4, z >> 4);
		if (ch != null) {
			ch.setBlockData(x & 15, y, z & 15, data);
			tio.saveChunk(ch); // TODO: Don't save it every time.
			tio.saveTorusData(this);
		}
	}
	
	public void update() {
		long gameTime = torus.world.getGameTime();
		int dayCycle = torus.getDayCycle();
		LocalWorld world = (LocalWorld) torus.getWorld();
		// Ambient light
		{
			int dayTime = Math.abs((int)(gameTime % dayCycle) - (dayCycle >> 1));
			if(dayTime < (dayCycle >> 2)-(dayCycle >> 4)) {
				ambientLight = 0.1f;
				clearColor.x = clearColor.y = clearColor.z = 0;
			} else if(dayTime > (dayCycle >> 2)+(dayCycle >> 4)) {
				ambientLight = 0.7f;
				clearColor.x = clearColor.y = 0.8f;
				clearColor.z = 1.0f;
			} else {
				//b:
				if(dayTime > (dayCycle >> 2)) {
					clearColor.z = 1.0f*(dayTime-(dayCycle >> 2))/(dayCycle >> 4);
				} else {
					clearColor.z = 0.0f;
				}
				//g:
				if(dayTime > (dayCycle >> 2)+(dayCycle >> 5)) {
					clearColor.y = 0.8f;
				} else if(dayTime > (dayCycle >> 2)-(dayCycle >> 5)) {
					clearColor.y = 0.8f+0.8f*(dayTime-(dayCycle >> 2)-(dayCycle >> 5))/(dayCycle >> 4);
				} else {
					clearColor.y = 0.0f;
				}
				//r:
				if(dayTime > (dayCycle >> 2)) {
					clearColor.x = 0.8f;
				} else {
					clearColor.x = 0.8f+0.8f*(dayTime-(dayCycle >> 2))/(dayCycle >> 4);
				}
				dayTime -= (dayCycle >> 2);
				dayTime <<= 3;
				ambientLight = 0.4f + 0.3f*dayTime/(dayCycle >> 1);
			}
		}
		// Entities
		for (int i = 0; i < entities.size(); i++) {
			Entity en = entities.get(i);
			en.update();
			if(en instanceof ItemEntity) {
				ItemEntity itemEn = (ItemEntity)en;
				// Check if a player or other entity is nearby that can pickup this item:
				for (int j = 0; j < entities.size(); j++) {
					Entity en2 = entities.get(j);
					// Every entity with and inventory can pick up stuff.
					if(en2.getInventory() != null) {
						if(en.getPosition().distance(en2.getPosition()) <= en2.pickupRange) {
							int newAmount = en2.getInventory().addItem(itemEn.items.getItem(), itemEn.items.getAmount());
							if(newAmount != 0) {
								itemEn.items.setAmount(newAmount);
							} else {
								entities.remove(en);
								break;
							}
						}
					}
				}
			}
		}
		// Block Entities
		for (NormalChunk ch : chunks) {
			if (ch.isLoaded() && ch.getBlockEntities().size() > 0) {
				blockEntities = ch.getBlockEntities().toArray(blockEntities);
				for (BlockEntity be : blockEntities) {
					if (be == null) break; // end of array
					if (be instanceof Updateable) {
						Updateable tk = (Updateable) be;
						tk.update(false);
						if (tk.randomUpdates()) {
							if (rnd.nextInt(5) < 1) { // 1/5 chance
								tk.update(true);
							}
						}
					}
				}
			}
		}
		
		// Liquids
		if (gameTime % 3 == 0 && world.inLqdUpdate) {
			world.inLqdUpdate = false;
			//Profiler.startProfiling();
			for (NormalChunk ch : chunks) {
				int wx = ch.getX() << 4;
				int wz = ch.getZ() << 4;
				if (ch.isLoaded() && ch.getLiquids().size() > 0) {
					liquids = ch.getUpdatingLiquids().toArray(liquids);
					int size = ch.getUpdatingLiquids().size();
					ch.getUpdatingLiquids().clear();
					for (int j = 0; j < size; j++) {
						Block block = ch.getBlockAtIndex(liquids[j]);
						int bx = (liquids[j] >> 4) & 15;
						int by = liquids[j] >> 8;
						int bz = liquids[j] & 15;
						Block[] neighbors = ch.getNeighbors(bx, by, bz);
						for (int i = 0; i < 5; i++) {
							Block b = neighbors[i];
							if (b == null) {
								int dx = 0, dy = 0, dz = 0;
								switch (i) {
									case 0: // at x -1
										dx = -1;
									break;
									case 1: // at x +1
										dx = 1;
										break;
									case 2:  // at z -1
										dz = -1;
										break;
									case 3: // at z +1
										dz = 1;
										break;
									case 4: // at y -1
										dy = -1;
										break;
									default:
										System.err.println("(LocalWorld/Liquids) More than 6 nullable neighbors!");
										break;
								}
								if(dy == -1 || (neighbors[4] != null && neighbors[4].getBlockClass() != Block.BlockClass.FLUID)) {
									ch.addBlockPossiblyOutside(block, (byte)0, CubyzMath.worldModulo(wx+bx+dx, worldSizeX), by+dy, CubyzMath.worldModulo(wz+bz+dz, worldSizeZ), true);
								}
							}
						}
					}
				}
			}
			//Profiler.printProfileTime("liquid-update");
		}
	}

	@Override
	public void queueChunk(NormalChunk ch) {
		queue(ch);
	}
	

	public void unQueueChunk(NormalChunk ch) {
		loadList.remove(ch);
	}

	public void queueChunk(ReducedChunk ch) {
		queue(ch);
	}

	public void unQueueChunk(ReducedChunk ch) {
		reducedLoadList.remove(ch);
	}
	
	@Override
	public void seek(int x, int z, int renderDistance, int maxResolution, float farDistanceFactor) {
		int xOld = x;
		int zOld = z;
		// Care about the MetaChunks:
		int mcRD = (renderDistance*10 >>> 4) + 4;
		int local = x & 255;
		x >>= 8;
		x += mcRD;
		if(local > 127)
			x++;
		local = z & 255;
		z >>= 8;
		z += mcRD;
		if(local > 127)
			z++;
		if(x != lastMetaX || z != lastMetaZ) {
			int mcDRD = mcRD << 1;
			MetaChunk[] newMeta = new MetaChunk[mcDRD*mcDRD];
			int index = 0;
			int minK = 0;
			for(int i = x-mcDRD; i < x; i++) {
				loop:
				for(int j = z-mcDRD; j < z; j++) {
					for(int k = minK; k < metaChunks.length; k++) {
						if(metaChunks[k] != null && CubyzMath.moduloMatchSign(metaChunks[k].x-i, worldSizeX >> 8) == 0 && CubyzMath.moduloMatchSign(metaChunks[k].z-j, worldSizeZ >> 8) == 0) {
							newMeta[index] = metaChunks[k];
							// Removes this chunk out of the list of chunks that will be considered in this function.
							metaChunks[k] = metaChunks[minK];
							metaChunks[minK] = newMeta[index];
							minK++;
							index++;
							continue loop;
						}
					}
					newMeta[index] = null;
					index++;
				}
			}
			metaChunks = newMeta;
			lastMetaX = x;
			lastMetaZ = z;
			this.mcDRD = mcDRD;
		}

		// Care about the Chunks:
		x = xOld;
		z = zOld;
		local = x & 15;
		x >>= 4;
		x += renderDistance;
		if(local > 7)
			x++;
		local = z & 15;
		z >>= 4;
		z += renderDistance;
		if(local > 7)
			z++;
		if(x == lastX && z == lastZ)
			return;
		int doubleRD = renderDistance << 1;
		NormalChunk [] newVisibles = new NormalChunk[doubleRD*doubleRD];
		int index = 0;
		int minK = 0;
		ArrayList<NormalChunk> chunksToQueue = new ArrayList<>();
		for(int i = x-doubleRD; i < x; i++) {
			loop:
			for(int j = z-doubleRD; j < z; j++) {
				for(int k = minK; k < chunks.length; k++) {
					if(CubyzMath.moduloMatchSign(chunks[k].getX()-i, worldSizeX >> 4) == 0 && CubyzMath.moduloMatchSign(chunks[k].getZ()-j, worldSizeZ >> 4) == 0) {
						newVisibles[index] = chunks[k];
						// Removes this chunk out of the list of chunks that will be considered in this function.
						chunks[k] = chunks[minK];
						chunks[minK] = newVisibles[index];
						minK++;
						index++;
						continue loop;
					}
				}
				NormalChunk ch = new NormalChunk(i, j, this, transformData(getChunkData(i, j), tio.blockPalette));
				chunksToQueue.add(ch);
				newVisibles[index] = ch;
				index++;
			}
		}
		for (int k = minK; k < chunks.length; k++) {
			if(chunks[k].isGenerated())
				tio.saveChunk(chunks[k]); // Only needs to be stored if it was ever generated.
			else
				unQueueChunk(chunks[k]);
		}
		chunks = newVisibles;
		lastX = x;
		lastZ = z;
		this.doubleRD = doubleRD;
		
		// Care about the ReducedChunks:
		// Load chunks after they have access to their neighbors:
		for(NormalChunk ch : chunksToQueue) {
			queueChunk(ch);
		}
		if (minK != chunks.length) { // if at least one chunk got unloaded
			tio.saveTorusData(this);
		}
		
		generateReducedChunks(xOld, zOld, (int)(renderDistance*farDistanceFactor), maxResolution);
	}
	
	public void generateReducedChunks(int x, int z, int renderDistance, int maxResolution) {
		int local = x & 15;
		x >>= 4;
		if(local > 7)
			x++;
		local = z & 15;
		z >>= 4;
		if(local > 7)
			z++;
		// Go through all resolutions:
		FastList<ReducedChunk> newReduced = new FastList<ReducedChunk>(reducedChunks.length, ReducedChunk.class);
		int minXLast = x - doubleRD/2;
		int maxXLast = x + doubleRD/2;
		int minZLast = z - doubleRD/2;
		int maxZLast = z + doubleRD/2;
		renderDistance &= ~1; // Make sure the render distance is a multiple of 2, so the chunks are always placed correctly.
		int minK = 0;
		int widthShift = 4;
		ArrayList<ReducedChunk> reducedChunksToQueue = new ArrayList<>();
		for(int res = 1; res <= maxResolution; res++) {
			int widthShiftOld = widthShift;
			int widthOld = 1 << (widthShift - 4);
			widthShift++;
			int widthNew = 1 << (widthShift - 4);
			int widthMask = widthNew - 1;
			// Pad between the different chunk sizes:
			int minXNew = minXLast;
			int maxXNew = maxXLast;
			int minZNew = minZLast;
			int maxZNew = maxZLast;
			if((minXNew & widthMask) != 0) minXNew -= widthOld;
			if((maxXNew & widthMask) != 0) maxXNew += widthOld;
			if((minZNew & widthMask) != 0) minZNew -= widthOld;
			if((maxZNew & widthMask) != 0) maxZNew += widthOld;
			for(int cx = minXNew; cx < maxXNew; cx += widthOld) {
				loop:
				for(int cz = minZNew; cz < maxZNew; cz += widthOld) {
					boolean visible = cx < minXLast || cx >= maxXLast || cz < minZLast || cz >= maxZLast;
					if(!visible) continue;
					for(int k = minK; k < reducedChunks.length; k++) {
						if(reducedChunks[k].resolutionShift == res && reducedChunks[k].widthShift == widthShiftOld && CubyzMath.moduloMatchSign(reducedChunks[k].cx-cx, worldSizeX >> 4) == 0 && CubyzMath.moduloMatchSign(reducedChunks[k].cz-cz, worldSizeZ >> 4) == 0) {
							newReduced.add(reducedChunks[k]);
							// Removes this chunk out of the list of chunks that will be considered in this function.
							reducedChunks[k] = reducedChunks[minK];
							reducedChunks[minK] = newReduced.array[newReduced.size - 1];
							minK++;
							continue loop;
						}
					}
					ReducedChunk ch = new ReducedChunk(cx, cz, res, widthShiftOld, transformData(getChunkData(cx, cz), tio.blockPalette));
					reducedChunksToQueue.add(ch);
					newReduced.add(ch);
					
				}
			}
			// Now add the real chunks:
			minXLast = minXNew;
			maxXLast = maxXNew;
			minZLast = minZNew;
			maxZLast = maxZNew;
			minXNew = minXLast - renderDistance;
			maxXNew = maxXLast + renderDistance;
			minZNew = minZLast - renderDistance;
			maxZNew = maxZLast + renderDistance;
			for(int cx = minXNew; cx < maxXNew; cx += widthNew) {
				loop:
				for(int cz = minZNew; cz < maxZNew; cz += widthNew) {
					boolean visible = cx < minXLast || cx >= maxXLast || cz < minZLast || cz >= maxZLast;
					if(!visible) continue;
					for(int k = minK; k < reducedChunks.length; k++) {
						if(reducedChunks[k].resolutionShift == res && reducedChunks[k].widthShift == widthShift && CubyzMath.moduloMatchSign(reducedChunks[k].cx-cx, worldSizeX >> 4) == 0 && CubyzMath.moduloMatchSign(reducedChunks[k].cz-cz, worldSizeZ >> 4) == 0) {
							newReduced.add(reducedChunks[k]);
							// Removes this chunk out of the list of chunks that will be considered in this function.
							reducedChunks[k] = reducedChunks[minK];
							reducedChunks[minK] = newReduced.array[newReduced.size - 1];
							minK++;
							continue loop;
						}
					}
					ReducedChunk ch = new ReducedChunk(cx, cz, res, widthShift, transformData(getChunkData(cx, cz), tio.blockPalette));
					reducedChunksToQueue.add(ch);
					newReduced.add(ch);
					
				}
			}
			minXLast = minXNew;
			maxXLast = maxXNew;
			minZLast = minZNew;
			maxZLast = maxZNew;
			renderDistance *= 2;
		}
		for (int k = minK; k < reducedChunks.length; k++) {
			unQueueChunk(reducedChunks[k]);
			//ClientOnly.deleteChunkMesh.accept(reducedChunks[k]);
		}
		newReduced.trimToSize();
		reducedChunks = newReduced.array;
		// Load chunks after they have access to their neighbors:
		for(ReducedChunk ch : reducedChunksToQueue) {
			queueChunk(ch);
		}
	}
	
	public void setBlocks(Block[] blocks) {
		torusBlocks = blocks;
	}
	
	public void getMapData(int x, int z, int width, int height, float [][] heightMap, float[][] heatMap, Biome[][] biomeMap) {
		int x0 = x&(~255);
		int z0 = z&(~255);
		for(int px = x0; CubyzMath.moduloMatchSign(px - x, worldSizeX) < width; px += 256) {
			for(int pz = z0; CubyzMath.moduloMatchSign(pz-z, worldSizeZ) < height; pz += 256) {
				MetaChunk ch = getMetaChunk(CubyzMath.worldModulo(px, worldSizeX), CubyzMath.worldModulo(pz, worldSizeZ));
				int xS = Math.max(px-x, 0);
				int zS = Math.max(pz-z, 0);
				int xE = Math.min(px + 256 - x, width);
				int zE = Math.min(pz + 256 - z, height);
				for(int cx = xS; cx < xE; cx++) {
					for(int cz = zS; cz < zE; cz++) {
						heightMap[cx][cz] = ch.heightMap[(cx + x) & 255][(cz + z) & 255];
						heatMap[cx][cz] = ch.heatMap[(cx + x) & 255][(cz + z) & 255];
						biomeMap[cx][cz] = ch.biomeMap[(cx + x) & 255][(cz + z) & 255];
					}
				}
			}
		}
	}

	
	@Override
	public MetaChunk getMetaChunk(int wx, int wz) {
		int x = wx >> 8;
		int z = wz >> 8;
		// Test if the chunk can be found in the list of visible chunks:
		int index = CubyzMath.moduloMatchSign(x-(lastMetaX-mcDRD), worldSizeX >> 8)*mcDRD + CubyzMath.moduloMatchSign(z-(lastMetaZ-mcDRD), worldSizeZ >> 8);
		wx = CubyzMath.worldModulo(wx, worldSizeX);
		wz = CubyzMath.worldModulo(wz, worldSizeZ);
		if(index < metaChunks.length && index >= 0) {
			MetaChunk ret = metaChunks[index];
			
			if (ret != null) {
				if(wx == ret.x && wz == ret.z)
					return ret;
			} else {
				MetaChunk ch = new MetaChunk(wx, wz, localSeed, this, registries);
				metaChunks[index] = ch;
				return ch;
			}
		}
		return new MetaChunk(wx, wz, localSeed, this, registries);
	}
	
	public MetaChunk getNoGenerateMetaChunk(int wx, int wy) {
		for(MetaChunk ch : metaChunks) {
			if(ch.x == wx && ch.z == wy) {
				return ch;
			}
		}
		return null;
	}
	
	@Override
	public NormalChunk getChunk(int x, int z) {
		// Test if the chunk can be found in the list of visible chunks:
		int index = CubyzMath.moduloMatchSign(x-(lastX-doubleRD), worldSizeX >> 4)*doubleRD + CubyzMath.moduloMatchSign(z-(lastZ-doubleRD), worldSizeZ >> 4);
		x = CubyzMath.worldModulo(x, worldSizeX >> 4);
		z = CubyzMath.worldModulo(z, worldSizeZ >> 4);
		if(index < chunks.length && index >= 0) {
			NormalChunk ret = chunks[index];
			if (ret != null && x == ret.getX() && z == ret.getZ())
				return ret;
		}
		return null;
	}

	@Override
	public Block getBlock(int x, int y, int z) {
		if (y > World.WORLD_HEIGHT || y < 0)
			return null;

		NormalChunk ch = getChunk(x >> 4, z >> 4);
		if (ch != null && ch.isGenerated()) {
			Block b = ch.getBlockAt(x & 15, y, z & 15);
			return b;
		} else {
			return null;
		}
	}
	
	@Override
	public byte getBlockData(int x, int y, int z) {
		if (y > World.WORLD_HEIGHT || y < 0)
			return 0;

		NormalChunk ch = getChunk(x >> 4, z >> 4);
		if (ch != null && ch.isGenerated()) {
			return ch.getBlockData(x & 15, y, z & 15);
		} else {
			return 0;
		}
	}
	
	public byte[] getChunkData(int x, int z) { // Gets the data of a Chunk.
		int index = -1;
		for(int i = 0; i < chunkData.size(); i++) {
			int [] arr = chunkData.get(i);
			if(arr[0] == x && arr[1] == z) {
				index = i;
				break;
			}
		}
		if(index == -1) {
			byte[] dummy = new byte[12];
			Bits.putInt(dummy, 0, x);
			Bits.putInt(dummy, 4, z);
			Bits.putInt(dummy, 8, 0);
			return dummy;
		}
		return blockData.get(index);
	}
	
	public long getSeed() {
		return localSeed;
	}
	
	public float getGlobalLighting() {
		return ambientLight;
	}
	
	public int getChunkQueueSize() {
		return loadList.size();
	}

	@Override
	public Vector4f getClearColor() {
		return clearColor;
	}

	@Override
	public BlockEntity getBlockEntity(int x, int y, int z) {
		/*BlockInstance bi = getBlockInstance(x, y, z);
		Chunk ck = _getNoGenerateChunk(bi.getX() >> 4, bi.getZ() >> 4);
		return ck.blockEntities().get(bi);*/
		return null; // TODO: Work on BlockEntities!
	}
	
	@Override
	public int getSizeX() {
		return worldSizeX;
	}
	
	@Override
	public int getSizeZ() {
		return worldSizeZ;
	}
	
	public ArrayList<CustomOre> getCustomOres() {
		return customOres;
	}

	@Override
	public NormalChunk[] getChunks() {
		return chunks;
	}

	@Override
	public Block[] getPlanetBlocks() {
		return torusBlocks;
	}
	
	@Override
	public Entity[] getEntities() {
		return entities.toArray(new Entity[entities.size()]);
	}
	
	public int getHeight(int x, int z) {
		return (int)(getMetaChunk(x & ~255, z & ~255).heightMap[x & 255][z & 255]);
	}

	@Override
	public void cleanup() {
		// Be sure to dereference and finalize the maximum of things
		try {
			forceSave();
			
			for (Thread thread : threads) {
				thread.interrupt();
				thread.join();
			}
			threads = new ArrayList<>();
			
			chunks = null;
			chunkData = null;
			blockData = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public CurrentSurfaceRegistries getCurrentRegistries() {
		return registries;
	}

	@Override
	public Biome getBiome(int x, int z) {
		MetaChunk mc = getMetaChunk(x & ~255, z & ~255);
		return mc.biomeMap[x & 255][z & 255];
	}

	@Override
	public Vector3f getLight(int x, int y, int z, Vector3f sunLight) {
		NormalChunk ch = getChunk(x >> 4, z >> 4);
		if(ch == null || !ch.isLoaded() || !Settings.easyLighting)
			return new Vector3f(1, 1, 1);
		int light = ch.getLight(x & 15, y, z & 15);
		int sun = (light >>> 24) & 255;
		int r = (light >>> 16) & 255;
		int g = (light >>> 8) & 255;
		int b = (light >>> 0) & 255;
		if(sun*sunLight.x > r) r = (int)(sun*sunLight.x);
		if(sun*sunLight.y > g) g = (int)(sun*sunLight.y);
		if(sun*sunLight.z > b) b = (int)(sun*sunLight.z);
		return new Vector3f(r/255.0f, g/255.0f, b/255.0f);
	}

	@Override
	public ReducedChunk[] getReducedChunks() {
		return reducedChunks;
	}
}
