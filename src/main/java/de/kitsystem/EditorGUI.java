package de.kitsystem;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * KitEditor. Bedienung per "Antippen": erst ein Item antippen, dann den Zielslot antippen.
 * Dadurch funktioniert es genauso auf PC wie am Handy (Bedrock via Geyser / Touch) - kein Ziehen noetig.
 */
public class EditorGUI implements Listener {
    private final KitSystem pl;

    public EditorGUI(KitSystem pl) { this.pl = pl; }

    static class Holder implements InventoryHolder {
        Inventory inv;
        String kit;            // null = Kit-Auswahl
        int[] layout;
        int sel = -1;
        final String[] slotKits = new String[54];
        @Override public Inventory getInventory() { return inv; }
    }

    private static int guiSlot(int invSlot) { return invSlot < 9 ? invSlot + 36 : invSlot; }
    private static int invSlot(int gui) { return gui >= 36 ? gui - 36 : gui; }

    private ItemStack named(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Msg.c(name));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(Msg.c(s));
        meta.lore(l);
        meta.addItemFlags(ItemFlag.values());
        it.setItemMeta(meta);
        return it;
    }

    // ---------- Kit-Auswahl (Icon pro Kit) ----------

    public void openMenu(Player p) {
        Holder h = new Holder();
        Inventory inv = Bukkit.createInventory(h, 54, Msg.c("§bKitEditor"));
        h.inv = inv;
        boolean admin = p.hasPermission("kitsystem.admin");
        int i = 0;
        for (Kit k : pl.kits().all()) {
            if (!k.finished && !admin) continue;
            if (i >= 54) break;
            ItemStack it = named(k.icon, "§b" + k.name,
                    "§7Klicke zum §bBearbeiten §7des Layouts",
                    k.finished ? "" : "§c(noch nicht fertiggestellt)");
            inv.setItem(i, it);
            h.slotKits[i] = k.name;
            i++;
        }
        p.openInventory(inv);
    }

    // ---------- Layout-Editor ----------

    public void openLayout(Player p, Kit k) {
        Holder h = new Holder();
        h.kit = k.name;
        h.layout = pl.kits().getLayout(p.getUniqueId(), k);
        Inventory inv = Bukkit.createInventory(h, 54, Msg.c("§bKitEditor §8» §f" + k.name));
        h.inv = inv;
        render(h, k);
        p.openInventory(inv);
    }

    private void render(Holder h, Kit k) {
        Inventory inv = h.inv;
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, (i < 9 || i >= 45) ? filler : null);

        // Ruestung (nur Anzeige): Helm, Brust, Hose, Schuhe, Offhand
        for (int a = 0; a < 4; a++) if (k.armor[3 - a] != null) inv.setItem(a, k.armor[3 - a].clone());
        if (k.offhand != null) inv.setItem(4, k.offhand.clone());
        inv.setItem(6, named(Material.PAPER, "§bAnleitung",
                "§71. Item antippen", "§72. Zielslot antippen", "§7(tauscht die Plätze)"));
        inv.setItem(8, named(k.icon, "§b" + k.name, "§7Kit-Icon"));

        for (int t = 0; t < 36; t++) {
            int src = h.layout[t];
            ItemStack it = src >= 0 ? Kit.cl(k.contents[src]) : null;
            if (it != null && t == h.sel) {
                ItemMeta m = it.getItemMeta();
                m.setEnchantmentGlintOverride(true);
                List<Component> lore = m.lore() != null ? new ArrayList<>(m.lore()) : new ArrayList<>();
                lore.add(Msg.c("§a▶ Ausgewählt – Zielslot antippen"));
                m.lore(lore);
                it.setItemMeta(m);
            }
            inv.setItem(guiSlot(t), it);
        }
        inv.setItem(45, named(Material.ARROW, "§eZurück", "§7Zur Kit-Auswahl"));
        inv.setItem(49, named(Material.LAVA_BUCKET, "§cLayout zurücksetzen", "§7Standard-Anordnung"));
        inv.setItem(53, named(Material.BARRIER, "§cSchließen"));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder h)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        int s = e.getSlot();

        if (h.kit == null) {
            String kn = s >= 0 && s < 54 ? h.slotKits[s] : null;
            Kit k = pl.kits().get(kn);
            if (k != null) {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
                openLayout(p, k);
            }
            return;
        }

        Kit k = pl.kits().get(h.kit);
        if (k == null) { p.closeInventory(); return; }

        if (s == 45) { openMenu(p); return; }
        if (s == 53) { p.closeInventory(); return; }
        if (s == 49) {
            pl.kits().resetLayout(p.getUniqueId(), k);
            h.layout = pl.kits().defaultLayout(k);
            h.sel = -1;
            render(h, k);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.8f);
            return;
        }
        if (s < 9 || s > 44) return;

        int t = invSlot(s);
        if (h.sel == -1) {
            if (h.layout[t] >= 0) h.sel = t;
        } else if (h.sel == t) {
            h.sel = -1;
        } else {
            int tmp = h.layout[h.sel];
            h.layout[h.sel] = h.layout[t];
            h.layout[t] = tmp;
            h.sel = -1;
            pl.kits().setLayout(p.getUniqueId(), k, h.layout);
        }
        render(h, k);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }
}
