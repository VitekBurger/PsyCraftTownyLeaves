package mao.psyCraftTowny.maps;

import mao.psyCraftTowny.config.Config;
import mao.psyCraftTowny.maps.GameMap;
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
    private final Map<UUID, GameMap.Mode> playerModeVotesMap = new ConcurrentHashMap<>();

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
            meta.setDisplayName("§dВыбор карты и режима");
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
        return meta != null && meta.hasDisplayName() && "§dВыбор карты и режима".equals(meta.getDisplayName());
    }

    public void openTab(Player player, Config config) {
        mapMenuOpen.add(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, MAP_GUI_SIZE, MAP_GUI_TITLE);
        
        // Карта - слоты 0-8
        final var mapSlot = new AtomicInteger(0);
        final var votesMap = calculateMapVotes(config.getMaps().keySet().stream().toList());
        config.getMaps().forEach((mapCode, gameMap) -> {
            if (mapSlot.get() < 9) {
                final var item = getItem(gameMap.menuItemKey());
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§fКарта: %s".formatted(gameMap.displayName()));
                    List<String> lore = new ArrayList<>();
                    lore.add("§eНажмите для выбора");
                    lore.add("Голосов: %d".formatted(votesMap.getOrDefault(mapCode, 0)));
                    if (Objects.equals(playerMapVotesMap.get(player.getUniqueId()), mapCode)) {
                        lore.add("§aВы выбрали эту карту");
                    }
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(mapSlot.get(), item);
                mapSlot.incrementAndGet();
            }
        });

        // Режим - слоты 18-20
        GameMap.Mode[] modes = GameMap.Mode.values();
        Map<GameMap.Mode, Integer> modeVotes = calculateModeVotes();
        for (int i = 0; i < modes.length; i++) {
            GameMap.Mode mode = modes[i];
            ItemStack item = new ItemStack(getModeMaterial(mode));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§bРежим: %s".formatted(mode.getRuName()));
                List<String> lore = new ArrayList<>();
                lore.add("§eНажмите для выбора");
                lore.add("Голосов: %d".formatted(modeVotes.getOrDefault(mode, 0)));
                if (playerModeVotesMap.get(player.getUniqueId()) == mode) {
                    lore.add("§aВы выбрали этот режим");
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(18 + i, item);
        }

        player.openInventory(inv);
    }

    private Material getModeMaterial(GameMap.Mode mode) {
        return switch (mode) {
            case TDM -> Material.IRON_SWORD;
            case CP -> Material.BEACON;
            case FFA -> Material.GOLDEN_AXE;
        };
    }

    public boolean isMapMenuOpen(Player player) {
        return player != null && mapMenuOpen.contains(player.getUniqueId());
    }

    public void handleGuiClick(Player player, ItemStack currentItem, Config config) {
        if (currentItem == null || currentItem.getType() == Material.AIR) return;

        // Клик по карте
        final var optionalSelectedMap = config.getMaps().values().stream()
                .filter(gameMap -> Objects.equals(gameMap.menuItemKey(), currentItem.getType().getKey().toString()))
                .findFirst();
        
        if (optionalSelectedMap.isPresent()) {
            playerMapVotesMap.put(player.getUniqueId(), optionalSelectedMap.get().code());
            openTab(player, config);
            return;
        }

        // Клик по режиму
        for (GameMap.Mode mode : GameMap.Mode.values()) {
            if (currentItem.getType() == getModeMaterial(mode)) {
                playerModeVotesMap.put(player.getUniqueId(), mode);
                openTab(player, config);
                return;
            }
        }
    }

    public void clearVotes() {
        playerMapVotesMap.clear();
        playerModeVotesMap.clear();
    }

    public String resolveNextMap(Config config) {
        final var codes = config.getMaps().keySet().stream().toList();
        if (codes.isEmpty()) return null;
        if (codes.size() == 1) return codes.getFirst();
        if (playerMapVotesMap.isEmpty()) return getRandomMapCode(codes);

        final var votes = calculateMapVotes(codes);
        return resolveWinner(votes, codes);
    }

    public GameMap.Mode resolveNextMode() {
        if (playerModeVotesMap.isEmpty()) return GameMap.Mode.CP; // По умолчанию CP

        Map<GameMap.Mode, Integer> votes = calculateModeVotes();
        List<GameMap.Mode> candidates = new ArrayList<>();
        int max = -1;

        for (Map.Entry<GameMap.Mode, Integer> entry : votes.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                candidates.clear();
                candidates.add(entry.getKey());
            } else if (entry.getValue() == max) {
                candidates.add(entry.getKey());
            }
        }

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private String resolveWinner(Map<String, Integer> votes, List<String> codes) {
        int maxVotes = -1;
        List<String> winners = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : votes.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winners.clear();
                winners.add(entry.getKey());
            } else if (entry.getValue() == maxVotes) {
                winners.add(entry.getKey());
            }
        }
        return winners.get(ThreadLocalRandom.current().nextInt(winners.size()));
    }

    private Map<String, Integer> calculateMapVotes(List<String> codes) {
        final var votes = new HashMap<String, Integer>();
        codes.forEach(code -> votes.put(code, 0));
        for (String votedCode : playerMapVotesMap.values()) {
            if (votes.containsKey(votedCode)) {
                votes.put(votedCode, votes.get(votedCode) + 1);
            }
        }
        return votes;
    }

    private Map<GameMap.Mode, Integer> calculateModeVotes() {
        Map<GameMap.Mode, Integer> votes = new HashMap<>();
        for (GameMap.Mode mode : GameMap.Mode.values()) votes.put(mode, 0);
        for (GameMap.Mode votedMode : playerModeVotesMap.values()) {
            votes.put(votedMode, votes.get(votedMode) + 1);
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
