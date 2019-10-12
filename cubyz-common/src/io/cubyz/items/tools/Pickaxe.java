package io.cubyz.items.tools;

import io.cubyz.base.init.MaterialInit;
import io.cubyz.blocks.Block;
import io.cubyz.items.Item;
import io.cubyz.items.ItemStack;

public class Pickaxe extends Tool {
	private static final int HEAD = 300, BINDING = 50, HANDLE = 50;
	private static final float baseSpeed = 5.0f;

	public Pickaxe(Material head, Material binding, Material handle) {
		super(head, binding, handle, calculateSpeed(head, binding, handle), calculateDmg(head, binding, handle));
		durability = maxDurability = head.headDurability + binding.bindingDurability + handle.handleDurability;
		// The image is just an overlay of the part images:
		texturePath = "assets/cubyz/textures/items/parts/"+handle.getRegistryID().getID()+"_handle.png#assets/cubyz/textures/items/parts/"+head.getRegistryID().getID()+"_pickaxe_head.png#assets/cubyz/textures/items/parts/"+binding.getRegistryID().getID()+"_binding.png";
	}
	
	private static float calculateSpeed(Material head, Material binding, Material handle) {
		return head.miningSpeed * baseSpeed;
	}
	
	private static float calculateDmg(Material head, Material binding, Material handle) {
		return head.damage + 1;// A pickaxe is better than a shovel.
	}
	
	public static Item canCraft(ItemStack head, ItemStack binding, ItemStack handle) {
		Material he = null, bi = null, ha = null;
		if(head.getItem() != null)
		for(Material ma : MaterialInit.MATERIALS) {
			if(ma.getItems().containsKey(head.getItem()) && head.getAmount()*ma.getItems().get(head.getItem()) >= HEAD) {
				he = ma;
			}
			if(ma.getItems().containsKey(binding.getItem()) && binding.getAmount()*ma.getItems().get(binding.getItem()) >= BINDING) {
				bi = ma;
			}
			if(ma.getItems().containsKey(handle.getItem()) && handle.getAmount()*ma.getItems().get(handle.getItem()) >= HANDLE) {
				ha = ma;
			}
		}
		if(he == null || bi == null || ha == null)
			return null;
		return new Pickaxe(he, bi, ha);
			
	}

	@Override
	public boolean canBreak(Block b) {
		return true; // true until blocks have been upgraded.
	}
}
