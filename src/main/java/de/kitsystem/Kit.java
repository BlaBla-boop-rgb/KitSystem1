package de.kitsystem;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class Kit {
    public final String id;
    public String displayName;
    /** Slots 0-35 wie im Spielerinventar (0-8 = Hotbar). */
    public ItemStack[] storage = new ItemStack[36];
    /** 0 = Stiefel, 1 = Hose, 2 = Brustplatte, 3 = Helm. */
    public ItemStack[] armor = new ItemStack[4];
    public ItemStack offhand;
    /** null = Icon wird automatisch anhand des Namens erzeugt. */
    public ItemStack customIcon;

    public Kit(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public void captureFrom(Player p) {
        PlayerInventory inv = p.getInventory();
        storage = cloneAll(inv.getStorageContents(), 36);
        armor = cloneAll(inv.getArmorContents(), 4);
        ItemStack off = inv.getItemInOffHand();
        offhand = off.getType().isAir() ? null : off.clone();
    }

    static ItemStack[] cloneAll(ItemStack[] src, int size) {
        ItemStack[] out = new ItemStack[size];
        for (int i = 0; i < Math.min(size, src.length); i++) {
            if (src[i] != null && !src[i].getType().isAir()) out[i] = src[i].clone();
        }
        return out;
    }

    /** Item, das der Armorstand in der Hand haelt. */
    public ItemStack mainWeapon() {
        for (int i = 0; i < 9; i++) {
            ItemStack it = storage[i];
            if (it != null && it.getType().name().endsWith("_SWORD")) return it.clone();
        }
        for (int i = 0; i < 9; i++) {
            if (storage[i] != null) return storage[i].clone();
        }
        return null;
    }
}
