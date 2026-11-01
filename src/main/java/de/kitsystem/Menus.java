package de.kitsystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Menus {
    // ---------- Holder (zum Erkennen unserer Menues) ----------
    public static final class ListHolder implements InventoryHolder {
        public final Map<Integer, String> kitBySlot = new HashMap<>();
        Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    public static final class LayoutHolder implements InventoryHolder {
        public final Kit kit;
        public final int[] map;
        public int selected = -1; // GUI-Slot 0-35
        Inventory inv;
        public LayoutHolder(Kit kit, int[] map) { this.kit = kit; this.map = map; }
        @Override public Inventory getInventory() { return inv; }
    }

    public static final class AdminHolder implements InventoryHolder {
        public final Kit kit;
        public boolean confirmDelete = false;
        Inventory inv;
        public AdminHolder(Kit kit) { this.kit = kit; }
        @Override public Inventory getInventory() { return inv; }
    }

    public static boolean isOurs(InventoryHolder h) {
        return h instanceof ListHolder || h instanceof LayoutHolder || h instanceof AdminHolder;
    }

    // GUI-Slot (Reihe 0-2 = Inventar, Reihe 3 = Hotbar) -> echter Inventar-Slot
    public static int toInv(int gui) {
        return gui < 27 ? gui + 9 : gui - 27;
    }

    private final KitSystem plugin;

    public Menus(KitSystem plugin) { this.plugin = plugin; }

    public static ItemStack named(Material m, String name, NamedTextColor col, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name, col).decoration(TextDecoration.ITALIC, false));
        if (lore.length > 0) {
            meta.lore(Arrays.stream(lore)
                    .map(l -> Component.text(l, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .toList());
        }
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack filler() {
        return named(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
    }

    // ---------- KitEditor: Liste aller Kits mit Icon ----------
    public void openList(Player p) {
        ListHolder h = new ListHolder();
        h.inv = Bukkit.createInventory(h, 54, Component.text("KitEditor", NamedTextColor.AQUA));
        int slot = 0;
        for (Kit k : plugin.kits().all()) {
            if (slot >= 45) break;
            h.inv.setItem(slot, plugin.icons().menuIcon(k));
            h.kitBySlot.put(slot, k.id);
            slot++;
        }
        if (slot == 0) {
            h.inv.setItem(22, named(Material.BARRIER, "Noch keine Kits vorhanden", NamedTextColor.RED));
        }
        p.openInventory(h.inv);
    }

    // ---------- Layout-Editor (Tippen -> Tippen, funktioniert auch am Handy) ----------
    public void openLayout(Player p, Kit kit) {
        LayoutHolder h = new LayoutHolder(kit, plugin.kits().getLayout(p.getUniqueId(), kit.id));
        h.inv = Bukkit.createInventory(h, 54, Component.text("Layout: ", NamedTextColor.AQUA)
                .append(Component.text(kit.displayName, NamedTextColor.WHITE)));
        render(h);
        p.openInventory(h.inv);
    }

    public void render(LayoutHolder h) {
        Inventory inv = h.inv;
        for (int g = 0; g < 36; g++) {
            ItemStack src = h.kit.storage[h.map[toInv(g)]];
            if (src == null) {
                inv.setItem(g, null);
            } else {
                ItemStack c = src.clone();
                if (g == h.selected) {
                    ItemMeta m = c.getItemMeta();
                    m.setEnchantmentGlintOverride(true);
                    c.setItemMeta(m);
                }
                inv.setItem(g, c);
            }
        }
        for (int i = 36; i < 54; i++) inv.setItem(i, filler());
        // Ruestung / Offhand nur zur Ansicht
        for (int i = 0; i < 4; i++) {
            ItemStack a = h.kit.armor[3 - i];
            if (a != null) inv.setItem(36 + i, a.clone());
        }
        if (h.kit.offhand != null) inv.setItem(40, h.kit.offhand.clone());
        inv.setItem(44, named(Material.BOOK, "So geht's", NamedTextColor.YELLOW,
                "1. Item antippen (leuchtet)", "2. Zielslot antippen -> Tausch",
                "Gespeichert wird automatisch."));
        inv.setItem(45, named(Material.REDSTONE, "Zurücksetzen", NamedTextColor.RED, "Standard-Layout wiederherstellen"));
        inv.setItem(49, named(Material.EMERALD, "Fertig", NamedTextColor.GREEN, "Menü schließen"));
        inv.setItem(53, named(Material.ARROW, "Zurück", NamedTextColor.AQUA, "Zur Kit-Liste"));
    }

    // ---------- Admin-Menue eines Kits ----------
    public void openAdmin(Player p, Kit kit) {
        AdminHolder h = new AdminHolder(kit);
        h.inv = Bukkit.createInventory(h, 27, Component.text("Kit: ", NamedTextColor.GOLD)
                .append(Component.text(kit.displayName, NamedTextColor.WHITE)));
        for (int i = 0; i < 27; i++) h.inv.setItem(i, filler());
        h.inv.setItem(4, plugin.icons().menuIcon(kit));
        h.inv.setItem(10, named(Material.CHEST, "Aus meinem Inventar übernehmen", NamedTextColor.GREEN,
                "Rüstung, Items & Offhand werden neu gespeichert", "Platzierte Ständer aktualisieren sich"));
        h.inv.setItem(11, named(Material.ITEM_FRAME, "Icon = Item in der Hand", NamedTextColor.YELLOW,
                "Eigenes Icon für das Layout-Menü"));
        h.inv.setItem(12, named(Material.NAME_TAG, "Icon automatisch (Name)", NamedTextColor.YELLOW,
                "Icon wird aus dem Kit-Namen erzeugt"));
        h.inv.setItem(13, named(Material.ARMOR_STAND, "Armorstand erhalten", NamedTextColor.GOLD,
                "Zum Platzieren oder Umplatzieren"));
        h.inv.setItem(14, named(Material.DIAMOND_SWORD, "Kit testen", NamedTextColor.AQUA, "Gibt dir das Kit"));
        h.inv.setItem(16, named(Material.TNT, "Kit löschen", NamedTextColor.RED, "Zweimal klicken zum Bestätigen"));
        p.openInventory(h.inv);
    }
}
