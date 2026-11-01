package de.kitsystem;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/** Ermittelt automatisch ein Icon anhand des Kit-Namens. */
public final class IconResolver {
    private IconResolver() {}

    public static Material resolve(String kitName, Kit kit, FileConfiguration cfg) {
        String lower = kitName.toLowerCase(Locale.ROOT);

        // 1) Schluesselwoerter aus der config.yml
        ConfigurationSection kw = cfg.getConfigurationSection("icon-keywords");
        if (kw != null) {
            for (String key : kw.getKeys(false)) {
                if (lower.contains(key.toLowerCase(Locale.ROOT))) {
                    Material m = Material.matchMaterial(kw.getString(key, ""));
                    if (ok(m)) return m;
                }
            }
        }
        // 2) Name entspricht direkt einem Material (z.B. "Apple", "Bow")
        String norm = lower.replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT);
        Material direct = Material.matchMaterial(norm);
        if (ok(direct)) return direct;

        // 3) Ungefaehre Suche: kuerzestes Material, das den Namen enthaelt
        if (norm.length() >= 3) {
            Material best = null;
            for (Material m : Material.values()) {
                if (!ok(m) || m.name().startsWith("LEGACY_")) continue;
                if (m.name().contains(norm) && (best == null || m.name().length() < best.name().length())) best = m;
            }
            if (best != null) return best;
        }
        // 4) Fallback: Helm des Kits, sonst erstes Item, sonst Namensschild
        if (kit != null) {
            if (kit.armor[3] != null) return kit.armor[3].getType();
            for (ItemStack i : kit.contents) if (i != null && ok(i.getType())) return i.getType();
            for (ItemStack i : kit.armor) if (i != null) return i.getType();
        }
        return Material.NAME_TAG;
    }

    private static boolean ok(Material m) {
        return m != null && m.isItem() && !m.isAir();
    }
}
