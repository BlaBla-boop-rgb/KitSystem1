package de.kitsystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.*;

/**
 * Erzeugt das Icon eines Kits automatisch anhand des Namens:
 * Helm-Material + Rüstungsbesatz (Trim) kommen deterministisch aus dem Namen,
 * Schlüsselwörter im Namen (z.B. "gold", "netherite", "eisen") legen das Material fest.
 * Gleicher Name = immer gleiches Icon.
 */
public final class IconFactory {
    private static final Map<String, Material> KEYWORDS = new LinkedHashMap<>();
    private static final Material[] DEFAULTS = {
            Material.IRON_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET,
            Material.GOLDEN_HELMET, Material.CHAINMAIL_HELMET, Material.LEATHER_HELMET
    };

    static {
        KEYWORDS.put("netherite", Material.NETHERITE_HELMET);
        KEYWORDS.put("diamond", Material.DIAMOND_HELMET);
        KEYWORDS.put("diamant", Material.DIAMOND_HELMET);
        KEYWORDS.put("gold", Material.GOLDEN_HELMET);
        KEYWORDS.put("iron", Material.IRON_HELMET);
        KEYWORDS.put("eisen", Material.IRON_HELMET);
        KEYWORDS.put("chain", Material.CHAINMAIL_HELMET);
        KEYWORDS.put("kette", Material.CHAINMAIL_HELMET);
        KEYWORDS.put("leather", Material.LEATHER_HELMET);
        KEYWORDS.put("leder", Material.LEATHER_HELMET);
        KEYWORDS.put("turtle", Material.TURTLE_HELMET);
        KEYWORDS.put("schildkr", Material.TURTLE_HELMET);
    }

    public ItemStack base(Kit kit) {
        if (kit.customIcon != null) return kit.customIcon.clone();
        return generated(kit.displayName);
    }

    public ItemStack generated(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        long seed = 7;
        for (char c : n.toCharArray()) seed = seed * 131 + c;
        Random r = new Random(seed);

        Material mat = DEFAULTS[r.nextInt(DEFAULTS.length)];
        for (Map.Entry<String, Material> e : KEYWORDS.entrySet()) {
            if (n.contains(e.getKey())) { mat = e.getValue(); break; }
        }

        List<TrimPattern> patterns = new ArrayList<>();
        Registry.TRIM_PATTERN.forEach(patterns::add);
        patterns.sort(Comparator.comparing(p -> String.valueOf(Registry.TRIM_PATTERN.getKey(p))));
        List<TrimMaterial> trimMats = new ArrayList<>();
        Registry.TRIM_MATERIAL.forEach(trimMats::add);
        trimMats.sort(Comparator.comparing(m -> String.valueOf(Registry.TRIM_MATERIAL.getKey(m))));

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof LeatherArmorMeta lm) {
            lm.setColor(Color.fromRGB(r.nextInt(256), r.nextInt(256), r.nextInt(256)));
        }
        if (meta instanceof ArmorMeta am && !patterns.isEmpty() && !trimMats.isEmpty()) {
            am.setTrim(new ArmorTrim(trimMats.get(r.nextInt(trimMats.size())),
                    patterns.get(r.nextInt(patterns.size()))));
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Icon fuer die Kit-Liste im KitEditor. */
    public ItemStack menuIcon(Kit kit) {
        ItemStack it = base(kit);
        it.setAmount(1);
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.text(kit.displayName, NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        m.lore(List.of(Component.text("Klicke zum ", NamedTextColor.GRAY)
                .append(Component.text("Bearbeiten", NamedTextColor.AQUA))
                .append(Component.text(" des Layouts", NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false)));
        it.setItemMeta(m);
        return it;
    }
}
