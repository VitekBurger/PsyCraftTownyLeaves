package mao.psyCraftTowny.teams;

import mao.psyCraftTowny.config.Config;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static mao.psyCraftTowny.service.MiniGameService.BLUE_TEAM;
import static mao.psyCraftTowny.service.MiniGameService.RED_TEAM;
import static mao.psyCraftTowny.utils.PlayerUtils.isObserver;

public class TeamSelectionService {
    private static final String TEAM_GUI_TITLE = "Выбор команды";
    private static final int TEAM_GUI_SIZE = 27;
    private static final int TEAM_ONE_SLOT = 11;
    private static final int TEAM_TWO_SLOT = 15;
    private final Set<UUID> teamMenuOpen = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> teamByPlayer = new ConcurrentHashMap<>();

    public void closeTab(Player player) {
        teamMenuOpen.remove(player.getUniqueId());
    }

    public String getTeamGuiTitle() {
        return TEAM_GUI_TITLE;
    }

    public void giveTeamSelectorItem(Player player) {
        ItemStack selector = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = selector.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§bВыбор команды");
            meta.setLore(List.of("§7Нажмите ПКМ в лобби"));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            selector.setItemMeta(meta);
        }
        player.getInventory().setItem(8, selector);
    }

    public boolean isTeamSelectorItem(ItemStack stack) {
        if (stack == null || stack.getType() != Material.NETHER_STAR) {
            return false;
        }
        if (!stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.hasDisplayName() && "§bВыбор команды".equals(meta.getDisplayName());
    }

    public void removeTeamSelection(Player player) {
        teamByPlayer.remove(player.getUniqueId());
    }

    public void clearAll() {
        teamByPlayer.clear();
    }

    public void openTab(Player player, Config config) {
        teamMenuOpen.add(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, TEAM_GUI_SIZE, TEAM_GUI_TITLE);
        inv.setItem(TEAM_ONE_SLOT, buildTeamItem(player, config, 1, Material.RED_WOOL));
        inv.setItem(TEAM_TWO_SLOT, buildTeamItem(player, config, 2, Material.BLUE_WOOL));
        player.openInventory(inv);
    }

    public Integer getSelectedTeam(Player player) {
        return teamByPlayer.get(player.getUniqueId());
    }

    public Integer getSelectedTeam(UUID playerId) {
        return teamByPlayer.get(playerId);
    }

    private boolean canJoinTeamWithBalance(Player player, int targetTeam) {
        int currentTeam = teamByPlayer.getOrDefault(player.getUniqueId(), -1);
        if (currentTeam == targetTeam) {
            return true;
        }
        int team1 = getOnlineTeamCount(RED_TEAM);
        int team2 = getOnlineTeamCount(BLUE_TEAM);
        if (currentTeam == RED_TEAM) {
            team1 = Math.max(0, team1 - 1);
        } else if (currentTeam == BLUE_TEAM) {
            team2 = Math.max(0, team2 - 1);
        }
        if (targetTeam == RED_TEAM) {
            team1++;
        } else if (targetTeam == BLUE_TEAM) {
            team2++;
        }
        return Math.abs(team1 - team2) <= 1;
    }

    public void rebalanceTeamsAfterRound() {
        List<Player> red = new ArrayList<>();
        List<Player> blue = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isObserver(player)) {
                continue;
            }
            Integer team = getSelectedTeam(player);
            if (team == null) {
                continue;
            }
            if (team == RED_TEAM) {
                red.add(player);
            } else if (team == BLUE_TEAM) {
                blue.add(player);
            }
        }

        while (Math.abs(red.size() - blue.size()) > 1) {
            if (red.size() > blue.size()) {
                Player moved = red.remove(red.size() - 1);
                teamByPlayer.put(moved.getUniqueId(), BLUE_TEAM);
                blue.add(moved);
                moved.sendMessage("§eБаланс обновлен: вы переведены в команду §9Синие§e.");
            } else {
                Player moved = blue.remove(blue.size() - 1);
                teamByPlayer.put(moved.getUniqueId(), RED_TEAM);
                red.add(moved);
                moved.sendMessage("§eБаланс обновлен: вы переведены в команду §cКрасные§e.");
            }
        }
    }

    public int assignTeamForRunningJoin(Player player) {
        int team1 = getOnlineTeamCount(RED_TEAM);
        int team2 = getOnlineTeamCount(BLUE_TEAM);
        int assigned = team1 <= team2 ? RED_TEAM : BLUE_TEAM;
        teamByPlayer.put(player.getUniqueId(), assigned);
        return assigned;
    }

    public void autoAssignTeamIfNeeded(Player player, Config config) {
        if (isObserver(player)) {
            return;
        }
        Integer current = getSelectedTeam(player);
        if (current != null && current >= 1 && current <= config.getTeamCount()) {
            return;
        }
        int team1 = getOnlineTeamCount(RED_TEAM);
        int team2 = getOnlineTeamCount(BLUE_TEAM);
        int assigned = team1 <= team2 ? RED_TEAM : BLUE_TEAM;
        if (getOnlineTeamCount(assigned) >= config.getPlayersPerTeam()) {
            int other = assigned == RED_TEAM ? BLUE_TEAM : RED_TEAM;
            if (getOnlineTeamCount(other) >= config.getPlayersPerTeam()) {
                player.sendMessage("§cОбе команды уже заполнены. Ожидайте следующую игру.");
                return;
            }
            assigned = other;
        }
        enforceTeam(player, assigned);
        player.sendMessage("§aВы автоматически добавлены в команду " + coloredTeamName(assigned) + "§a.");
    }

    public void selectTeam(Player player, int teamId, Config config) {
        if (teamId < 1 || teamId > config.getTeamCount()) {
            player.sendMessage("§cТакой команды нет.");
            return;
        }
        int current = Objects.requireNonNullElse(getSelectedTeam(player), -1);
        if (current == teamId) {
            player.sendMessage("§eВы уже в этой команде.");
            return;
        }
        int onlineInTeam = getOnlineTeamCount(teamId);
        if (onlineInTeam >= config.getPlayersPerTeam()) {
            player.sendMessage("§cКоманда заполнена.");
            return;
        }
        if (!canJoinTeamWithBalance(player, teamId)) {
            player.sendMessage("§cНельзя перейти: нарушится баланс команд.");
            return;
        }
        teamByPlayer.put(player.getUniqueId(), teamId);
        player.sendMessage("§aВы выбрали команду " + coloredTeamName(teamId) + "§a.");
    }

    public String coloredTeamName(int teamId) {
        return teamId == RED_TEAM ? "§cКрасные" : "§9Синие";
    }

    public boolean isTeamMenuOpen(Player player) {
        return player != null && teamMenuOpen.contains(player.getUniqueId());
    }

    public void enforceTeam(Player player, int assigned) {
        teamByPlayer.put(player.getUniqueId(), assigned);
    }

    public int getOnlineTeamCount(int teamId) {
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (isObserver(online)) {
                continue;
            }
            Integer assigned = getSelectedTeam(online);
            if (assigned != null && assigned == teamId) {
                count++;
            }
        }
        return count;
    }

    private ItemStack buildTeamItem(Player viewer, Config config, int teamId, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int online = getOnlineTeamCount(teamId);
            meta.setDisplayName(teamId == RED_TEAM ? "§cКрасные" : "§9Синие");
            List<String> lore = new ArrayList<>();
            lore.add("§7Игроков: §f" + online + "/" + config.getPlayersPerTeam());
            lore.add("§eНажмите для выбора");
            Integer current = getSelectedTeam(viewer);
            if (current != null && current == teamId) {
                lore.add("§aВы выбрали эту команду");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
