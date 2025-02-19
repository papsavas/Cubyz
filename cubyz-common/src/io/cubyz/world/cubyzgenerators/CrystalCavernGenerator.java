package io.cubyz.world.cubyzgenerators;

import java.util.Random;

import io.cubyz.api.CubyzRegistries;
import io.cubyz.api.Resource;
import io.cubyz.blocks.Block;
import io.cubyz.blocks.Block.BlockClass;
import io.cubyz.math.CubyzMath;
import io.cubyz.world.Chunk;
import io.cubyz.world.NormalChunk;
import io.cubyz.world.Region;
import io.cubyz.world.Surface;

/**
 * Generates a special cavern that contains giant crystals.
 */

public class CrystalCavernGenerator implements Generator {
	
	private static Block glowCrystal, crystalOre;
	
	private static ThreadLocal<int[][]> crystalDataArray = new ThreadLocal<int[][]>() {
		@Override
		public int[][] initialValue() {
			return new int[2048][3]; // TODO: Properly evaluate the maximum number of crystal spawns per crystal cavern.
		}
	};
	
	public static void init(Block crystal, Block ore) {
		glowCrystal = crystal;
		crystalOre = ore;
	}
	
	
	@Override
	public Resource getRegistryID() {
		return new Resource("cubyz", "lifeland_crystal_cavern");
	}
	
	@Override
	public int getPriority() {
		return 65537; // Directly after normal caves.
	}
	
	private static final int range = 3;
	private Block water = CubyzRegistries.BLOCK_REGISTRY.getByID("cubyz:water");
	private Block ice = CubyzRegistries.BLOCK_REGISTRY.getByID("cubyz:ice");
	private Block stone = CubyzRegistries.BLOCK_REGISTRY.getByID("cubyz:stone");
	
	private static final int crystalChunkWorldSize = 256;

	@Override
	public void generate(long seed, int wx, int wy, int wz, Chunk chunk, Region containingRegion, Surface surface) {
		int ccx = wx/crystalChunkWorldSize;
		int ccy = wy/crystalChunkWorldSize;
		int ccz = wz/crystalChunkWorldSize;
		Random rand = new Random(seed);
		int rand1 = rand.nextInt() | 1;
		int rand2 = rand.nextInt() | 1;
		int rand3 = rand.nextInt() | 1;
		// Generate caves from all nearby chunks:
		for(int x = ccx - range; x <= ccx + range; ++x) {
			for(int y = ccy - range; y <= ccy + range; ++y) {
				for(int z = ccz - range; z <= ccz + range; ++z) {
					int randX = CubyzMath.worldModulo(x, surface.getSizeX()/crystalChunkWorldSize)*rand1;
					int randY = y*rand2;
					int randZ = CubyzMath.worldModulo(z, surface.getSizeZ()/crystalChunkWorldSize)*rand3;
					rand.setSeed((randY << 48) ^ (randY >>> 16) ^ (randX << 32) ^ randZ ^ seed);
					considerCoordinates(x, y, z, wx, wy, wz, chunk, rand);
				}
			}
		}
	}
	
	private void generateCave(long random, int wx, int wy, int wz, Chunk chunk, double worldX, double worldY, double worldZ, float size, float direction, float slope, int curStep, double caveHeightModifier, int[][] crystalSpawns, int[] index) {
		double cwx = (double) (wx + NormalChunk.chunkSize/2);
		double cwy = (double) (wy + NormalChunk.chunkSize/2);
		double cwz = (double) (wz + NormalChunk.chunkSize/2);
		float directionModifier = 0.0F;
		float slopeModifier = 0.0F;
		Random localRand = new Random(random);
		// Choose a random cave length if not specified:
		int local = (range - 1)*crystalChunkWorldSize;
		int caveLength = local - localRand.nextInt(local / 4);

		for(boolean highSlope = localRand.nextInt(6) == 0; curStep < caveLength; ++curStep) {
			double xzScale = 6.0 + Math.sin(curStep*Math.PI/caveLength)*size;
			double yScale = xzScale*caveHeightModifier;
			// Move cave center point one unit into a direction given by slope and direction:
			float xzUnit = (float)Math.cos(slope);
			float yUnit = (float)Math.sin(slope);
			double delX = Math.cos(direction) * xzUnit;
			double delZ = Math.sin(direction) * xzUnit;
			worldX += delX;
			worldY += yUnit;
			worldZ += Math.sin(direction)*xzUnit;

			if(highSlope) {
				slope *= 0.92F;
			} else {
				slope *= 0.7F;
			}

			slope += slopeModifier * 0.01F;
			direction += directionModifier * 0.01F;
			slopeModifier *= 0.9F;
			directionModifier *= 0.75F;
			slopeModifier += (localRand.nextFloat() - localRand.nextFloat())*localRand.nextFloat()*2;
			directionModifier += (localRand.nextFloat() - localRand.nextFloat())*localRand.nextFloat()*4;
			double deltaX = worldX - cwx;
			double deltaZ = worldZ - cwz;
			double stepsLeft = (double)(caveLength - curStep);
			double maxLength = (double)(size + 8);
			// Abort if the cave is getting to far away from this chunk:
			if(deltaX*deltaX + deltaZ*deltaZ - stepsLeft*stepsLeft > maxLength*maxLength) {
				return;
			}
			
			// Only care about it if it is inside the current chunk:
			if(worldX >= cwx - NormalChunk.chunkSize/2 - xzScale && worldZ >= cwz - NormalChunk.chunkSize/2 - xzScale && worldX <= cwx + NormalChunk.chunkSize/2 + xzScale && worldZ <= cwz + NormalChunk.chunkSize/2 + xzScale) {
				// Determine min and max of the current cave segment in all directions.
				int xMin = (int)(worldX - xzScale) - wx - 1;
				int xMax = (int)(worldX + xzScale) - wx + 1;
				int yMin = (int)(worldY - yScale) - wy - 1;
				int yMax = (int)(worldY + yScale) - wy + 1;
				int zMin = (int)(worldZ - xzScale) - wz - 1;
				int zMax = (int)(worldZ + xzScale) - wz + 1;
				if (xMin < 0)
					xMin = 0;
				if (xMax > NormalChunk.chunkSize)
					xMax = NormalChunk.chunkSize;
				if (yMin < 0)
					yMin = 0;
				if (yMax > NormalChunk.chunkSize)
					yMax = NormalChunk.chunkSize;
				if (zMin < 0)
					zMin = 0;
				if (zMax > NormalChunk.chunkSize)
					zMax = NormalChunk.chunkSize;
				// Go through all blocks within range of the cave center and remove them if they
				// are within range of the center.
				for(int curX = xMin; curX < xMax; ++curX) {
					double distToCenterX = ((double) (curX + wx) - worldX) / xzScale;
					
					for(int curZ = zMin; curZ < zMax; ++curZ) {
						double distToCenterZ = ((double) (curZ + wz) - worldZ) / xzScale;
						if(distToCenterX * distToCenterX + distToCenterZ * distToCenterZ < 1.0) {
							for(int curY = yMax - 1; curY >= yMin; --curY) {
								double distToCenterY = ((double) (curY + wy) - worldY) / (yScale);
								if(distToCenterX*distToCenterX + distToCenterY*distToCenterY + distToCenterZ*distToCenterZ < 1.0 && water != chunk.getBlock(curX, curY, curZ) && ice != chunk.getBlock(curX, curY, curZ)) {
									chunk.updateBlock(curX, curY, curZ, null);
								}
							}
						}
					}
				}
			}
			long seed = localRand.nextLong();
			// Only let crystals spawn when they are close enough to the chunk.
			if(worldX >= cwx - 32 - NormalChunk.chunkSize/2 && worldY >= cwy - 32 - NormalChunk.chunkSize/2 && worldZ >= cwz - 32 - NormalChunk.chunkSize/2 && worldX <= cwx + 32 + NormalChunk.chunkSize/2 && worldY <= cwy + 32 + NormalChunk.chunkSize/2 && worldZ <= cwz + 32 + NormalChunk.chunkSize/2) {
				// Consider a good amount of crystal spawns in the region.
				Random rand = new Random(seed);
				int amount = (int)(1+20*xzScale*yScale/size/size);
				for(int i = 0; i < amount; i++) {
					// Choose a random point on the surface of the surrounding spheroid to generate a crystal there:
					double theta = 2*Math.PI*rand.nextDouble();
			        double phi = Math.acos(1 - 2*rand.nextDouble());
			        double x = Math.sin(phi)*Math.cos(theta);
			        double y = Math.sin(phi)*Math.sin(theta);
			        double z = Math.cos(phi);
			        // Check if the crystal touches the wall:
			        if(Math.abs(delX*x+yUnit*y+delZ*z) < 0.05) {
				        crystalSpawns[index[0]++] = new int[] {(int)(worldX + x*xzScale), (int)(worldY + y*yScale), (int)(worldZ + z*xzScale)};
			        }
				}
			}
		}
	}
	
	private double distSqr(double x, double y, double z) {
		return x*x+y*y+z*z;
	}
	
	private void considerCrystal(int wx, int wy, int wz, int[] xyz, Chunk chunk, long seed, boolean useNeedles) {
		if(xyz[0] >= wx-32 && xyz[0] <= wx+32+NormalChunk.chunkSize && xyz[1] >= wy-32 && xyz[1] <= wy+32+NormalChunk.chunkSize && xyz[2] >= wz-32 && xyz[2] <= wz+32+NormalChunk.chunkSize) {
			int x = xyz[0] - wx;
			int y = xyz[1] - wy;
			int z = xyz[2] - wz;
			Random rand = new Random(seed);
			// Make some crystal spikes in random directions:
			int spikes = 4;
			if(useNeedles) spikes++;
			spikes += rand.nextInt(spikes); // Use somewhat between spikes and 2*spikes spikes.
			for(int i = 0; i < spikes; i++) {
				int length = rand.nextInt(24)+8;
				// Choose a random direction:
				double theta = 2*Math.PI*rand.nextDouble();
		        double phi = Math.acos(1 - 2*rand.nextDouble());
		        double delX = Math.sin(phi)*Math.cos(theta);
		        double delY = Math.sin(phi)*Math.sin(theta);
		        double delZ = Math.cos(phi);
		        for(double j = 0; j < length;) {
		        	double x2 = x + delX*j;
		        	double y2 = y + delY*j;
		        	double z2 = z + delZ*j;
		        	double size;
		        	if(useNeedles)
		        		size = 0.7;
		        	else
		        		size = 12*(length-j)/length/spikes;
		        	int xMin = (int)(x2-size);
		        	int xMax = (int)(x2+size);
		        	int yMin = (int)(y2-size);
		        	int yMax = (int)(y2+size);
		        	int zMin = (int)(z2-size);
		        	int zMax = (int)(z2+size);
		        	for(int x3 = xMin; x3 <= xMax; x3++) {
			        	for(int y3 = yMin; y3 <= yMax; y3++) {
				        	for(int z3 = zMin; z3 <= zMax; z3++) {
				        		double dist = distSqr(x3-x2, y3-y2, z3-z2);
				        		if(dist <= size*size) {
						        	if(x3 >= 0 && x3 < NormalChunk.chunkSize && y3 >= 0 && y3 < NormalChunk.chunkSize && z3 >= 0 && z3 < NormalChunk.chunkSize) {
						        		if(chunk.getBlock((int)x3, (int)y3, (int)z3) == null || chunk.getBlock((int)x3, (int)y3, (int)z3).isDegradable() || chunk.getBlock((int)x3, (int)y3, (int)z3).getBlockClass() == BlockClass.FLUID) {
						        			chunk.updateBlock((int)x3, (int)y3, (int)z3, glowCrystal);
						        		} else if(chunk.getBlock((int)x3, (int)y3, (int)z3) == stone) {
						        			chunk.updateBlock((int)x3, (int)y3, (int)z3, crystalOre); // When the crystal goes through stone, generate the corresponding ore at that position.
						        		}
						        	}
				        		}
				        	}
			        	}
		        	}
		        	if(size > 2) size = 2;
		        	j += size/2; // Make sure there are no crystal bits floating in the air.
		        	if(size < 0.5) break; // Also preventing floating crystal bits.
		        }
			}
		}
	}

	private void considerCoordinates(int x, int y, int z, int wx, int wy, int wz, Chunk chunk, Random rand) {
		if(rand.nextInt(16) != 0) return; // This should be pretty rare(mostly because it is so huge).
		// Choose some in world coordinates to start generating:
		double worldX = (x + rand.nextFloat())*crystalChunkWorldSize;
		double worldY = (y + rand.nextFloat())*crystalChunkWorldSize;
		if(worldY > -128) return; // crystal caverns not generate close to the surface!
		double worldZ = (z + rand.nextFloat())*crystalChunkWorldSize;
		float direction = rand.nextFloat()*(float)Math.PI*2.0F;
		float slope = (rand.nextFloat() - 0.5F)/4.0F;
		float size = rand.nextFloat()*20 + 20;
		int[][] crystalSpawns = crystalDataArray.get();
		int[] index = {0};
		long rand1 = rand.nextLong();
		long rand2 = rand.nextLong();
		long rand3 = rand.nextLong();
		boolean useNeedles = rand.nextBoolean(); // Different crystal type.
		generateCave(rand.nextLong(), wx, wy, wz, chunk, worldX, worldY, worldZ, size, direction, slope, 0, 0.75, crystalSpawns, index);

		// Generate the crystals:
		for(int i = 0; i < index[0]; i++) {
			considerCrystal(wx, wy, wz, crystalSpawns[i], chunk, crystalSpawns[i][0]*rand1 + crystalSpawns[i][1]*rand2 + crystalSpawns[i][2]*rand3, useNeedles);
		}
	}

	@Override
	public long getGeneratorSeed() {
		return 0x9b450ffb0d415317L;
	}
}
