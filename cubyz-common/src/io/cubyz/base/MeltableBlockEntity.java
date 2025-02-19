package io.cubyz.base;

import org.joml.Vector3i;

import io.cubyz.api.CubyzRegistries;
import io.cubyz.blocks.BlockEntity;
import io.cubyz.blocks.Updateable;
import io.cubyz.world.Surface;

/**
 * A block that can turn into water block when a temperature threshold is met.<br>
 * TODO: Make accessible to all block types with different thresholds and allow definition of those in addon files.
 */

public class MeltableBlockEntity extends BlockEntity implements Updateable {
	
	int heatCount;

	public MeltableBlockEntity(Surface surface, Vector3i pos) {
		super(surface, pos);
	}

	@Override
	public boolean randomUpdates() {
		return true;
	}

	@Override
	public void update(boolean isRandomUpdate) {
		if (isRandomUpdate) {
			/*float temp = surface.getBiome(position.x, position.z).temperature - position.y*0; TODO: measure temperature in the new biome system.
			if (temp > 0.45f) {
				heatCount++;
			}
			if (heatCount == 5) {
				surface.placeBlock(position.x, position.y, position.z, CubyzRegistries.BLOCK_REGISTRY.getByID("cubyz:water"), (byte) 0);
			}*/
		}
	}

}
