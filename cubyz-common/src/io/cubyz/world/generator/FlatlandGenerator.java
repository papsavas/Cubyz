package io.cubyz.world.generator;

import io.cubyz.api.CubyzRegistries;
import io.cubyz.api.Resource;
import io.cubyz.blocks.Block;
import io.cubyz.world.Chunk;
import io.cubyz.world.Surface;

/**
 * Simple generator that does flat worlds with 3 layers.
 * 1. Grass
 * 2. Dirt
 * 3. Bedrock
 */

public class FlatlandGenerator extends SurfaceGenerator {

	private static Block grass = CubyzRegistries.BLOCK_REGISTRY.getByID("cubyz:grass");
	private static Block soil = CubyzRegistries.BLOCK_REGISTRY.getByID("cubyz:soil");
	
	@Override
	public void generate(Chunk chunk, Surface surface) {
		for (int x = 0; x < chunk.getWidth(); x++) {
			for (int z = 0; z < chunk.getWidth(); z++) {
				for (int y = 0; y < chunk.getWidth(); y++) {
					int wy = y + chunk.getWorldY();
					if(wy >= 3) continue;
					Block b = null;
					if (wy == 2) {
						b = grass;
					} else {
						b = soil;
					}
					chunk.updateBlock(x, y, z, b);
				}
			}
		}
	}

	@Override
	public Resource getRegistryID() {
		return new Resource("cubyz", "flatland");
	}

}
