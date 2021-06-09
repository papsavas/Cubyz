package io.cubyz.ui;

import io.cubyz.client.Cubyz;
import io.cubyz.items.Inventory;
import io.cubyz.rendering.Window;
import io.cubyz.ui.components.InventorySlot;

/**
 * Basic overlay while in-game.<br>
 * Contains hotbar, hunger bars, and crosshair.
 */

public class GameOverlay extends MenuGUI {

	int crosshair;
	int selection;

	long lastPlayerHurtMs; // stored here and not in Player for easier multiplayer integration
	float lastPlayerHealth;

	private InventorySlot inv [] = new InventorySlot[8];

	@Override
	public void init(long nvg) {
		crosshair = NGraphics.loadImage("assets/cubyz/textures/crosshair.png");
		selection = NGraphics.loadImage("assets/cubyz/guis/inventory/selected_slot.png");

		Inventory inventory = Cubyz.player.getInventory();
		for(int i = 0; i < 8; i++) {
			inv[i] = new InventorySlot(inventory.getStack(i), i*64 - 256, 64, Component.ALIGN_BOTTOM);
		}
	}

	@Override
	public void render(long nvg, Window win) {
		NGraphics.drawImage(crosshair, win.getWidth()/2 - 16, win.getHeight()/2 - 16, 32, 32);
		NGraphics.setColor(0, 0, 0);
		if(!(Cubyz.gameUI.getMenuGUI() instanceof GeneralInventory)) {
			NGraphics.drawImage(selection, win.getWidth()/2 - 254 + Cubyz.inventorySelection*64, win.getHeight() - 62, 60, 60);
			for(int i = 0; i < 8; i++) {
				inv[i].reference = Cubyz.player.getInventory().getStack(i); // without it, if moved in inventory, stack won't refresh
				inv[i].render(nvg, win);
			}
		}

		// Draw the health bar:
		HealthBar healthBar = new HealthBar(Cubyz.player.maxHealth, Cubyz.player.health);
		if(lastPlayerHealth != healthBar.currentFeature) {
			if(lastPlayerHealth > healthBar.currentFeature) {
				lastPlayerHurtMs = System.currentTimeMillis();
			}
			lastPlayerHealth = healthBar.currentFeature;
		}
		if (System.currentTimeMillis() < lastPlayerHurtMs+510) {
			NGraphics.setColor(255, 50, 50, (int) (255-(System.currentTimeMillis()-lastPlayerHurtMs))/2);
			NGraphics.fillRect(0, 0, win.getWidth(), win.getHeight());
		}

		healthBar.drawBarLogoAndText(win, 6, 9);
		healthBar.drawInsideBar(6, win);

		// Draw the hunger bar:
		HungerBar hungerBar = new HungerBar(Cubyz.player.maxHunger, Cubyz.player.hunger);
		hungerBar.drawBarLogoAndText(win, 36, 39);
		hungerBar.drawInsideBar(36, win);
	}

	@Override
	public boolean doesPauseGame() {
		return false;
	}
}