package de.kitsystem;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class StandListener implements Listener {
    private final KitSystem plugin;

    public StandListener(KitSystem plugin) { this.plugin = plugin; }

    /** Armorstand-Item platzieren. */
    @EventHandler
    public void onPlace(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        String id = plugin.stands().kitOfItem(item);
        if (id == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!p.hasPermission("kitsystem.admin")) return;
        Kit kit = plugin.kits().get(id);
        if (kit == null) {
            KitSystem.msg(p, "Dieses Kit existiert nicht mehr.");
            return;
        }
        Location loc = e.getClickedBlock().getRelative(e.getBlockFace()).getLocation().add(0.5, 0, 0.5);
        float yaw = Math.round((p.getLocation().getYaw() + 180f) / 45f) * 45f;
        loc.setYaw(yaw);
        plugin.stands().spawn(loc, kit);
        item.setAmount(item.getAmount() - 1);
        KitSystem.msg(p, "Ständer platziert. Shift + Rechtsklick hebt ihn wieder auf.");
    }

    /** Rechtsklick auf Ständer: Kit nehmen. Admin + Sneak: aufheben (zum Umplatzieren). */
    @EventHandler
    public void onClickStand(PlayerInteractAtEntityEvent e) {
        String id = plugin.stands().kitOf(e.getRightClicked());
        if (id == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        Kit kit = plugin.kits().get(id);
        if (kit == null) {
            e.getRightClicked().remove();
            return;
        }
        if (p.isSneaking() && p.hasPermission("kitsystem.admin")) {
            e.getRightClicked().remove();
            Map<Integer, ItemStack> left = p.getInventory().addItem(plugin.stands().createItem(kit));
            left.values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
            KitSystem.msg(p, "Ständer aufgehoben - du kannst ihn neu platzieren.");
            return;
        }
        if (!p.hasPermission("kitsystem.use")) return;
        plugin.kits().give(p, kit);
        KitSystem.msg(p, "Du hast das Kit " + kit.displayName + " erhalten.");
    }

    @EventHandler
    public void onManipulate(PlayerArmorStandManipulateEvent e) {
        if (plugin.stands().kitOf(e.getRightClicked()) != null) e.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (plugin.stands().kitOf(e.getEntity()) != null) e.setCancelled(true);
    }

    /** Beim Laden eines Chunks: Ständer mit dem aktuellen Kit abgleichen. */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        for (Entity en : e.getEntities()) {
            String id = plugin.stands().kitOf(en);
            if (id == null) continue;
            Kit kit = plugin.kits().get(id);
            if (kit == null) {
                plugin.getServer().getScheduler().runTask(plugin, en::remove);
            } else {
                plugin.stands().apply((ArmorStand) en, kit);
            }
        }
    }
}
