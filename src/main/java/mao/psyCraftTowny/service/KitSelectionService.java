package mao.psyCraftTowny.service;

import mao.psyCraftTowny.model.KitType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KitSelectionService {
    private static final String KIT_GUI_TITLE = "Выбор кита";
    private static final int KIT_GUI_SIZE = 54;
    private static final int KIT_SWORDSMAN_SLOT = 10;
    private static final int KIT_ARCHER_SLOT = 13;
    private static final int KIT_ENGINEER_SLOT = 16;
    private static final int KIT_SUPPORT_SLOT = 34;
    private static final int KIT_CROSSBOW_SLOT = 22;
    private static final int KIT_TANK_SLOT = 19;
    private static final int KIT_NINJA_SLOT = 28;
    private static final int KIT_TRAPPER_SLOT = 25;
    private static final int KIT_MEDIC_SLOT = 43;
    private final Set<UUID> kitMenuOpen = ConcurrentHashMap.newKeySet();
    private final Map<UUID, KitType> kitByPlayer = new ConcurrentHashMap<>();

    public String getKitGuiTitle() {
        return KIT_GUI_TITLE;
    }

    public void closeTab(Player player) {
        kitMenuOpen.remove(player.getUniqueId());
    }

    public boolean isKitSelectorItem(ItemStack stack) {
        if (stack == null || stack.getType() != Material.BLAZE_POWDER) {
            return false;
        }
        if (!stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.hasDisplayName() && "§dВыбор кита".equals(meta.getDisplayName());
    }

    public void openTab(Player player) {
        Inventory inv = Bukkit.createInventory(null, KIT_GUI_SIZE, KIT_GUI_TITLE);
        KitType selected = kitByPlayer.getOrDefault(player.getUniqueId(), KitType.SWORDSMAN);
        inv.setItem(KIT_SWORDSMAN_SLOT, buildKitMenuItem(KitType.SWORDSMAN, selected == KitType.SWORDSMAN));
        inv.setItem(KIT_ARCHER_SLOT, buildKitMenuItem(KitType.ARCHER, selected == KitType.ARCHER));
        inv.setItem(KIT_ENGINEER_SLOT, buildKitMenuItem(KitType.ENGINEER, selected == KitType.ENGINEER));
        inv.setItem(KIT_SUPPORT_SLOT, buildKitMenuItem(KitType.SUPPORT, selected == KitType.SUPPORT));
        inv.setItem(KIT_CROSSBOW_SLOT, buildKitMenuItem(KitType.CROSSBOWMAN, selected == KitType.CROSSBOWMAN));
        inv.setItem(KIT_TANK_SLOT, buildKitMenuItem(KitType.TANK, selected == KitType.TANK));
        inv.setItem(KIT_NINJA_SLOT, buildKitMenuItem(KitType.NINJA, selected == KitType.NINJA));
        inv.setItem(KIT_TRAPPER_SLOT, buildKitMenuItem(KitType.TRAPPER, selected == KitType.TRAPPER));
        inv.setItem(KIT_MEDIC_SLOT, buildKitMenuItem(KitType.MEDIC, selected == KitType.MEDIC));
        player.openInventory(inv);
    }

    public void selectDefaultKitForNewPlayer(Player player) {
        kitByPlayer.putIfAbsent(player.getUniqueId(), KitType.SWORDSMAN);
    }

    public boolean isKitMenuOpen(Player player) {
        return player != null && kitMenuOpen.contains(player.getUniqueId());
    }

    public KitType getSelectedKit(Player player) {
        return kitByPlayer.getOrDefault(player.getUniqueId(), KitType.SWORDSMAN);
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

    public void selectKit(Player player, KitType kitType) {
        kitByPlayer.put(player.getUniqueId(), kitType);
        player.sendMessage("§aВы выбрали кит: §f" + kitType.displayName());
    }

    public ItemStack buildKitMenuItem(KitType type, boolean selected) {
        ItemStack item = getItemStackByKitType(type);
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

    private ItemStack getItemStackByKitType(KitType type) {
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
        return new ItemStack(material);
    }

    public void giveKitSelectorItem(Player player) {
        ItemStack selector = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = selector.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§dВыбор кита");
            meta.setLore(List.of("§7Нажмите ПКМ в лобби"));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            selector.setItemMeta(meta);
        }
        player.getInventory().setItem(7, selector);
    }
}
