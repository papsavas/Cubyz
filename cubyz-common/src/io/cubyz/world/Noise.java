package io.cubyz.world;

import java.util.Random;

import io.cubyz.math.CubyzMath;

/**
 * Fractal Noise Generator for world generation.
 * @author IntegratedQuantum
 */

public class Noise {
	private static long getSeed(int x, int y, int offsetX, int offsetY, int worldSizeX, int worldSizeY, long seed, int scale, int maxResolution) {
		Random rand = new Random(seed*(scale | 1));
		long l1 = rand.nextLong() | 1;
		long l2 = rand.nextLong() | 1;
		return (((long)(CubyzMath.worldModulo((offsetX + x)*maxResolution, worldSizeX)))*l1) ^ seed ^ (((long)(CubyzMath.worldModulo((offsetY + y)*maxResolution, worldSizeY)))*l2);
	}
	public static void generateFractalTerrain(int wx, int wy, int x0, int y0, int width, int height, int scale, long seed, int worldSizeX, int worldSizeZ, float[][] map, int maxResolution) {
		int max =scale+1;
		int and = scale-1;
		float[][] bigMap = new float[max][max];
		int offsetX = wx&(~and);
		int offsetY = wy&(~and);
		Random rand = new Random();
		// Generate the 4 corner points of this map using a coordinate-depending seed:
		rand.setSeed(getSeed(0, 0, offsetX, offsetY, worldSizeX, worldSizeZ, seed, scale, maxResolution));
		bigMap[0][0] = rand.nextFloat();
		rand.setSeed(getSeed(0, scale, offsetX, offsetY, worldSizeX, worldSizeZ, seed, scale, maxResolution));
		bigMap[0][scale] = rand.nextFloat();
		rand.setSeed(getSeed(scale, 0, offsetX, offsetY, worldSizeX, worldSizeZ, seed, scale, maxResolution));
		bigMap[scale][0] = rand.nextFloat();
		rand.setSeed(getSeed(scale, scale, offsetX, offsetY, worldSizeX, worldSizeZ, seed, scale, maxResolution));
		bigMap[scale][scale] = rand.nextFloat();
		generateInitializedFractalTerrain(offsetX, offsetY, scale, scale, seed, worldSizeX, worldSizeZ, bigMap, 0, 1, maxResolution);
		for(int px = 0; px < width; px++) {
			for(int py = 0; py < height; py++) {
				map[x0 + px][y0 + py] = bigMap[(wx&and)+px][(wy&and)+py];
				if(map[x0 + px][y0 + py] >= 1.0f) {
					map[x0 + px][y0 + py] = 0.9999f;
				}
			}
		}
	}
	
	public static void generateInitializedFractalTerrain(int offsetX, int offsetY, int scale, int startingScale, long seed, int worldSizeX, int worldSizeZ, float[][] bigMap, float lowerLimit, float upperLimit, int maxResolution) {
		// Increase the "grid" of points with already known heights in each round by a factor of 2×2, like so(# marks the gridpoints of the first grid, * the points of the second grid and + the points of the third grid(and so on…)):
		/*
			#+*+#
			+++++
			*+*+*
			+++++
			#+*+#
		 */
		// Each new gridpoint gets the average height value of the surrounding known grid points which is afterwards offset by a random value. Here is a visual representation of this process(with random starting values):
		/*
		█░▒▓						small							small
			█???█	grid	█?█?█	random	█?▓?█	grid	██▓██	random	██▓██
			?????	resize	?????	change	?????	resize	▓▓▒▓█	change	▒▒▒▓█
			?????	→→→→	▒?▒?▓	→→→→	▒?░?▓	→→→→	▒▒░▒▓	→→→→	▒░░▒▓
			?????			?????	of new	?????			░░░▒▓	of new	░░▒▓█
			 ???▒			 ?░?▒	values	 ?░?▒			 ░░▒▒	values	 ░░▒▒
			 
			 Another important thing to note is that the side length of the grid has to be 2^n + 1 because every new gridpoint needs a new neighbor. So the rightmost column and the bottom row are already part of the next map piece.
			 One other important thing in the implementation of this algorithm is that the relative height change has to decrease the in every iteration. Otherwise the terrain would look really noisy.
		 */
		int max =startingScale+1;
		Random rand = new Random(seed);
		for(int res = startingScale*2; res != 0; res >>>= 1) {
			// x coordinate on the grid:
			for(int x = 0; x < max; x += res<<1) {
				for(int y = res; y+res < max; y += res<<1) {
					rand.setSeed(getSeed(x, y, offsetX, offsetY, worldSizeX, worldSizeZ, seed, res, maxResolution));
					bigMap[x][y] = (bigMap[x][y-res]+bigMap[x][y+res])/2 + (rand.nextFloat()-0.5f)*res/scale;
					if(bigMap[x][y] > upperLimit) bigMap[x][y] = upperLimit;
					if(bigMap[x][y] < lowerLimit) bigMap[x][y] = lowerLimit;
				}
			}
			// y coordinate on the grid:
			for(int x = res; x+res < max; x += res<<1) {
				for(int y = 0; y < max; y += res<<1) {
					rand.setSeed(getSeed(x, y, offsetX, offsetY, worldSizeX, worldSizeZ, seed, res, maxResolution));
					bigMap[x][y] = (bigMap[x-res][y]+bigMap[x+res][y])/2 + (rand.nextFloat()-0.5f)*res/scale;
					if(bigMap[x][y] > upperLimit) bigMap[x][y] = upperLimit;
					if(bigMap[x][y] < lowerLimit) bigMap[x][y] = lowerLimit;
				}
			}
			// No coordinate on the grid:
			for(int x = res; x+res < max; x += res<<1) {
				for(int y = res; y+res < max; y += res<<1) {
					rand.setSeed(getSeed(x, y, offsetX, offsetY, worldSizeX, worldSizeZ, seed, res, maxResolution));
					bigMap[x][y] = (bigMap[x-res][y-res]+bigMap[x+res][y-res]+bigMap[x-res][y+res]+bigMap[x+res][y+res])/4 + (rand.nextFloat()-0.5f)*res/scale;
					if(bigMap[x][y] > upperLimit) bigMap[x][y] = upperLimit;
					if(bigMap[x][y] < lowerLimit) bigMap[x][y] = lowerLimit;
				}
			}
		}
	}
	public static void generateSparseFractalTerrain(int wx, int wy, int width, int height, int scale, long seed, int worldSizeX, int worldSizeZ, float[][] map, int maxResolution) {
		for(int x0 = 0; x0 < width; x0 += scale) {
			for(int y0 = 0; y0 < height; y0 += scale) {
				generateFractalTerrain(wx + x0, wy + y0, x0, y0, Math.min(width-x0, scale), Math.min(height-y0, scale), scale, seed, worldSizeX, worldSizeZ, map, maxResolution);
			}
		}
	}
	public static float[][] generateFractalTerrain(int wx, int wy, int width, int height, int scale, long seed, int worldSizeX, int worldSizeZ) {
		float[][] map = new float[width][height];
		for(int x0 = 0; x0 < width; x0 += scale) {
			for(int y0 = 0; y0 < height; y0 += scale) {
				generateFractalTerrain(wx + x0, wy + y0, x0, y0, Math.min(width-x0, scale), Math.min(height-y0, scale), scale, seed, worldSizeX, worldSizeZ, map, 1);
			}
		}
		return map;
	}
}
