package de.kitsystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class KitSystem extends JavaPlugin {
    private KitManager kits;
    private IconFactory icons;
    private StandManager stands;
    private Menus menus;
    private NamespacedKey editorItemKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        editorItemKey = new NamespacedKey(this, "editor_item");
        kits = new KitManager(this);
        kits.load();
        icons = new IconFactory();
        stands = new StandManager(this);
        menus = new Menus(this);

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new StandListener(this), this);

        KitCommand cmd = new KitCommand(this);
        getCommand("kit").setExecutor(cmd);
        getCommand("kit").setTabCompleter(cmd);
        getCommand("kiteditor").setExecutor(cmd);
        getLogger().info(kits.all().size() + " Kit(s) geladen.");
    }

    public KitManager kits() { return kits; }
    public IconFactory icons() { return icons; }
    public StandManager stands() { return stands; }
    public Menus menus() { return menus; }

    public static void msg(CommandSender s, String text) {
        s.sendMessage(Component.text("[Kits] ", NamedTextColor.AQUA).append(Component.text(text, NamedTextColor.GRAY)));
    }

    // ---------- Amboss-Item (oeffnet den KitEditor) ----------

    public ItemStack createEditorItem() {
        ItemStack it = new ItemStack(Material.ANVIL);
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.text("KitEditor", NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        m.lore(List.of(Component.text("Rechtsklick: Layout & Inventar sortieren", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        m.getPersistentDataContainer().set(editorItemKey, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(m);
        return it;
    }

    public boolean isEditorItem(ItemStack it) {
        return it != null && it.hasItemMeta()
                && it.getItemMeta().getPersistentDataContainer().has(editorItemKey, PersistentDataType.BYTE);
    }

    public void giveEditorItem(Player p) {
        if (!getConfig().getBoolean("anvil-item.enabled", true)) return;
        PlayerInventory inv = p.getInventory();
        for (ItemStack it : inv.getContents()) if (isEditorItem(it)) return;
        int slot = Math.max(0, Math.min(8, getConfig().getInt("anvil-item.slot", 8)));
        ItemStack cur = inv.getItem(slot);
        if (cur == null || cur.getType().isAir()) inv.setItem(slot, createEditorItem());
    }
}
