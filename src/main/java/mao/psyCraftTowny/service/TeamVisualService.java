package mao.psyCraftTowny.service;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

final class TeamVisualService {
    private final Scoreboard scoreboard;
    private final Team redBoardTeam;
    private final Team blueBoardTeam;

    TeamVisualService() {
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        this.redBoardTeam = getOrCreateBoardTeam("pcta_red", ChatColor.RED, "§c[R] ");
        this.blueBoardTeam = getOrCreateBoardTeam("pcta_blue", ChatColor.BLUE, "§9[B] ");
    }

    void removePlayer(Player player) {
        if (player == null) {
            return;
        }
        redBoardTeam.removeEntry(player.getName());
        blueBoardTeam.removeEntry(player.getName());
    }

    void applyPlayerTeamVisual(Player player, Integer teamId, int redTeam, int blueTeam) {
        if (player == null) {
            return;
        }
        player.setScoreboard(scoreboard);
        removePlayer(player);
        if (teamId == null) {
            player.setPlayerListName(player.getName());
            return;
        }
        if (teamId == redTeam) {
            redBoardTeam.addEntry(player.getName());
            player.setPlayerListName("§c" + player.getName());
            return;
        }
        if (teamId == blueTeam) {
            blueBoardTeam.addEntry(player.getName());
            player.setPlayerListName("§9" + player.getName());
            return;
        }
        player.setPlayerListName(player.getName());
    }

    String colorizeName(String name, Integer teamId, int redTeam, int blueTeam) {
        if (name == null || name.isBlank()) {
            return "§7Игрок";
        }
        if (teamId == null) {
            return "§7" + name;
        }
        if (teamId == redTeam) {
            return "§c" + name;
        }
        if (teamId == blueTeam) {
            return "§9" + name;
        }
        return "§7" + name;
    }

    private Team getOrCreateBoardTeam(String name, ChatColor color, String prefix) {
        Team team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.registerNewTeam(name);
        }
        team.setColor(color);
        team.setPrefix(prefix);
        team.setAllowFriendlyFire(false);
        team.setCanSeeFriendlyInvisibles(true);
        return team;
    }
}
