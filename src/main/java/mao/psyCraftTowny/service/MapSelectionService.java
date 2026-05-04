package mao.psyCraftTowny.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MapSelectionService {
    private static final String MAP_GUI_TITLE = "Выбор карты";
    private static final int MAP_GUI_SIZE = 27;
    private final Set<UUID> mapMenuOpen = ConcurrentHashMap.newKeySet();

    public void closeTab(Player player) {
        mapMenuOpen.remove(player.getUniqueId());
    }

    public String getMapGuiTitle() {
        return MAP_GUI_TITLE;
    }

    public void giveMapSelectorItem(Player player) {
        ItemStack selector = new ItemStack(Material.DEAD_BUSH);
        ItemMeta meta = selector.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§dВыбор карты");
            meta.setLore(List.of("§7Нажмите ПКМ в лобби"));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            selector.setItemMeta(meta);
        }
        player.getInventory().setItem(6, selector);
    }

    public boolean isMapSelectorItem(ItemStack stack) {
        if (stack == null || stack.getType() != Material.DEAD_BUSH) {
            return false;
        }
        if (!stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.hasDisplayName() && "§dВыбор карты".equals(meta.getDisplayName());
    }

    public void openTab(Player player) {
        mapMenuOpen.add(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, MAP_GUI_SIZE, MAP_GUI_TITLE);
        // TODO

        player.openInventory(inv);
    }

    public boolean isMapMenuOpen(Player player) {
        return player != null && mapMenuOpen.contains(player.getUniqueId());
    }

    public void selectMapByMenuItem(Player player, ItemStack currentItem) {

    }
}
