package de.kitsystem;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public final class GuiListener implements Listener {
    private final KitSystem plugin;

    public GuiListener(KitSystem plugin) { this.plugin = plugin; }

    // ---------- Amboss-Item ----------
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.giveEditorItem(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.giveEditorItem(p));
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!plugin.isEditorItem(e.getItem())) return;
        e.setCancelled(true);
        plugin.menus().openList(e.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (plugin.isEditorItem(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    // ---------- Menues ----------
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (Menus.isOurs(e.getView().getTopInventory().getHolder())) e.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        var holder = top.getHolder();
        if (!Menus.isOurs(holder)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != top) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        int slot = e.getSlot();

        if (holder instanceof Menus.ListHolder lh) {
            String id = lh.kitBySlot.get(slot);
            Kit kit = plugin.kits().get(id);
            if (kit != null) {
                p.playSound(p, Sound.UI_BUTTON_CLICK, 0.6f, 1f);
                plugin.menus().openLayout(p, kit);
            }
        } else if (holder instanceof Menus.LayoutHolder h) {
            handleLayout(p, h, slot);
        } else if (holder instanceof Menus.AdminHolder h) {
            handleAdmin(p, h, slot);
        }
    }

    private void handleLayout(Player p, Menus.LayoutHolder h, int slot) {
        if (slot < 36) {
            if (h.selected == -1) {
                if (h.kit.storage[h.map[Menus.toInv(slot)]] == null) return; // leeren Slot nicht auswaehlen
                h.selected = slot;
                p.playSound(p, Sound.UI_BUTTON_CLICK, 0.6f, 1.3f);
            } else if (h.selected == slot) {
                h.selected = -1;
            } else {
                int a = Menus.toInv(h.selected), b = Menus.toInv(slot);
                int t = h.map[a];
                h.map[a] = h.map[b];
                h.map[b] = t;
                h.selected = -1;
                plugin.kits().saveLayout(p.getUniqueId(), h.kit.id, h.map);
                p.playSound(p, Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
            }
            plugin.menus().render(h);
        } else if (slot == 45) {
            for (int i = 0; i < 36; i++) h.map[i] = i;
            h.selected = -1;
            plugin.kits().saveLayout(p.getUniqueId(), h.kit.id, h.map);
            plugin.menus().render(h);
        } else if (slot == 49) {
            p.closeInventory();
            KitSystem.msg(p, "Layout für " + h.kit.displayName + " gespeichert.");
        } else if (slot == 53) {
            plugin.menus().openList(p);
        }
    }

    private void handleAdmin(Player p, Menus.AdminHolder h, int slot) {
        if (!p.hasPermission("kitsystem.admin")) return;
        Kit kit = h.kit;
        switch (slot) {
            case 10 -> {
                kit.captureFrom(p);
                plugin.kits().save();
                plugin.stands().refreshAll(kit);
                KitSystem.msg(p, "Kit " + kit.displayName + " aus deinem Inventar aktualisiert.");
                p.closeInventory();
            }
            case 11 -> {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType().isAir()) {
                    KitSystem.msg(p, "Du musst ein Item in der Hand halten.");
                    return;
                }
                kit.customIcon = hand.clone();
                kit.customIcon.setAmount(1);
                plugin.kits().save();
                plugin.menus().openAdmin(p, kit);
            }
            case 12 -> {
                kit.customIcon = null;
                plugin.kits().save();
                plugin.menus().openAdmin(p, kit);
            }
            case 13 -> giveStand(p, kit);
            case 14 -> {
                plugin.kits().give(p, kit);
                p.closeInventory();
            }
            case 16 -> {
                if (!h.confirmDelete) {
                    h.confirmDelete = true;
                    ItemStack it = h.inv.getItem(16);
                    ItemMeta m = it.getItemMeta();
                    m.displayName(net.kyori.adventure.text.Component.text("Wirklich löschen? Nochmal klicken!",
                            NamedTextColor.DARK_RED).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                    it.setItemMeta(m);
                } else {
                    plugin.stands().removeAll(kit.id);
                    plugin.kits().remove(kit);
                    KitSystem.msg(p, "Kit " + kit.displayName + " gelöscht.");
                    p.closeInventory();
                }
            }
            default -> { }
        }
    }

    public static void giveStand(Player p, Kit kit) {
        KitSystem pl = KitSystem.getPlugin(KitSystem.class);
        Map<Integer, ItemStack> left = p.getInventory().addItem(pl.stands().createItem(kit));
        left.values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
        KitSystem.msg(p, "Du hast den Armorstand für " + kit.displayName + " erhalten.");
    }
}
