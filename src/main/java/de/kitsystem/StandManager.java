package de.kitsystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class StandManager {
    private final KitSystem plugin;
    private final NamespacedKey standKey;      // auf der Entity
    private final NamespacedKey itemKey;       // auf dem Item

    public StandManager(KitSystem plugin) {
        this.plugin = plugin;
        this.standKey = new NamespacedKey(plugin, "kit_stand");
        this.itemKey = new NamespacedKey(plugin, "kit_stand_item");
    }

    /** Das Armorstand-Item, das man nach Abschluss eines Kits bekommt. */
    public ItemStack createItem(Kit kit) {
        ItemStack it = new ItemStack(Material.ARMOR_STAND);
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.text("Kit-Ständer: " + kit.displayName, NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        m.lore(List.of(
                Component.text("Rechtsklick auf einen Block: platzieren", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Shift + Rechtsklick auf den Ständer: aufheben", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        m.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, kit.id);
        it.setItemMeta(m);
        return it;
    }

    public String kitOfItem(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    public String kitOf(Entity e) {
        if (!(e instanceof ArmorStand)) return null;
        return e.getPersistentDataContainer().get(standKey, PersistentDataType.STRING);
    }

    public ArmorStand spawn(Location loc, Kit kit) {
        return loc.getWorld().spawn(loc, ArmorStand.class, as -> {
            as.setArms(true);
            as.setBasePlate(false);
            as.setGravity(false);
            as.setInvulnerable(true);
            as.setPersistent(true);
            as.setCanPickupItems(false);
            as.setDisabledSlots(EquipmentSlot.values());
            as.getPersistentDataContainer().set(standKey, PersistentDataType.STRING, kit.id);
            apply(as, kit);
        });
    }

    /** Setzt Ruestung, Waffe und Namen des Staenders passend zum Kit (auch nach Aenderungen). */
    public void apply(ArmorStand as, Kit kit) {
        EntityEquipment eq = as.getEquipment();
        eq.setBoots(copy(kit.armor[0]));
        eq.setLeggings(copy(kit.armor[1]));
        eq.setChestplate(copy(kit.armor[2]));
        eq.setHelmet(copy(kit.armor[3]));
        eq.setItemInMainHand(kit.mainWeapon());
        eq.setItemInOffHand(copy(kit.offhand));
        as.customName(Component.text(kit.displayName, NamedTextColor.AQUA));
        as.setCustomNameVisible(true);
    }

    private static ItemStack copy(ItemStack i) {
        return i == null ? null : i.clone();
    }

    /** Aktualisiert alle geladenen Staender dieses Kits. */
    public void refreshAll(Kit kit) {
        for (World w : plugin.getServer().getWorlds()) {
            for (ArmorStand as : w.getEntitiesByClass(ArmorStand.class)) {
                if (kit.id.equals(kitOf(as))) apply(as, kit);
            }
        }
    }

    public void removeAll(String kitId) {
        for (World w : plugin.getServer().getWorlds()) {
            for (ArmorStand as : w.getEntitiesByClass(ArmorStand.class)) {
                if (kitId.equals(kitOf(as))) as.remove();
            }
        }
    }
}
