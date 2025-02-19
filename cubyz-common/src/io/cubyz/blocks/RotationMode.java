package io.cubyz.blocks;

import org.joml.RayAabIntersection;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;

import io.cubyz.api.RegistryElement;
import io.cubyz.entity.Entity;
import io.cubyz.util.ByteWrapper;
import io.cubyz.util.FloatFastList;
import io.cubyz.util.IntFastList;
import io.cubyz.world.Surface;

/**
 * Each block gets 8 bit of additional storage(apart from the reference to the block type).<br>
 * These 8 bits are accessed and interpreted by the `RotationMode`.<br>
 * With the `RotationMode` interface there is almost no limit to what can be done with those 8 bit.
 */

public interface RotationMode extends RegistryElement {
	/**
	 * Called when generating the chunk mesh.
	 * @param bi
	 * @param vertices
	 * @param normals
	 * @param faces
	 * @param lighting
	 * @param texture
	 * @param renderIndex
	 * @return incremented renderIndex
	 */
	public int generateChunkMesh(BlockInstance bi, FloatFastList vertices, FloatFastList normals, IntFastList faces, IntFastList lighting, FloatFastList texture, IntFastList renderIndices, int renderIndex);
	
	/**
	 * Update or place a block.
	 * @param surface
	 * @param x
	 * @param y
	 * @param z
	 * @param relativePlayerPosition Position of the player head relative to the (0, 0, 0) corner of the block.
	 * @param playerDirection
	 * @param relativeDir the direction in which the selected neighbor is.
	 * @param currentData 0 if no block was there before.
	 * @param blockPlacing true if the position of the block was previously empty/nonsolid.
	 * @return true if the placing was successful, false otherwise.
	 */
	public boolean generateData(Surface surface, int x, int y, int z, Vector3f relativePlayerPosition, Vector3f playerDirection, Vector3i relativeDir, ByteWrapper currentData, boolean blockPlacing);

	/**
	 * @return if the block should be destroyed or changed when a certain neighbor is removed.
	 */
	public boolean dependsOnNeightbors();
	
	/**
	 * Updates data of a placed block if the RotationMode dependsOnNeighbors().
	 * If the returned value is null, then the block will be removed instead of only updating the data.
	 * @param oldData
	 * @param removedDir given as neighbor index (See NormalChunk.)
	 * @return new data
	 */
	public Byte updateData(byte oldData, int removedDir, Block newNeighbor);
	
	/**
	 * A RotationMode may even alter the blocks transparency. Here is where it's done.
	 * @param data The blocks data
	 * @param relativeDir the relative direction of the other block. Given as difference of the indices in the Chunk array. 100% unintuitive to use, maybe I'll add an automatic transformation later.
	 */
	public boolean checkTransparency(byte data, int relativeDir);
	
	/**
	 * @return standard data for natural generation.
	 */
	public byte getNaturalStandard();
	
	/**
	 * @return Whether this RotationMode changes this blocks hitbox for player collision or block selection.
	 */
	public boolean changesHitbox();
	
	/**
	 * 
	 * @param intersection
	 * @param bi
	 * @param min minimal point of the surrounding block. May be overwritten.
	 * @param max maximal point of the surrounding block. May be overwritten.
	 * @return
	 */
	public float getRayIntersection(RayAabIntersection intersection, BlockInstance bi, Vector3f min, Vector3f max, Vector3f transformedPosition);

	/**
	 * Check if the entity would collide with the block.
	 * @param pos position of the entity.
	 * @param width width of the entity hitbox.
	 * @param height of the entity hitbox.
	 * @param x x-coordinate of the block.
	 * @param y y-coordinate of the block.
	 * @param z z-coordinate of the block.
	 * @param data block data
	 * @return Whether the entity and block hitboxes overlap.
	 */
	public boolean checkEntity(Vector3f pos, float width, float height, int x, int y, int z, byte blockData);
	
	/**
	 * Check if the entity would collide with the block, if its position was changed by `vel`.
	 * If a collision occurs, adjust the velocity in way such that the entity does not move inside the block.
	 * @param ent Entity to consider
	 * @param vel Velocity of the entity. The 4th element is reserved for stepping: a y-movement that is done exactly once.
	 * @param x x-coordinate of the block.
	 * @param y y-coordinate of the block.
	 * @param z z-coordinate of the block.
	 * @param data block data
	 * @return Returns true if the block behaves like a normal block and therefor needs to be handled like a normal block in the specified direction. Returns false if everything has been handled already in here.
	 */
	public boolean checkEntityAndDoCollision(Entity ent, Vector4f vel, int x, int y, int z, byte blockData);
}
