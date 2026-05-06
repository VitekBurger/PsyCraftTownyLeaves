package mao.psyCraftTowny.service;

import mao.psyCraftTowny.PsyCraftTowny;
import mao.psyCraftTowny.model.KitType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

import static mao.psyCraftTowny.service.MiniGameService.BLUE_TEAM;
import static mao.psyCraftTowny.service.MiniGameService.RED_TEAM;

public class PlayerItemsService {
    private final PsyCraftTowny plugin;

    public PlayerItemsService(PsyCraftTowny plugin) {
        this.plugin = plugin;
    }

    public void giveAllItems(Player player, KitType type, Integer teamId) {
        switch (type) {
            case SWORDSMAN -> applySwordsmanKit(player);
            case ARCHER -> applyArcherKit(player);
            case ENGINEER -> applyEngineerKit(player);
            case SUPPORT -> applySupportKit(player);
            case CROSSBOWMAN -> applyCrossbowKit(player);
            case TANK -> applyTankKit(player);
            case NINJA -> applyNinjaKit(player);
            case TRAPPER -> applyTrapperKit(player);
            case MEDIC -> applyMedicKit(player);
        }

        if (type == KitType.CROSSBOWMAN) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, false, false));
        }

        if (shouldHaveShield(type)) {
            giveTeamShield(player, teamId);
        } else {
            player.getInventory().setItemInOffHand(null);
        }
    }

    private boolean shouldHaveShield(KitType type) {
        return switch (type) {
            case ARCHER -> false;
            default -> true;
        };
    }

    private void applySwordsmanKit(Player player) {
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD, 1));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_PICKAXE, 1));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));
        player.getInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 1));
        player.getInventory().addItem(new ItemStack(Material.WATER_BUCKET, 1));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 32));
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
    }

    private void applyArcherKit(Player player) {
        player.getInventory().addItem(new ItemStack(Material.BOW, 1));
        player.getInventory().addItem(new ItemStack(Material.ARROW, 128));
        player.getInventory().addItem(new ItemStack(Material.IRON_PICKAXE, 1));
        player.getInventory().addItem(new ItemStack(Material.STONE_AXE, 1));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 16));
        player.getInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 1));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 32));
        player.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
    }

    private void applyEngineerKit(Player player) {
        player.getInventory().addItem(new ItemStack(Material.STONE_SWORD, 1));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_PICKAXE, 1));
        player.getInventory().addItem(new ItemStack(Material.TNT, 10));
        player.getInventory().addItem(new ItemStack(Material.FLINT_AND_STEEL, 1));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 32));
        player.getInventory().addItem(new ItemStack(Material.WATER_BUCKET, 1));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE_SLAB, 64));
        player.getInventory().addItem(new ItemStack(Material.STONE_BUTTON, 16));
        player.getInventory().addItem(new ItemStack(Material.FISHING_ROD, 1));
        player.getInventory().addItem(new ItemStack(Material.SLIME_BLOCK, 8));
        player.getInventory().addItem(new ItemStack(Material.OAK_BUTTON, 16));
        player.getInventory().addItem(new ItemStack(Material.LEVER, 16));
        player.getInventory().addItem(new ItemStack(Material.REDSTONE, 64));
        player.getInventory().addItem(new ItemStack(Material.REPEATER, 16));
        player.getInventory().addItem(new ItemStack(Material.PISTON, 16));
        player.getInventory().addItem(new ItemStack(Material.STICKY_PISTON, 16));
        player.getInventory().addItem(new ItemStack(Material.OBSERVER, 12));
        player.getInventory().addItem(new ItemStack(Material.DISPENSER, 8));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 96));
        player.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));
    }

    private void applySupportKit(Player player) {
        player.getInventory().addItem(new ItemStack(Material.STONE_SWORD, 1));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_PICKAXE, 1));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 24));
        player.getInventory().addItem(createCustomPotion("§aЗелье отравления I", PotionEffectType.POISON, 20 * 22, 0, 2));
        player.getInventory().addItem(createCustomPotion("§cЗелье моментального урона I", PotionEffectType.INSTANT_DAMAGE, 1, 0, 2));
        player.getInventory().addItem(createCustomPotion("§7Зелье слабости", PotionEffectType.WEAKNESS, 20 * 90, 0, 2));
        player.getInventory().addItem(createCustomPotion("§6Зелье огнестойкости", PotionEffectType.FIRE_RESISTANCE, 20 * 180, 0, 2));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 40));
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
    }

    private void applyCrossbowKit(Player player) {
        player.getInventory().addItem(new ItemStack(Material.CROSSBOW, 1));
        player.getInventory().addItem(new ItemStack(Material.ARROW, 96));
        player.getInventory().addItem(createCrossbowFireworks(16, 1));
        player.getInventory().addItem(createCrossbowFireworks(1, 5)); // Ult
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD, 1));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_PICKAXE, 1));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 20));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 32));
        player.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
    }

    private void applyTankKit(Player player) {
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD, 1));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_PICKAXE, 1));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 24));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 40));
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        
        // Ультра-замедление (в 2.5 раза медленнее обычного: 0.2 / 2.5 = 0.08)
        player.setWalkSpeed(0.08F);
        player.setSprinting(false);
        
        // Сопротивление II (amplifier 1)
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1000000, 1, false, false, false));
    }

    private void applyNinjaKit(Player player) {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD, 1));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_PICKAXE, 1));
        player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 3));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 24));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 32));
        player.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));
    }

    private void applyTrapperKit(Player player) {
        player.getInventory().addItem(new ItemStack(Material.STONE_SWORD, 1));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_PICKAXE, 1));
        player.getInventory().addItem(new ItemStack(Material.FISHING_ROD, 1));
        player.getInventory().addItem(new ItemStack(Material.COBWEB, 20));
        player.getInventory().addItem(new ItemStack(Material.TRIPWIRE_HOOK, 12));
        player.getInventory().addItem(new ItemStack(Material.STRING, 32));
        player.getInventory().addItem(new ItemStack(Material.STONE_PRESSURE_PLATE, 16));
        player.getInventory().addItem(new ItemStack(Material.WIND_CHARGE, 10));
        player.getInventory().addItem(new ItemStack(Material.REDSTONE, 32));
        player.getInventory().addItem(createCustomPotion("§8Зелье слепоты", PotionEffectType.BLINDNESS, 20 * 10, 0, 1));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 24));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 32));
        player.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
    }

    private void applyMedicKit(Player player) {
        player.getInventory().addItem(new ItemStack(Material.STONE_SWORD, 1));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_PICKAXE, 1));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 20));
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 5));
        player.getInventory().addItem(createCustomLingeringPotion("§dОседающее зелье исцеления", PotionEffectType.INSTANT_HEALTH, 1, 0, 5));
        player.getInventory().addItem(createCustomLingeringPotion("§aОседающее зелье регенерации", PotionEffectType.REGENERATION, 20 * 15, 0, 3));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 40));
        player.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));
    }

    private ItemStack createCustomPotion(String name, PotionEffectType type, int durationTicks, int amplifier, int amount) {
        ItemStack item = new ItemStack(Material.SPLASH_POTION, amount);
        ItemMeta rawMeta = item.getItemMeta();
        if (rawMeta instanceof PotionMeta meta) {
            meta.setDisplayName(name);
            meta.addCustomEffect(new PotionEffect(type, durationTicks, amplifier), true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCustomLingeringPotion(String name, PotionEffectType type, int durationTicks, int amplifier, int amount) {
        ItemStack item = new ItemStack(Material.LINGERING_POTION, amount);
        ItemMeta rawMeta = item.getItemMeta();
        if (rawMeta instanceof PotionMeta meta) {
            meta.setDisplayName(name);
            meta.addCustomEffect(new PotionEffect(type, durationTicks, amplifier), true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCrossbowFireworks(int amount, int fireworkAmount) {
        ItemStack item = new ItemStack(Material.FIREWORK_ROCKET, amount);
        ItemMeta rawMeta = item.getItemMeta();
        if (rawMeta instanceof FireworkMeta meta) {
            meta.setPower(1);
            FireworkEffect effect = FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(Color.ORANGE, Color.RED)
                    .withFade(Color.YELLOW)
                    .trail(true)
                    .flicker(true)
                    .build();
            for (int i = 0; i < fireworkAmount; i++) {
                meta.addEffect(effect);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void giveTeamShield(Player player, Integer teamId) {
        if (teamId == null) {
            return;
        }
        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta itemMeta = shield.getItemMeta();
        if (itemMeta instanceof BlockStateMeta meta) {
            Banner banner = (Banner) meta.getBlockState();
            DyeColor teamColor = teamId == RED_TEAM ? DyeColor.RED : DyeColor.BLUE;
            banner.setBaseColor(teamColor);
            banner.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
            meta.setBlockState(banner);
            if (teamId == RED_TEAM) {
                meta.setDisplayName("§cЩит Красных");
            } else if (teamId == BLUE_TEAM) {
                meta.setDisplayName("§9Щит Синих");
            } else {
                meta.setDisplayName("§fКомандный щит");
            }
            meta.setLore(List.of("§7Щит вашей команды"));
            shield.setItemMeta(meta);
        }
        player.getInventory().setItemInOffHand(shield);
        if (!hasTeamShieldInInventory(player)) {
            player.getInventory().addItem(shield.clone());
        }
        player.updateInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.getGameMode() != GameMode.SPECTATOR) {
                ItemStack off = player.getInventory().getItemInOffHand();
                if (off == null || off.getType() != Material.SHIELD) {
                    player.getInventory().setItemInOffHand(shield.clone());
                    player.updateInventory();
                }
            }
        }, 2L);
    }

    private boolean hasTeamShieldInInventory(Player player) {
        if (player == null) {
            return false;
        }
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() != Material.SHIELD || !stack.hasItemMeta()) {
                continue;
            }
            ItemMeta meta = stack.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Щит")) {
                return true;
            }
        }
        return false;
    }
}
