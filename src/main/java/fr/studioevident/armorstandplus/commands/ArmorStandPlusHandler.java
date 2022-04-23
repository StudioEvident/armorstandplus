package fr.studioevident.armorstandplus.commands;

import fr.studioevident.armorstandplus.ArmorStandPlus;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;

import java.util.*;

public class ArmorStandPlusHandler {
    private final ArmorStandPlus plugin;

    public ArmorStandPlusHandler(ArmorStandPlus plugin) {
        this.plugin = plugin;
    }

    public boolean doAction(Player player, String command, boolean isLeftClick) {
        Entity targetEntity = plugin.getTargetEntity(player);

        if (!command.equals("glow") && !(targetEntity instanceof ArmorStand)) return true;

        if (!command.equals("glow") &&
            !command.equals("copy") &&
            !plugin.hasAccess(player, (ArmorStand)targetEntity)) {
                plugin.sendMessage(player, "error-not-owner");
                return true;
        }

        ArmorStand armorStand = (ArmorStand)targetEntity;
        Location location = armorStand == null ? new Location(player.getWorld(), 0, 0, 0) : armorStand.getLocation();

        switch (command) {
            case "lock":
                plugin.lockArmorStand(player, armorStand);
                return true;

            case "unlock":
                plugin.unlockArmorStand(player, armorStand);
                return true;

            case "toggleuser":
                String userName = player.getInventory().getItemInOffHand().getItemMeta().getDisplayName();

                if (userName.equals(player.getName())) {
                    errorParticles(armorStand);
                    plugin.sendMessage(player, "error-you-are-owner");
                }

                OfflinePlayer[] allPlayers = Bukkit.getOfflinePlayers();
                for (OfflinePlayer offlinePlayer : allPlayers) {
                    if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(userName)) {
                        plugin.toggleUserArmorStand(player, offlinePlayer, armorStand);
                        return true;
                    }
                }

                errorParticles(armorStand);
                plugin.sendMessage(player, "error-player-does-not-exist", "{NAME}", userName);
                return true;


            case "copy":
                ItemStack itemStack = player.getInventory().getItemInOffHand();
                ItemStack book = new ItemStack(Material.WRITTEN_BOOK);

                BookMeta copyMeta = (BookMeta)book.getItemMeta();
                copyMeta.setTitle("ArmorStand Configuration");
                copyMeta.setAuthor("TheMisterObvious");

                List<String> armorStandProperties = new ArrayList<>();

                // Secure book with impossible character
                armorStandProperties.add("§9ArmorStandCopy");

                // Boolean properties
                armorStandProperties.add(
                    armorStand.hasArms() +
                    ";" + armorStand.hasBasePlate() +
                    ";" + armorStand.hasGravity() +
                    ";" + armorStand.isSmall() +
                    ";" + armorStand.isVisible()
                );

                // EulerAngle properties
                armorStandProperties.add(armorStand.getHeadPose().getX() + ":" + armorStand.getHeadPose().getY() + ":" + armorStand.getHeadPose().getZ());
                armorStandProperties.add(armorStand.getBodyPose().getX() + ":" + armorStand.getBodyPose().getY() + ":" + armorStand.getBodyPose().getZ());
                armorStandProperties.add(armorStand.getRightArmPose().getX() + ":" + armorStand.getRightArmPose().getY() + ":" + armorStand.getRightArmPose().getZ());
                armorStandProperties.add(armorStand.getLeftArmPose().getX() + ":" + armorStand.getLeftArmPose().getY() + ":" + armorStand.getLeftArmPose().getZ());
                armorStandProperties.add(armorStand.getRightLegPose().getX() + ":" + armorStand.getRightLegPose().getY() + ":" + armorStand.getRightLegPose().getZ());
                armorStandProperties.add(armorStand.getLeftLegPose().getX() + ":" + armorStand.getLeftLegPose().getY() + ":" + armorStand.getLeftLegPose().getZ());

                // Float properties
                armorStandProperties.add(String.valueOf(armorStand.getLocation().getYaw()));

                // Set the book with all the armor stand properties
                copyMeta.setPages(armorStandProperties);
                book.setItemMeta(copyMeta);

                // Replace the book and quill with the copy book
                if (itemStack.getType().toString().equals(plugin.getConfig().getString("itemModifiers.copy", ""))) player.getInventory().setItemInOffHand(book);
                successParticles(armorStand);
                return true;

            case "paste":
                ItemStack itemInOffHand = player.getInventory().getItemInOffHand();
                BookMeta pasteMeta = (BookMeta)itemInOffHand.getItemMeta();

                if (pasteMeta == null) {
                    errorParticles(armorStand);
                    return true;
                }

                if (pasteMeta.getPageCount() != 9 || !pasteMeta.getAuthor().equals("TheMisterObvious") || !pasteMeta.getPage(1).equals("§9ArmorStandCopy")) {
                    errorParticles(armorStand);
                    return true;
                }

                String[] booleanProps = pasteMeta.getPage(2).split(";");

                // Set all properties to the targeted armor stand.
                if (Boolean.parseBoolean(booleanProps[0]) != armorStand.hasArms()) armorStand.setArms(Boolean.parseBoolean(booleanProps[0]));
                if (Boolean.parseBoolean(booleanProps[1]) != armorStand.hasBasePlate()) armorStand.setBasePlate(Boolean.parseBoolean(booleanProps[1]));
                if (Boolean.parseBoolean(booleanProps[2]) != armorStand.hasGravity()) armorStand.setGravity(Boolean.parseBoolean(booleanProps[2]));
                if (Boolean.parseBoolean(booleanProps[3]) != armorStand.isSmall()) armorStand.setSmall(Boolean.parseBoolean(booleanProps[3]));
                if (Boolean.parseBoolean(booleanProps[4]) != armorStand.isVisible()) armorStand.setVisible(Boolean.parseBoolean(booleanProps[4]));

                String[] headPos = pasteMeta.getPage(3).split(":");
                String[] bodyPos = pasteMeta.getPage(4).split(":");
                String[] rightArmPos = pasteMeta.getPage(5).split(":");
                String[] leftArmPos = pasteMeta.getPage(6).split(":");
                String[] rightLegPos = pasteMeta.getPage(7).split(":");
                String[] leftLegPos = pasteMeta.getPage(8).split(":");

                EulerAngle head = new EulerAngle(Double.parseDouble(headPos[0]), Double.parseDouble(headPos[1]), Double.parseDouble(headPos[2]));
                EulerAngle body = new EulerAngle(Double.parseDouble(bodyPos[0]), Double.parseDouble(bodyPos[1]), Double.parseDouble(bodyPos[2]));
                EulerAngle rightArm = new EulerAngle(Double.parseDouble(rightArmPos[0]), Double.parseDouble(rightArmPos[1]), Double.parseDouble(rightArmPos[2]));
                EulerAngle leftArm = new EulerAngle(Double.parseDouble(leftArmPos[0]), Double.parseDouble(leftArmPos[1]), Double.parseDouble(leftArmPos[2]));
                EulerAngle rightLeg = new EulerAngle(Double.parseDouble(rightLegPos[0]), Double.parseDouble(rightLegPos[1]), Double.parseDouble(rightLegPos[2]));
                EulerAngle leftLeg = new EulerAngle(Double.parseDouble(leftLegPos[0]), Double.parseDouble(leftLegPos[1]), Double.parseDouble(leftLegPos[2]));

                if (!head.equals(armorStand.getHeadPose())) armorStand.setHeadPose(head);
                if (!body.equals(armorStand.getBodyPose())) armorStand.setBodyPose(body);
                if (!rightArm.equals(armorStand.getRightArmPose())) armorStand.setRightArmPose(rightArm);
                if (!leftArm.equals(armorStand.getLeftArmPose())) armorStand.setLeftArmPose(leftArm);
                if (!rightLeg.equals(armorStand.getRightLegPose())) armorStand.setRightLegPose(rightLeg);
                if (!leftLeg.equals(armorStand.getLeftLegPose())) armorStand.setLeftLegPose(leftLeg);

                float rotation = Float.parseFloat(pasteMeta.getPage(9));
                if (armorStand.getLocation().getYaw() != rotation) armorStand.teleport(new Location(location.getWorld(), location.getX(), location.getY(), location.getZ(), rotation, location.getPitch()));
                successParticles(armorStand);
                return true;

            case "glow":
                int glowRadius = plugin.getConfig().getInt("glowRadius");
                int glowTime = plugin.getConfig().getInt("glowTime")*20;

                Location playerLoc = player.getLocation();
                Collection<Entity> nearbyEntities = playerLoc.getWorld().getNearbyEntities(playerLoc, glowRadius, glowRadius, glowRadius);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof ArmorStand) {
                        ArmorStand as = (ArmorStand)entity;
                        if (!as.isVisible() && plugin.hasAccess(player, as)) as.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowTime, 0, false, false));
                    }
                }
                return true;

            case "rename":
                String name = player.getInventory().getItemInOffHand().getItemMeta().getDisplayName();

                if (name.equals("") || armorStand.getName().replace("&", "§").equals(name)) name = null;

                if (name == null) {
                    armorStand.setCustomName("Armor Stand");
                    armorStand.setCustomNameVisible(false);
                } else {
                    name = name.replace("&", "§");
                    armorStand.setCustomName(name);
                    armorStand.setCustomNameVisible(true);
                }
                return true;


            case "move":
                Material itemInMainHand = player.getInventory().getItemInMainHand().getType();

                double x = location.getX();
                double y = location.getY();
                double z = location.getZ();

                double dx = 0;
                double dy = 0;
                double dz = 0;

                double distance = player.isSneaking() ? 0.1 : 1;
                distance = isLeftClick ? -distance : distance;

                if (itemInMainHand == Material.RED_WOOL) dx = distance;
                else if (itemInMainHand == Material.GREEN_WOOL) dy = distance;
                else if (itemInMainHand == Material.BLUE_WOOL) dz = distance;
                else return true;

                armorStand.teleport(new Location(location.getWorld(), x+dx, y+dy ,z+dz, location.getYaw(), location.getPitch()));
                return true;

            case "rotate":
                float newRotation = armorStand.getLocation().getYaw();
                newRotation = (newRotation == 180) ? -180 : newRotation;

                float rotat = player.isSneaking() ? 5 : 45;
                rotat = isLeftClick ? -rotat : rotat;

                newRotation += rotat;

                armorStand.teleport(new Location(location.getWorld(), location.getX(), location.getY(), location.getZ(), newRotation, location.getPitch()));
                return true;


            case "sethelmet":
                ItemStack playerItem = player.getInventory().getItemInMainHand();
                ItemStack armorStandItem = armorStand.getEquipment().getHelmet();

                player.getInventory().setItemInMainHand(armorStandItem);
                armorStand.getEquipment().setHelmet(playerItem);
                return true;

            case "head":
                itemInMainHand = player.getInventory().getItemInMainHand().getType();
                head = armorStand.getHeadPose();

                x = head.getX();
                y = head.getY();
                z = head.getZ();

                double movement = player.isSneaking() ? Math.toRadians(5) : Math.toRadians(45);

                if (itemInMainHand == Material.RED_WOOL) x = isLeftClick ? x-movement : x+movement;
                else if (itemInMainHand == Material.GREEN_WOOL) y = isLeftClick ? y-movement : y+movement;
                else if (itemInMainHand == Material.BLUE_WOOL) z = isLeftClick ? z-movement : z+movement;
                else return true;

                armorStand.setHeadPose(new EulerAngle(x, y, z));
                return true;

            case "body":
                itemInMainHand = player.getInventory().getItemInMainHand().getType();
                body = armorStand.getBodyPose();

                x = body.getX();
                y = body.getY();
                z = body.getZ();

                movement = player.isSneaking() ? Math.toRadians(5) : Math.toRadians(45);

                if (itemInMainHand == Material.RED_WOOL) x = isLeftClick ? x-movement : x+movement;
                else if (itemInMainHand == Material.GREEN_WOOL) y = isLeftClick ? y-movement : y+movement;
                else if (itemInMainHand == Material.BLUE_WOOL) z = isLeftClick ? z-movement : z+movement;
                else return true;

                armorStand.setBodyPose(new EulerAngle(x, y, z));
                return true;

            case "arm":
                itemInMainHand = player.getInventory().getItemInMainHand().getType();
                EulerAngle arm = isLeftClick ? armorStand.getLeftArmPose() : armorStand.getRightArmPose();

                x = arm.getX();
                y = arm.getY();
                z = arm.getZ();

                movement = player.isSneaking() ? Math.toRadians(5) : Math.toRadians(45);

                if (itemInMainHand == Material.RED_WOOL) x += movement;
                else if (itemInMainHand == Material.GREEN_WOOL) y += movement;
                else if (itemInMainHand == Material.BLUE_WOOL) z += movement;
                else return true;

                if (isLeftClick) armorStand.setLeftArmPose(new EulerAngle(x, y, z));
                else armorStand.setRightArmPose(new EulerAngle(x, y, z));
                return true;

            case "leg":
                itemInMainHand = player.getInventory().getItemInMainHand().getType();
                EulerAngle leg = isLeftClick ? armorStand.getLeftLegPose() : armorStand.getRightLegPose();

                x = leg.getX();
                y = leg.getY();
                z = leg.getZ();

                movement = player.isSneaking() ? Math.toRadians(5) : Math.toRadians(45);

                if (itemInMainHand == Material.RED_WOOL) x += movement;
                else if (itemInMainHand == Material.GREEN_WOOL) y += movement;
                else if (itemInMainHand == Material.BLUE_WOOL) z += movement;
                else return true;

                if (isLeftClick) armorStand.setLeftLegPose(new EulerAngle(x, y, z));
                else armorStand.setRightLegPose(new EulerAngle(x, y, z));
                return true;


            case "arms":
                armorStand.setArms(!armorStand.hasArms());
                successParticles(armorStand);
                return true;

            case "baseplate":
                armorStand.setBasePlate(!armorStand.hasBasePlate());
                successParticles(armorStand);
                return true;

            case "gravity":
                armorStand.setGravity(!armorStand.hasGravity());
                successParticles(armorStand);
                return true;

            case "small":
                armorStand.setSmall(!armorStand.isSmall());
                successParticles(armorStand);
                return true;

            case "visible":
                armorStand.setVisible(!armorStand.isVisible());
                successParticles(armorStand);
                return true;

            // If someone messed up with the config
            default:
                errorParticles(armorStand);
                plugin.sendMessage(player, "error-unknown-command");
                return true;
        }

    }

    public void successParticles(ArmorStand armorStand) {
        World world = armorStand.getWorld();
        Location location = armorStand.getLocation();
        Location eyeLocation = armorStand.getEyeLocation();

        Particle.DustOptions options = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.0F);

        for (int i = 0 ; i < 4 ; i++) {
            Location random = eyeLocation.add(new Random().nextDouble() - 0.5, new Random().nextDouble()*1.5 - 1.3, new Random().nextDouble() - 0.5);
            world.spawnParticle(Particle.REDSTONE, random, 5, options);
        }
    }

    public void errorParticles(ArmorStand armorStand) {
        World world = armorStand.getWorld();
        Location eyeLocation = armorStand.getEyeLocation();

        Particle.DustOptions options = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0F);

        for (int i = 0 ; i < 4 ; i++) {
            Location random = eyeLocation.add(new Random().nextDouble() - 0.5, new Random().nextDouble()*1.5 - 1.3, new Random().nextDouble() - 0.5);
            world.spawnParticle(Particle.REDSTONE, random, 5, options);
        }
    }

}
