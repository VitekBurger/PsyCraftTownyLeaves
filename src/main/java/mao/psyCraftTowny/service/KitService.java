package mao.psyCraftTowny.service;

import mao.psyCraftTowny.model.KitType;
import org.bukkit.Material;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class KitService {
    public ItemStack buildKitMenuItem(KitType type, boolean selected) {
        Material material = switch (type) {
            case SWORDSMAN -> Material.IRON_SWORD;
            case ARCHER -> Material.BOW;
            case ENGINEER -> Material.TNT;
            case SUPPORT -> Material.GOLDEN_APPLE;
            case CROSSBOWMAN -> Material.CROSSBOW;
            case TANK -> Material.SHIELD;
            case NINJA -> Material.ENDER_PEARL;
            case TRAPPER -> Material.COBWEB;
            case MEDIC -> Material.POTION;
        };
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§fКит: " + type.displayName());
            List<String> lore = new ArrayList<>();
            switch (type) {
                case SWORDSMAN -> lore.add("§7Железный меч, блоки, лава, железный сет, стейки");
                case ARCHER -> lore.add("§7Кольчужный сет, лук, 2 стака стрел, деревянный меч, лава, стейки, блоки");
                case ENGINEER -> lore.add("§7ТНТ, вода, редстоун-механизмы, полублоки, кожаный сет");
                case SUPPORT -> lore.add("§7Поддержка: золотые яблоки, еда, усиления и броня");
                case CROSSBOWMAN -> lore.add("§7Арбалет, болты, фейерверки и мобильный бой");
                case TANK -> lore.add("§7Толстая броня, щит и фронтлайн");
                case NINJA -> lore.add("§7Скорость, перлы и быстрый раш");
                case TRAPPER -> lore.add("§7Паутина, ловушки и контроль проходов");
                case MEDIC -> lore.add("§7Лечение союзников и выживаемость");
            }
            lore.add("§eНажмите для выбора");
            if (selected) {
                lore.add("§aВы выбрали этот кит");
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void applyKit(Player player, KitType type) {
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
    }

    public KitType resolveKitTypeByMenuItem(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }
        String name = meta.getDisplayName();
        for (KitType type : KitType.values()) {
            if (name.contains(type.displayName())) {
                return type;
            }
        }
        return switch (stack.getType()) {
            case IRON_SWORD -> KitType.SWORDSMAN;
            case BOW -> KitType.ARCHER;
            case TNT -> KitType.ENGINEER;
            case GOLDEN_APPLE -> KitType.SUPPORT;
            case CROSSBOW -> KitType.CROSSBOWMAN;
            case SHIELD -> KitType.TANK;
            case ENDER_PEARL -> KitType.NINJA;
            case COBWEB -> KitType.TRAPPER;
            case POTION -> KitType.MEDIC;
            default -> null;
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
        player.getInventory().addItem(new ItemStack(Material.STONE_AXE, 1));
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
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 5));
        player.getInventory().addItem(createCustomPotion("§aЗелье отравления II", PotionEffectType.POISON, 20 * 22, 1, 2));
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
        player.getInventory().addItem(createCrossbowFireworks(16));
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
        player.getInventory().addItem(new ItemStack(Material.COBWEB, 20));
        player.getInventory().addItem(new ItemStack(Material.TRIPWIRE_HOOK, 12));
        player.getInventory().addItem(new ItemStack(Material.STRING, 32));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 24));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 32));
        player.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
    }

    private void applyMedicKit(Player player) {
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD, 1));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_PICKAXE, 1));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 20));
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 3));
        player.getInventory().addItem(createCustomPotion("§dЗелье моментального исцеления", PotionEffectType.INSTANT_HEALTH, 1, 0, 3));
        player.getInventory().addItem(createCustomPotion("§aЗелье регенерации", PotionEffectType.REGENERATION, 20 * 45, 0, 3));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 40));
        player.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
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

    private ItemStack createCrossbowFireworks(int amount) {
        ItemStack item = new ItemStack(Material.FIREWORK_ROCKET, amount);
        ItemMeta rawMeta = item.getItemMeta();
        if (rawMeta instanceof FireworkMeta meta) {
            meta.setPower(1);
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(Color.ORANGE, Color.RED)
                    .withFade(Color.YELLOW)
                    .trail(true)
                    .flicker(true)
                    .build());
            item.setItemMeta(meta);
        }
        return item;
    }
}
