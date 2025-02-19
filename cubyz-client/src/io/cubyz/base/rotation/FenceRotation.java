package io.cubyz.base.rotation;

import org.joml.RayAabIntersection;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;

import io.cubyz.api.Resource;
import io.cubyz.blocks.Block;
import io.cubyz.blocks.BlockInstance;
import io.cubyz.blocks.RotationMode;
import io.cubyz.client.Meshes;
import io.cubyz.entity.Entity;
import io.cubyz.models.Model;
import io.cubyz.util.ByteWrapper;
import io.cubyz.util.FloatFastList;
import io.cubyz.util.IntFastList;
import io.cubyz.world.NormalChunk;
import io.cubyz.world.Surface;

public class FenceRotation implements RotationMode {
	Resource id = new Resource("cubyz", "fence");
	@Override
	public Resource getRegistryID() {
		return id;
	}

	@Override
	public boolean generateData(Surface surface, int x, int y, int z, Vector3f relativePlayerPosition, Vector3f playerDirection, Vector3i relativeDirection, ByteWrapper currentData, boolean blockPlacing) {
		if(!blockPlacing) return false;
		NormalChunk chunk = surface.getChunk(x >> NormalChunk.chunkShift, y >> NormalChunk.chunkShift, z >> NormalChunk.chunkShift);
		currentData.data = (byte)1;
		// Get all neighbors and set the corresponding bits:
		Block[] neighbors = chunk.getNeighbors(x, y ,z);
		if(neighbors[0] != null && neighbors[0].isSolid()) {
			currentData.data |= 0b00010;
		}
		if(neighbors[1] != null && neighbors[1].isSolid()) {
			currentData.data |= 0b00100;
		}
		if(neighbors[2] != null && neighbors[2].isSolid()) {
			currentData.data |= 0b01000;
		}
		if(neighbors[3] != null && neighbors[3].isSolid()) {
			currentData.data |= 0b10000;
		}
		return true;
	}

	@Override
	public boolean dependsOnNeightbors() {
		return true;
	}

	@Override
	public Byte updateData(byte data, int dir, Block newNeighbor) {
		if(dir == 4 | dir == 5) return data;
		byte mask = (byte)(1 << (dir + 1));
		data &= ~mask;
		if(newNeighbor != null && newNeighbor.isSolid())
			data |= mask;
		return data;
	}

	@Override
	public boolean checkTransparency(byte data, int dir) {
		return true;
	}

	@Override
	public byte getNaturalStandard() {
		return 1;
	}

	@Override
	public boolean changesHitbox() {
		return true;
	}

	@Override
	public float getRayIntersection(RayAabIntersection intersection, BlockInstance bi, Vector3f min, Vector3f max, Vector3f transformedPosition) {
		// Check the +. TODO: Check the actual model.
		float xOffset = 0;
		float xLen = 1;
		float zOffset = 0;
		float zLen = 1;
		if((bi.getData() & 0b00010) == 0) {
			xOffset += 0.5f;
			xLen -= 0.5f;
		}
		if((bi.getData() & 0b00100) == 0) {
			xLen -= 0.5f;
		}
		if((bi.getData() & 0b01000) == 0) {
			zOffset += 0.5f;
			zLen -= 0.5f;
		}
		if((bi.getData() & 0b10000) == 0) {
			zLen -= 0.5f;
		}
		min.z += zOffset;
		max.z = min.z + zLen;
		if(!intersection.test(min.x + 0.375f, min.y, min.z, max.x - 0.375f, max.y, max.z)) {
			min.z -= zOffset;
			max.z = min.z + 1;
			min.x += xOffset;
			max.x = min.x + xLen;
			if(!intersection.test(min.x, min.y, min.z + 0.375f, max.x, max.y, max.z - 0.375f)) {
				return Float.MAX_VALUE;
			}
		}
		return min.add(0.5f, 0.5f, 0.5f).sub(transformedPosition).length();
	}

	@Override
	public boolean checkEntity(Vector3f pos, float width, float height, int x, int y, int z, byte blockData) {
		// Hit area is just a simple + with a width of 0.25:
		return y >= pos.y
				&& y <= pos.y + height
				&&
				(
					( // - of the +:
						x + 0.625f >= pos.x - width
						&& x + 0.375f <= pos.x+ width
						&& z + 1 >= pos.x - width
						&& z <= pos.x + width
					)
					||
					( // | of the +:
						z + 0.625f >= pos.z - width
						&& z + 0.375f <= pos.z+ width
						&& x + 1 >= pos.x - width
						&& x <= pos.x + width
					)
				);
	}

	@Override
	public boolean checkEntityAndDoCollision(Entity ent, Vector4f vel, int x, int y, int z, byte blockData) {
		// Hit area is just a simple + with a width of 0.25:
		float xOffset = 0;
		float xLen = 1;
		float zOffset = 0;
		float zLen = 1;
		if((blockData & 0b00010) == 0) {
			xOffset += 0.5f;
			xLen -= 0.5f;
		}
		if((blockData & 0b00100) == 0) {
			xLen -= 0.5f;
		}
		if((blockData & 0b01000) == 0) {
			zOffset += 0.5f;
			zLen -= 0.5f;
		}
		if((blockData & 0b10000) == 0) {
			zLen -= 0.5f;
		}
		
		ent.aabCollision(vel, x + xOffset, y, z + 0.375f, xLen, 1, 0.25f, blockData);
		ent.aabCollision(vel, x + 0.375f, y, z + zOffset, 0.35f, 1, zLen, blockData);
		return false;
	}
	
	@Override
	public int generateChunkMesh(BlockInstance bi, FloatFastList vertices, FloatFastList normals, IntFastList faces, IntFastList lighting, FloatFastList texture, IntFastList renderIndices, int renderIndex) {
		Model model = Meshes.blockMeshes.get(bi.getBlock()).model;
		int x = bi.getX() & NormalChunk.chunkMask;
		int y = bi.getY() & NormalChunk.chunkMask;
		int z = bi.getZ() & NormalChunk.chunkMask;
		int offsetX = bi.getBlock().atlasX;
		int offsetY = bi.getBlock().atlasY;
		boolean negX = (bi.getData() & 0b00010) == 0;
		boolean posX = (bi.getData() & 0b00100) == 0;
		boolean negZ = (bi.getData() & 0b01000) == 0;
		boolean posZ = (bi.getData() & 0b10000) == 0;
		
		// Simply copied the code from model and move all vertices to the center that touch an edge that isn't connected to another fence.
		int indexOffset = vertices.size/3;
		int[] light = bi.light;
		for(int i = 0; i < model.positions.length; i += 3) {
			float newX = model.positions[i];
			if(newX == 0 && negX) newX = 0.5f;
			if(newX == 1 && posX) newX = 0.5f;
			newX += x;
			float newY = model.positions[i+1] + y;
			float newZ = model.positions[i+2];
			if(newZ == 0 && negZ) newZ = 0.5f;
			if(newZ == 1 && posZ) newZ = 0.5f;
			newZ += z;
			vertices.add(newX);
			vertices.add(newY);
			vertices.add(newZ);
			
			lighting.add(Model.interpolateLight(model.positions[i], model.positions[i+1], model.positions[i+2], model.normals[i], model.normals[i+1], model.normals[i+2], light));
			renderIndices.add(renderIndex);
		}
		
		for(int i = 0; i < model.indices.length; i++) {
			faces.add(model.indices[i] + indexOffset);
		}
		
		for(int i = 0; i < model.textCoords.length; i += 2) {
			texture.add((model.textCoords[i] + offsetX)/Meshes.atlasSize);
			texture.add((model.textCoords[i+1] + offsetY)/Meshes.atlasSize);
		}
		
		normals.add(model.normals);
		return renderIndex + 1;
	}
}
