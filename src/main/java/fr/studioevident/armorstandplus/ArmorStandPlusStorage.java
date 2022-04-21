package fr.studioevident.armorstandplus.utils;

import fr.studioevident.armorstandplus.ArmorStandPlus;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ArmorStandPlusStorage {
    private final ArmorStandPlus plugin;
    private final NamespacedKey ownerNamespacedKey;
    private final NamespacedKey lockedNamespacedKey;
    private final NamespacedKey usersNamespaceKey;

    public ArmorStandPlusStorage(ArmorStandPlus plugin) {
        this.plugin = plugin;
        this.ownerNamespacedKey = new NamespacedKey(plugin, "owner");
        this.usersNamespaceKey = new NamespacedKey(plugin, "users");
        this.lockedNamespacedKey = new NamespacedKey(plugin, "locked");
    }

    public boolean isLocked(Entity entity) {
        if (!(entity instanceof ArmorStand)) return false;
        return isLocked((ArmorStand)entity);
    }

    public boolean isLocked(ArmorStand entity) {
        Byte lockedData = entity.getPersistentDataContainer().get(lockedNamespacedKey, PersistentDataType.BYTE);

        if (lockedData == null) return false;

        return lockedData == 1;
    }

    public UUID getArmorStandOwner(ArmorStand entity) {
        String ownerData = entity.getPersistentDataContainer().get(ownerNamespacedKey, PersistentDataType.STRING);

        if (ownerData == null) return null;
        return UUID.fromString(ownerData);
    }

    // setArmorStandOwner:
    // set the uuid of the player who placed the armor stand into persistent data of the armor stand

    public void setArmorStandOwner(ArmorStand armorStand, UUID owner) {
        armorStand.getPersistentDataContainer().set(ownerNamespacedKey, PersistentDataType.STRING, owner.toString());
    }

    public void lockArmorStand(ArmorStand armorStand) {
        armorStand.getPersistentDataContainer().set(lockedNamespacedKey, PersistentDataType.BYTE, (byte)1);
    }

    public void unlockArmorStand(ArmorStand armorStand) {
        armorStand.getPersistentDataContainer().set(lockedNamespacedKey, PersistentDataType.BYTE, (byte)0);
    }

    public List<UUID> getArmorStandUsers(ArmorStand armorStand) {
        String usersData = armorStand.getPersistentDataContainer().get(usersNamespaceKey, PersistentDataType.STRING);
        if (usersData == null || usersData.length() == 0) return new ArrayList<>();
        return Arrays.stream(usersData.split(";")).map(UUID::fromString).collect(Collectors.toList());
    }

    public void addUser(ArmorStand armorStand, UUID user) {
        List<UUID> usersData = getArmorStandUsers(armorStand);
        usersData.add(user);
        armorStand.getPersistentDataContainer().set(usersNamespaceKey, PersistentDataType.STRING, usersData.stream().map(UUID::toString).collect(Collectors.joining(";")));
    }

    public void removeUser(ArmorStand armorStand, UUID user) {
        List<UUID> usersData = getArmorStandUsers(armorStand);
        usersData.remove(user);
        armorStand.getPersistentDataContainer().set(usersNamespaceKey, PersistentDataType.STRING, usersData.stream().map(UUID::toString).collect(Collectors.joining(";")));
    }
}
