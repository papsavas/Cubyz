package io.cubyz.world.cubyzgenerators;

import io.cubyz.api.RegistryElement;
import io.cubyz.world.Region;
import io.cubyz.world.Chunk;
import io.cubyz.world.Surface;

/**
 * Some interface to access all different generators(caves,terrain,…) through one simple function.
 */

public interface Generator extends RegistryElement {
	
	abstract int getPriority(); // Used to prioritize certain generators(like map generation) over others(like vegetation generation).
	abstract void generate(long seed, int wx, int wy, int wz, Chunk chunk, Region containingRegion, Surface surface);
	
	/**
	 * To avoid duplicate seeds in similar generation algorithms, the SurfaceGenerator xors the torus-seed with the generator specific seed.
	 * @return The seed of this generator. SHould be unique
	 */
	abstract long getGeneratorSeed();
	
}
