package io.cubyz.items;

import io.cubyz.api.CurrentSurfaceRegistries;
import io.cubyz.blocks.Block;
import io.cubyz.ndt.NDTContainer;

/**
 * A stack of items, used for storage in inventories.
 */

public class ItemStack {

	private Item item;
	int amount = 0;
	
	public ItemStack() {
		item = null;
	}
	
	public ItemStack(Item item) {
		this.item = item;
	}
	
	public ItemStack(Item item, int amount) {
		this.item = item;
		this.amount = amount;
	}
	
	/**
	 * Create a new ItemStack from the supplied item stack.<br>
	 * Deletes the content of the supplied item stack to avoid duplication.
	 * @param supplier
	 */
	public ItemStack(ItemStack supplier) {
		this.item = supplier.item;
		this.amount = supplier.amount;
		supplier.clear();
	}
	
	public void update() {}
	
	public boolean filled() {
		return amount >= item.stackSize;
	}
	
	public boolean empty() {
		return amount <= 0;
	}
	
	public int add(int number) {
		this.amount += number;
		if(this.amount < 0) {
			number = number-this.amount;
			this.amount = 0;
		}
		else if(this.amount > item.stackSize) {
			number = number-this.amount+item.stackSize;
			this.amount = item.stackSize;
		}
		if(empty()) {
			clear();
		}
		return number;
	}
	
	/**
	 * @param number number of items
	 * @return whether the given number of items can be added to this stack.
	 */
	public boolean canAddAll(int number) {
		return this.amount + number <= item.stackSize;
	}
	
	public void setItem(Item i) {
		item = i;
	}
	
	public Item getItem() {
		return item;
	}
	
	public Block getBlock() {
		if(item == null)
			return null;
		if (item instanceof ItemBlock)
			return ((ItemBlock) item).getBlock();
		else
			return null;
	}
	
	public int getAmount() {
		return amount;
	}
	
	/**
	 * For use in special cases only!
	 * @param a new amount
	 */
	public void setAmount(int a) {
		amount = a;
	}
	
	public void loadFrom(NDTContainer container, CurrentSurfaceRegistries registries) {
		item = registries.itemRegistry.getByID(container.getString("id"));
		if (item == null) {
			throw new IllegalStateException("item " + container.getString("id") + " is not in registry.");
		}
		if (container.hasKey("size"))
			amount = container.getInteger("size");
		else
			amount = 1;
	}
	
	public void saveTo(NDTContainer container) {
		if (item == null) {
			throw new IllegalStateException("item is null");
		}
		container.setString("id", item.getRegistryID().toString());
		container.setInteger("size", amount);
	}
	
	public void clear() {
		item = null;
		amount = 0;
	}
	
}