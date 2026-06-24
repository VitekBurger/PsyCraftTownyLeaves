package mao.psyCraftTowny.config;

import org.bukkit.Location;
import mao.psyCraftTowny.maps.GameMap;
import mao.psyCraftTowny.points.CapturePoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Config {
    private int teamCount;
    private int playersPerTeam;
    private boolean autoPlayersPerTeam;
    private int gameDurationMinutes;
    private int respawnsPerPlayer;
    private int respawnDelaySeconds;
    private final double capturePercentPerPlayerPerSecond;
    private Location lobbySpawn;
    private final Map<String, GameMap> maps;

    public Config(int teamCount, int playersPerTeam, boolean autoPlayersPerTeam, int gameDurationMinutes, int respawnsPerPlayer, int respawnDelaySeconds, double capturePercentPerPlayerPerSecond, Location lobbySpawn, Map<String, GameMap> maps) {
        this.teamCount = teamCount;
        this.playersPerTeam = playersPerTeam;
        this.autoPlayersPerTeam = autoPlayersPerTeam;
        this.gameDurationMinutes = gameDurationMinutes;
        this.respawnsPerPlayer = respawnsPerPlayer;
        this.respawnDelaySeconds = respawnDelaySeconds;
        this.capturePercentPerPlayerPerSecond = capturePercentPerPlayerPerSecond;
        this.lobbySpawn = lobbySpawn;
        this.maps = maps;
    }

    public int getTeamCount() {
        return teamCount;
    }

    public void setTeamCount(int teamCount) {
        this.teamCount = teamCount;
    }

    public boolean isAutoPlayersPerTeam() {
        return autoPlayersPerTeam;
    }

    public void setAutoPlayersPerTeam(boolean autoPlayersPerTeam) {
        this.autoPlayersPerTeam = autoPlayersPerTeam;
    }

    public int getPlayersPerTeam() {
        return playersPerTeam;
    }

    public void setPlayersPerTeam(int playersPerTeam) {
        this.playersPerTeam = playersPerTeam;
    }

    public int getGameDurationMinutes() {
        return gameDurationMinutes;
    }

    public void setGameDurationMinutes(int gameDurationMinutes) {
        this.gameDurationMinutes = gameDurationMinutes;
    }

    public int getRespawnsPerPlayer() {
        return respawnsPerPlayer;
    }

    public void setRespawnsPerPlayer(int respawnsPerPlayer) {
        this.respawnsPerPlayer = respawnsPerPlayer;
    }

    public int getRespawnDelaySeconds() {
        return respawnDelaySeconds;
    }

    public void setRespawnDelaySeconds(int respawnDelaySeconds) {
        this.respawnDelaySeconds = respawnDelaySeconds;
    }

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(Location lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
    }

    public Map<Integer, Location> getTeamSpawns(String mapCode) {
        final var map = maps.get(mapCode);
        if (map != null) {
            return map.teamSpawns();
        }
        return new ConcurrentHashMap<>();
    }

    public Map<Integer, CapturePoint> getCapturePoints(String mapCode) {
        final var map = maps.get(mapCode);
        if (map != null) {
            return map.capturePoints();
        }
        return new ConcurrentHashMap<>();
    }

    public Map<String, GameMap> getMaps() {
        return maps;
    }

    public double getCapturePercentPerPlayerPerSecond() {
        return capturePercentPerPlayerPerSecond;
    }
}
