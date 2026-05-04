package mao.psyCraftTowny.service;

import mao.psyCraftTowny.model.Config;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class MapSelectionService {
    private static final String MAP_GUI_TITLE = "Выбор карты";
    private static final int MAP_GUI_SIZE = 27;
    private final Set<UUID> mapMenuOpen = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> playerMapVotesMap = new ConcurrentHashMap<>();

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

    public void openTab(Player player, Config config) {
        mapMenuOpen.add(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, MAP_GUI_SIZE, MAP_GUI_TITLE);
        final var slot = new AtomicInteger(10);
        final var votesMap = calculateVotes(config.getMaps().keySet().stream().toList());
        config.getMaps().forEach((mapCode, gameMap) -> {
            if (slot.get() < 17) {
                final var item = getItem(gameMap.menuItemKey());
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§fКарта: %s".formatted(gameMap.displayName()));
                    List<String> lore = new ArrayList<>();
                    lore.add("§eНажмите для выбора");
                    lore.add("Режим: %s".formatted(gameMap.mode().getRuName()));
                    lore.add("Голосов: %d".formatted(votesMap.getOrDefault(mapCode, 0)));
                    if (Objects.equals(playerMapVotesMap.get(player.getUniqueId()), mapCode)) {
                        lore.add("§aВы выбрали эту карту");
                    }
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(slot.get(), item);
                slot.incrementAndGet();
            }
        });
        player.openInventory(inv);
    }

    public boolean isMapMenuOpen(Player player) {
        return player != null && mapMenuOpen.contains(player.getUniqueId());
    }

    public void selectMap(Player player, ItemStack currentItem, Config config) {
        final var optionalSelectedMap = config.getMaps().values().stream()
                .filter(gameMap -> Objects.equals(gameMap.menuItemKey(), currentItem.getType().getKey().toString()))
                .findFirst();
        if (optionalSelectedMap.isEmpty()) {
            return;
        }
        final var selectedMap = optionalSelectedMap.get();
        playerMapVotesMap.put(player.getUniqueId(), selectedMap.code());
        openTab(player, config);
    }

    public void clearVotes() {
        playerMapVotesMap.clear();
    }

    public String resolveNextMap(Config config) {
        final var codes = config.getMaps().keySet().stream().toList();
        if (codes.isEmpty()) {
            return null;
        }
        if (codes.size() == 1) {
            return codes.getFirst();
        }
        if (playerMapVotesMap.isEmpty()) {
            return getRandomMapCode(codes);
        }
        final var votes = calculateVotes(codes);
        int maxVotes = 0;
        for (int count : votes.values()) {
            if (count > maxVotes) {
                maxVotes = count;
            }
        }

        if (maxVotes == 0) {
            return getRandomMapCode(codes);
        }

        List<String> winnerCandidates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : votes.entrySet()) {
            if (entry.getValue() == maxVotes) {
                winnerCandidates.add(entry.getKey());
            }
        }

        if (winnerCandidates.size() == 1) {
            return winnerCandidates.getFirst();
        }

        return getRandomMapCode(winnerCandidates);
    }

    private HashMap<String, Integer> calculateVotes(List<String> codes) {
        final var votes = new HashMap<String, Integer>();
        codes.forEach(code -> votes.put(code, 0));
        for (String votedCode : playerMapVotesMap.values()) {
            if (votes.containsKey(votedCode)) {
                votes.put(votedCode, votes.get(votedCode) + 1);
            }
        }
        return votes;
    }

    private ItemStack getItem(String key) {
        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        if (namespacedKey == null) return null;

        Material material = Registry.MATERIAL.get(namespacedKey);
        if (material == null) return null;

        return new ItemStack(material);
    }

    private String getRandomMapCode(List<String> codes) {
        return codes.get(ThreadLocalRandom.current().nextInt(codes.size()));
    }
}
