package io.cubyz.save;

import java.util.Map;

import io.cubyz.api.CubyzRegistries;
import io.cubyz.api.RegistryElement;
import io.cubyz.api.Resource;
import io.cubyz.blocks.Block;
import io.cubyz.math.Bits;

public class BlockChange {
	// TODO: make it possible for the user to add/remove mods without completely shifting the auto-generated ids.
	public int oldType, newType; // IDs of the blocks. -1 = air
	public int x, y, z; // Coordinates relative to the respective chunk.
	
	public BlockChange(int ot, int nt, int x, int y, int z) {
		oldType = ot;
		newType = nt;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public BlockChange(byte[] data, int off, Map<Resource, Integer> blockPalette) {
		x = Bits.getInt(data, off + 0);
		y = Bits.getInt(data, off + 4);
		z = Bits.getInt(data, off + 8);
		
		// Convert the palette (torus-specific) ID to the runtime ID
		int palId = Bits.getInt(data, off + 12);
		int runtimeId = -1;
		if (palId != -1) {
			for (Resource id : blockPalette.keySet()) {
				Integer i = blockPalette.get(id);
				if (i == palId) {
					Block b = (Block) CubyzRegistries.BLOCK_REGISTRY.getByID(id.toString());
					if (b == null) {
						throw new MissingBlockException(id);
					} else {
						runtimeId = b.ID;
					}
				}
			}
		}
		newType = runtimeId;
		oldType = -2;
	}
	
	/**
	 * Save BlockChange to array data at offset off.
	 * Data Length: 16 bytes
	 * @param data
	 * @param off
	 */
	public void save(byte[] data, int off, Map<Resource, Integer> blockPalette) {
		Bits.putInt(data, off, x);
		Bits.putInt(data, off + 4, y);
		Bits.putInt(data, off + 8, z);
		if (newType == -1) {
			Bits.putInt(data, off + 12, -1);
		} else {
			Resource id = null;
			for (RegistryElement elem : CubyzRegistries.BLOCK_REGISTRY.registered()) {
				Block b = (Block) elem;
				if (b.ID == newType) {
					id = b.getRegistryID();
				}
			}
			if (id == null) {
				throw new RuntimeException("newType is invalid: " + newType);
			}
			if (!blockPalette.containsKey(id)) {
				blockPalette.put(id, blockPalette.size());
			}
			Bits.putInt(data, off + 12, blockPalette.get(id));
		}
	}
}
