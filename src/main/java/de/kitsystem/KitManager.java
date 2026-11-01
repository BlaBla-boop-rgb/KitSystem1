package de.kitsystem;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public final class KitManager {
    public static final Pattern VALID_ID = Pattern.compile("^[\\p{L}0-9_-]{1,24}$");
    public static final Set<String> RESERVED = Set.of("create", "edit", "update", "rename", "icon",
            "stand", "give", "delete", "list");

    private final KitSystem plugin;
    private final Map<String, Kit> kits = new LinkedHashMap<>();
    private final File kitFile;
    private final File playerFile;
    private YamlConfiguration playerCfg;

    public KitManager(KitSystem plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.kitFile = new File(plugin.getDataFolder(), "kits.yml");
        this.playerFile = new File(plugin.getDataFolder(), "layouts.yml");
    }

    public static String toId(String name) {
        return name.trim().replace(' ', '_').toLowerCase(Locale.ROOT);
    }

    // ---------- Laden / Speichern ----------

    public void load() {
        kits.clear();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(kitFile);
        ConfigurationSection sec = cfg.getConfigurationSection("kits");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                ConfigurationSection k = sec.getConfigurationSection(id);
                if (k == null) continue;
                Kit kit = new Kit(id, k.getString("display", id));
                List<String> st = k.getStringList("storage");
                for (int i = 0; i < 36 && i < st.size(); i++) kit.storage[i] = dec(st.get(i));
                List<String> ar = k.getStringList("armor");
                for (int i = 0; i < 4 && i < ar.size(); i++) kit.armor[i] = dec(ar.get(i));
                kit.offhand = dec(k.getString("offhand", ""));
                kit.customIcon = dec(k.getString("icon", ""));
                kits.put(id, kit);
            }
        }
        playerCfg = YamlConfiguration.loadConfiguration(playerFile);
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Kit kit : kits.values()) {
            String p = "kits." + kit.id + ".";
            cfg.set(p + "display", kit.displayName);
            List<String> st = new ArrayList<>();
            for (ItemStack i : kit.storage) st.add(enc(i));
            cfg.set(p + "storage", st);
            List<String> ar = new ArrayList<>();
            for (ItemStack i : kit.armor) ar.add(enc(i));
            cfg.set(p + "armor", ar);
            cfg.set(p + "offhand", enc(kit.offhand));
            cfg.set(p + "icon", enc(kit.customIcon));
        }
        try {
            cfg.save(kitFile);
        } catch (IOException e) {
            plugin.getLogger().severe("kits.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    private void savePlayers() {
        try {
            playerCfg.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("layouts.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    static String enc(ItemStack i) {
        if (i == null || i.getType().isAir()) return "";
        return Base64.getEncoder().encodeToString(i.serializeAsBytes());
    }

    static ItemStack dec(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(s));
        } catch (Exception e) {
            return null;
        }
    }

    // ---------- Kits ----------

    public Kit get(String id) {
        return id == null ? null : kits.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Kit> all() {
        return kits.values();
    }

    public void add(Kit kit) {
        kits.put(kit.id, kit);
        save();
    }

    public void remove(Kit kit) {
        kits.remove(kit.id);
        save();
        ConfigurationSection sec = playerCfg.getConfigurationSection("layouts");
        if (sec != null) {
            for (String uuid : sec.getKeys(false)) playerCfg.set("layouts." + uuid + "." + kit.id, null);
            savePlayers();
        }
    }

    // ---------- Layout pro Spieler ----------
    // map[zielSlot] = Quell-Slot im Kit. Immer eine Permutation von 0-35.

    public int[] getLayout(UUID uuid, String kitId) {
        List<Integer> l = playerCfg.getIntegerList("layouts." + uuid + "." + kitId);
        int[] m = new int[36];
        boolean ok = l.size() == 36;
        boolean[] seen = new boolean[36];
        if (ok) {
            for (int i = 0; i < 36; i++) {
                int v = l.get(i);
                if (v < 0 || v > 35 || seen[v]) { ok = false; break; }
                seen[v] = true;
                m[i] = v;
            }
        }
        if (!ok) for (int i = 0; i < 36; i++) m[i] = i;
        return m;
    }

    public void saveLayout(UUID uuid, String kitId, int[] map) {
        List<Integer> l = new ArrayList<>(36);
        for (int v : map) l.add(v);
        playerCfg.set("layouts." + uuid + "." + kitId, l);
        savePlayers();
    }

    // ---------- Kit ausgeben ----------

    public void give(Player p, Kit kit) {
        int[] map = getLayout(p.getUniqueId(), kit.id);
        PlayerInventory inv = p.getInventory();
        inv.clear();
        for (int t = 0; t < 36; t++) {
            ItemStack it = kit.storage[map[t]];
            inv.setItem(t, it == null ? null : it.clone());
        }
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) armor[i] = kit.armor[i] == null ? null : kit.armor[i].clone();
        inv.setArmorContents(armor);
        inv.setItemInOffHand(kit.offhand == null ? null : kit.offhand.clone());
        plugin.giveEditorItem(p);
    }
}
