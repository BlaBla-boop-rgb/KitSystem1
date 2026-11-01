package de.kitsystem;

import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KitListener implements Listener {
    private final KitSystem pl;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public KitListener(KitSystem pl) { this.pl = pl; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (pl.getConfig().getBoolean("give-editor-on-join", true) && p.hasPermission("kitsystem.use")) {
            pl.giveEditorItem(p);
        }
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack it = e.getItem();
        if (it == null || !it.hasItemMeta()) return;
        Player p = e.getPlayer();
        PersistentDataContainer pdc = it.getItemMeta().getPersistentDataContainer();

        if (pdc.has(pl.editorKey, PersistentDataType.BYTE)) {
            e.setCancelled(true);
            if (p.hasPermission("kitsystem.use")) pl.editor().openMenu(p);
            return;
        }

        String kitName = pdc.get(pl.standItemKey, PersistentDataType.STRING);
        if (kitName == null) return;
        e.setCancelled(true);
        if (a != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;

        Kit kit = pl.kits().get(kitName);
        if (kit == null) { Msg.send(p, "§cDieses Kit existiert nicht mehr."); return; }

        Block target = e.getClickedBlock().getRelative(e.getBlockFace());
        if (!target.isPassable() && !target.isEmpty()) { Msg.send(p, "§cHier ist kein Platz."); return; }
        Location loc = target.getLocation().add(0.5, 0, 0.5);
        if (e.getBlockFace() == BlockFace.DOWN) loc.add(0, -1, 0);
        loc.setYaw(Math.round((p.getLocation().getYaw() + 180f) / 45f) * 45f);

        pl.stands().spawn(loc, kit, p.getUniqueId());
        if (p.getGameMode() != GameMode.CREATIVE) it.setAmount(it.getAmount() - 1);
        Msg.send(p, "§aKit-Ständer §b" + kit.name + " §aplatziert. §7(Shift + Rechtsklick zum Umsetzen)");
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (pl.isEditorItem(e.getItemInHand())) e.setCancelled(true);
    }

    @EventHandler
    public void onStand(PlayerInteractAtEntityEvent e) {
        if (!(e.getRightClicked() instanceof ArmorStand as)) return;
        String kn = pl.stands().kitOf(as);
        if (kn == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        boolean admin = p.hasPermission("kitsystem.admin");
        Kit kit = pl.kits().get(kn);

        if (p.isSneaking()) { // aufheben / umsetzen
            String owner = as.getPersistentDataContainer().get(pl.standOwnerKey, PersistentDataType.STRING);
            if (!admin && !p.getUniqueId().toString().equals(owner)) {
                Msg.send(p, "§cDu darfst diesen Ständer nicht bewegen.");
                return;
            }
            as.remove();
            if (kit != null) {
                KitSystem.give(p, pl.stands().standItem(kit));
                Msg.send(p, "§aStänder aufgehoben. Platziere ihn neu, wo du willst.");
            }
            return;
        }

        if (kit == null) { Msg.send(p, "§cDieses Kit existiert nicht mehr."); return; }
        if (!p.hasPermission("kitsystem.use") || (!kit.finished && !admin)) {
            Msg.send(p, "§cDu kannst dieses Kit nicht nehmen.");
            return;
        }
        int cd = kit.cooldown >= 0 ? kit.cooldown : pl.getConfig().getInt("kit-cooldown-seconds", 5);
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(p.getUniqueId());
        if (last != null && now - last < cd * 1000L) {
            Msg.send(p, "§cBitte warte noch kurz.");
            return;
        }
        cooldowns.put(p.getUniqueId(), now);
        pl.kits().giveKit(p, kit);
        Msg.send(p, "Kit §b" + kit.name + " §7erhalten.");
    }

    @EventHandler
    public void onManipulate(PlayerArmorStandManipulateEvent e) {
        if (pl.stands().kitOf(e.getRightClicked()) != null) e.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof ArmorStand as && pl.stands().kitOf(as) != null) e.setCancelled(true);
    }

    /** Staender nach Chunk-Laden / Neustart auf den aktuellen Kit-Stand bringen. */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        for (Entity en : e.getEntities()) {
            if (en instanceof ArmorStand as) {
                String kn = pl.stands().kitOf(as);
                if (kn == null) continue;
                Kit k = pl.kits().get(kn);
                if (k != null) pl.stands().apply(as, k);
            }
        }
    }
}
