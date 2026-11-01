package de.kitsystem;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class KitCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBS = List.of("create", "edit", "update", "rename", "icon",
            "stand", "give", "delete", "list");
    private final KitSystem plugin;

    public KitCommand(KitSystem plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (c.getName().equalsIgnoreCase("kiteditor") || a.length == 0) {
            if (s instanceof Player p) plugin.menus().openList(p);
            else KitSystem.msg(s, "Nur für Spieler.");
            return true;
        }
        String sub = a[0].toLowerCase(Locale.ROOT);

        if (sub.equals("list")) {
            String names = String.join(", ", plugin.kits().all().stream().map(k -> k.displayName).toList());
            KitSystem.msg(s, "Kits: " + (names.isEmpty() ? "keine" : names));
            return true;
        }

        if (SUBS.contains(sub)) {
            if (!s.hasPermission("kitsystem.admin")) {
                KitSystem.msg(s, "Dafür fehlt dir die Berechtigung.");
                return true;
            }
            admin(s, sub, a);
            return true;
        }

        // /kit <name> -> Kit ausruesten
        if (!(s instanceof Player p)) return true;
        Kit kit = plugin.kits().get(KitManager.toId(String.join(" ", a)));
        if (kit == null) {
            KitSystem.msg(s, "Kit nicht gefunden. /kit list");
            return true;
        }
        plugin.kits().give(p, kit);
        KitSystem.msg(s, "Du hast das Kit " + kit.displayName + " erhalten.");
        return true;
    }

    private void admin(CommandSender s, String sub, String[] a) {
        if (a.length < 2) {
            KitSystem.msg(s, "Benutzung: /kit " + sub + " <name>");
            return;
        }

        if (sub.equals("create")) {
            if (!(s instanceof Player p)) { KitSystem.msg(s, "Nur für Spieler."); return; }
            String display = String.join(" ", Arrays.copyOfRange(a, 1, a.length));
            String id = KitManager.toId(display);
            if (!KitManager.VALID_ID.matcher(id).matches() || KitManager.RESERVED.contains(id)) {
                KitSystem.msg(s, "Ungültiger Name (max. 24 Zeichen, Buchstaben/Zahlen/_/-).");
                return;
            }
            if (plugin.kits().get(id) != null) {
                KitSystem.msg(s, "Ein Kit mit diesem Namen existiert bereits.");
                return;
            }
            Kit kit = new Kit(id, display);
            kit.captureFrom(p);
            plugin.kits().add(kit);
            GuiListener.giveStand(p, kit);
            KitSystem.msg(s, "Kit " + display + " erstellt! Es ist jetzt im KitEditor (Amboss) bearbeitbar. "
                    + "Platziere den Armorstand, wo du ihn haben willst.");
            return;
        }

        Kit kit = plugin.kits().get(a[1]);
        if (kit == null) {
            KitSystem.msg(s, "Kit nicht gefunden.");
            return;
        }

        switch (sub) {
            case "edit" -> {
                if (s instanceof Player p) plugin.menus().openAdmin(p, kit);
            }
            case "update" -> {
                if (!(s instanceof Player p)) return;
                kit.captureFrom(p);
                plugin.kits().save();
                plugin.stands().refreshAll(kit);
                KitSystem.msg(s, "Kit aus deinem Inventar aktualisiert.");
            }
            case "rename" -> {
                if (a.length < 3) { KitSystem.msg(s, "Benutzung: /kit rename <kit> <neuer Anzeigename>"); return; }
                kit.displayName = String.join(" ", Arrays.copyOfRange(a, 2, a.length));
                plugin.kits().save();
                plugin.stands().refreshAll(kit);
                KitSystem.msg(s, "Umbenannt in " + kit.displayName + ".");
            }
            case "icon" -> {
                if (a.length >= 3 && a[2].equalsIgnoreCase("reset")) {
                    kit.customIcon = null;
                } else if (s instanceof Player p && !p.getInventory().getItemInMainHand().getType().isAir()) {
                    kit.customIcon = p.getInventory().getItemInMainHand().clone();
                    kit.customIcon.setAmount(1);
                } else {
                    KitSystem.msg(s, "Item in die Hand nehmen oder: /kit icon <kit> reset");
                    return;
                }
                plugin.kits().save();
                KitSystem.msg(s, "Icon aktualisiert.");
            }
            case "stand" -> {
                if (s instanceof Player p) GuiListener.giveStand(p, kit);
            }
            case "give" -> {
                Player target = a.length >= 3 ? Bukkit.getPlayer(a[2]) : (s instanceof Player p ? p : null);
                if (target == null) { KitSystem.msg(s, "Spieler nicht gefunden."); return; }
                plugin.kits().give(target, kit);
                KitSystem.msg(s, "Kit " + kit.displayName + " an " + target.getName() + " gegeben.");
            }
            case "delete" -> {
                plugin.stands().removeAll(kit.id);
                plugin.kits().remove(kit);
                KitSystem.msg(s, "Kit " + kit.displayName + " gelöscht.");
            }
            default -> { }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (c.getName().equalsIgnoreCase("kiteditor")) return out;
        String cur = a[a.length - 1].toLowerCase(Locale.ROOT);
        if (a.length == 1) {
            out.addAll(SUBS);
            plugin.kits().all().forEach(k -> out.add(k.id));
        } else if (a.length == 2 && SUBS.contains(a[0].toLowerCase(Locale.ROOT)) && !a[0].equalsIgnoreCase("create")
                && !a[0].equalsIgnoreCase("list")) {
            plugin.kits().all().forEach(k -> out.add(k.id));
        } else if (a.length == 3 && a[0].equalsIgnoreCase("give")) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        } else if (a.length == 3 && a[0].equalsIgnoreCase("icon")) {
            out.add("reset");
        }
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(cur));
        return out;
    }
}
