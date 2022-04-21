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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class ArmorStandPlusListeners implements Listener {
    private final ArmorStandPlus plugin;
    private final ArmorStandPlusStorage storage;
    private final ArmorStandPlusHandler handler;

    public ArmorStandPlusListeners(ArmorStandPlus plugin, ArmorStandPlusStorage storage, ArmorStandPlusHandler handler) {
        this.plugin = plugin;
        this.storage = storage;
        this.handler = handler;
    }

    // Set the owner of an armor stand to the nearest player
    @EventHandler
    public void onEntitySpawn(final EntitySpawnEvent event) {
        Location location = event.getLocation();

        if (!(event.getEntity() instanceof ArmorStand)) return;
        ArmorStand armorStand = (ArmorStand)event.getEntity();

        Collection<Entity> nearbyEntities = location.getWorld().getNearbyEntities(armorStand.getLocation(), 8, 8, 8);
        if (!(nearbyEntities.iterator().hasNext())) return;

        double distance = 10000000;
        Entity nearby = nearbyEntities.iterator().next();
        for (Entity e : nearbyEntities) {
            if (e instanceof Player) {
                Location l = e.getLocation();

                double testDistance = location.distance(l);
                if (testDistance < distance) {
                    distance = testDistance;
                    nearby = e;
                }
            }
        }

        if (!(nearby instanceof Player)) return;
        Player nearestPlayer = (Player)nearby;

        storage.setArmorStandOwner(armorStand, nearestPlayer.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStandModification(final PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();
        ArmorStand armorStand = event.getRightClicked();

        if (plugin.hasAccess(player, armorStand)) {
            EquipmentSlot slot = event.getSlot();

            if (player.isSneaking()) {
                ItemStack playerItem = event.getPlayerItem();
                ItemStack armorStandItem = event.getRightClicked().getEquipment().getItemInOffHand();

                player.getInventory().setItemInMainHand(armorStandItem);
                armorStand.getEquipment().setItemInOffHand(playerItem);

                event.setCancelled(true);
            }

            return;
        }

        if (!storage.isLocked(armorStand)) return;

        plugin.sendMessage(player, "armor-stand-locked", "{NAME}", Bukkit.getOfflinePlayer(storage.getArmorStandOwner(armorStand)).getName());
        event.setCancelled(true);
    }

    // Get when ever a locked armor stand take damage from
    // an entity other than a player (tnt, skeleton, etc...)
    @EventHandler(ignoreCancelled = true)
    public void onArmorStandDamage(final EntityDamageEvent event) {
        Entity entity = event.getEntity();

        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        if (!storage.isLocked(entity)) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStandDamageByEntity(final EntityDamageByEntityEvent event) {
        Entity damagingEntity = event.getDamager();
        Entity entity = event.getEntity();
        
        if (!(damagingEntity instanceof Player)) return;
        Player player = (Player)damagingEntity;

        if (!(entity instanceof ArmorStand)) return;
        ArmorStand armorStand = (ArmorStand)entity;

        if (plugin.hasAccess(player, armorStand)) {
            Material item = player.getInventory().getItemInOffHand().getType();

            if (item == Material.AIR) return;
            String commandToExecute = plugin.isModifierItem(item);

            if (commandToExecute == null) return;

            handler.doAction(player, commandToExecute, true);
            event.setCancelled(true);
        } else {
            if (!storage.isLocked(entity)) return;

            plugin.sendMessage(player, "armor-stand-locked", "{NAME}", Bukkit.getOfflinePlayer(storage.getArmorStandOwner(armorStand)).getName());
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStandInteraction(final PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Material item = player.getInventory().getItemInOffHand().getType();

        // If item in offhand is not air, check is the item is associate to an action.
        if (item == Material.AIR) return;
        String commandToExecute = plugin.isModifierItem(item);

        // If the item is not associate to an action, return.
        if (commandToExecute == null) return;

        // Do the associate action and cancel the event.
        handler.doAction(player, commandToExecute, false);
        event.setCancelled(true);
    }

    // This event is only here to handle the glow command activation.
    // So players don't have to look at an INVISIBLE armor stand.
    @EventHandler(ignoreCancelled = true)
    public void onGlowUsedInAir(final PlayerInteractEvent event) {
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
