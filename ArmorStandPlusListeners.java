package fr.studioevident.armorstandplus.listeners;

import fr.studioevident.armorstandplus.ArmorStandPlus;
import fr.studioevident.armorstandplus.commands.ArmorStandPlusHandler;
import fr.studioevident.armorstandplus.utils.ArmorStandPlusStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class ArmorStandPlusListeners implements Listener {
    private final ArmorStandPlus plugin;
    private final ArmorStandPlusStorage storage;
    private final ArmorStandPlusHandler handler;

    public ArmorStandPlusListeners(ArmorStandPlus plugin, ArmorStandPlusStorage storage, ArmorStandPlusHandler handler) {
        this.plugin = plugin;
        this.storage = storage;
        this.handler = handler;
    }

    // Set the owner of each armor stands in the world, for now
    // this is getting the nearest player but upgrade in coming
    @EventHandler
    public void onEntitySpawn(final @NotNull EntitySpawnEvent event) {
        Location location = event.getLocation();

        if (!plugin.isLockable(event.getEntity())) return;
        ArmorStand armorStand = (ArmorStand)event.getEntity();

        Player nearestPlayer = plugin.getNearestPlayerAround(location, 8);

        if (nearestPlayer == null) return;

        storage.setArmorStandOwner(armorStand, nearestPlayer.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStandDamage(final @NotNull EntityDamageEvent event) {
        Entity entity = event.getEntity();

        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        if (!storage.isLocked(entity)) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStandModification(final @NotNull PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        if (!storage.isLocked(entity)) return;
        ArmorStand armorStand = (ArmorStand)entity;

        if (plugin.isOwner(player, armorStand)) {
            // Do some sneaking actions
            return;
        }

        plugin.sendMessage(player, "armor-stand-locked", "{NAME}", Bukkit.getOfflinePlayer(storage.getArmorStandOwner(armorStand)).getName());
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStandDamageByEntity(final @NotNull EntityDamageByEntityEvent event) {
        Entity damagingEntity = event.getDamager();
        Entity entity = event.getEntity();
        
        if (!(damagingEntity instanceof Player)) return;
        Player player = (Player)damagingEntity;

        if (!(entity instanceof ArmorStand)) return;
        ArmorStand armorStand = (ArmorStand)entity;

        if (plugin.isOwner(player, armorStand)) {
            Material item = player.getInventory().getItemInOffHand().getType();

            if (item == Material.AIR) return;
            String commandToExecute = plugin.isModifierItem(item);

            if (commandToExecute == null) return;

            handler.doAction(player, commandToExecute, true);
            event.setCancelled(true);
        }

        if (!storage.isLocked(entity)) return;

        plugin.sendMessage(player, "armor-stand-locked", "{NAME}", Bukkit.getOfflinePlayer(storage.getArmorStandOwner(armorStand)).getName());
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStandInteraction(final @NotNull PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Material item = player.getInventory().getItemInOffHand().getType();

        if (item == Material.AIR) return;
        String commandToExecute = plugin.isModifierItem(item);

        if (commandToExecute == null) return;

        handler.doAction(player, commandToExecute, false);
        event.setCancelled(true);
    }

    // This event is only here to handle the glow command activation.
    // So players don't have to look at an INVISIBLE armor stand.
    @EventHandler
    public void onGlowUsedInAir(final @NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR) return;

        Material itemInMainHand = player.getInventory().getItemInMainHand().getType();

        if (itemInMainHand != Material.AIR) return;

        Material itemInOffHand = player.getInventory().getItemInOffHand().getType();
        if (!itemInOffHand.toString().equals(plugin.getConfig().getString("itemModifiers.glow", ""))) return;

        handler.doAction(player, "glow", false);
        event.setCancelled(true);
    }
}
