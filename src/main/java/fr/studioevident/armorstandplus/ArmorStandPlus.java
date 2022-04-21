package fr.studioevident.armorstandplus;

import fr.studioevident.armorstandplus.commands.ArmorStandPlusHandler;
import fr.studioevident.armorstandplus.listeners.ArmorStandPlusListeners;
import fr.studioevident.armorstandplus.utils.ArmorStandPlusStorage;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ArmorStandPlus extends JavaPlugin {
    private final ArmorStandPlusStorage storage = new ArmorStandPlusStorage(this);
    private final ArmorStandPlusHandler handler = new ArmorStandPlusHandler(this);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new ArmorStandPlusListeners(this, storage, handler), this);
    }

    public void sendMessage(Player player, String configPath, Object... parameters) {
        String message = getConfig().getString("messages." + configPath, "");
        for (int i = 0; i < parameters.length - 1; i += 2) {
            message = message.replace(parameters[i].toString(), parameters[i + 1].toString());
        }
        player.spigot().sendMessage(TextComponent.fromLegacyText(message));
    }

    public boolean hasAccess(Player player, ArmorStand armorStand) {
        if (armorStand == null) return false;
        if (storage.getArmorStandOwner(armorStand) == null) return false;
        return storage.getArmorStandOwner(armorStand).equals(player.getUniqueId()) || storage.getArmorStandUsers(armorStand).contains(player.getUniqueId());
    }

    public String isModifierItem(Material item) {
        Set<String> itemModifiers = getConfig().getConfigurationSection("itemModifiers").getKeys(false);
        for (String path : itemModifiers) {
            if (getConfig().getString("itemModifiers." + path, "").equals(item.toString())) return path;
        }
        // Not in the config because it will make too much bug if its modified
        if (item == Material.WRITTEN_BOOK) return "paste";
        return null;
    }

    public Entity getTargetEntity(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection();

        RayTraceResult rayTraceResult = player.getWorld().rayTraceEntities(eyeLocation.add(direction), direction, 6);
        if (rayTraceResult == null) return null;

        return rayTraceResult.getHitEntity();
    }

    // Commands action:

    public void lockArmorStand(Player player, ArmorStand armorStand) {
        if (storage.isLocked(armorStand)) {
            handler.errorParticles(armorStand);
            sendMessage(player, "error-already-locked");
            return;
        }

        storage.lockArmorStand(armorStand);
        handler.successParticles(armorStand);
        sendMessage(player, "lock-success");
    }

    public void unlockArmorStand(Player player, ArmorStand armorStand) {
        if (!storage.isLocked(armorStand)) {
            handler.errorParticles(armorStand);
            sendMessage(player, "error-not-locked");
            return;
        }

        storage.unlockArmorStand(armorStand);
        handler.successParticles(armorStand);
        sendMessage(player, "unlock-success");
    }

    public void toggleUserArmorStand(Player owner, OfflinePlayer user, ArmorStand armorStand) {
        if (!storage.isLocked(armorStand)) {
            sendMessage(owner, "error-not-locked");
            return;
        }

        UUID userUUID = user.getUniqueId();
        List<UUID> allowedUsers = storage.getArmorStandUsers(armorStand);
        if (allowedUsers.contains(userUUID)) {
            storage.removeUser(armorStand, userUUID);
            sendMessage(owner, "lock-remove-success", "{NAME}", user.getName());
        } else {
            storage.addUser(armorStand, userUUID);
            sendMessage(owner, "lock-add-success", "{NAME}", user.getName());
        }
    }

}

