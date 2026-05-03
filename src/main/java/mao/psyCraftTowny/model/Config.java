package mao.psyCraftTowny.model;

import org.bukkit.Location;

import java.util.Map;

public class Config {
    private final Map<Integer, Location> teamSpawns;
    private final Map<Integer, CapturePoint> capturePoints;
    private int teamCount;
    private int playersPerTeam;
    private boolean autoPlayersPerTeam;
    private int gameDurationMinutes;
    private int respawnsPerPlayer;
    private int respawnDelaySeconds;
    private double capturePercentPerPlayerPerSecond;
    private Location lobbySpawn;

    public Config(int teamCount, int playersPerTeam, boolean autoPlayersPerTeam, int gameDurationMinutes, int respawnsPerPlayer, int respawnDelaySeconds, double capturePercentPerPlayerPerSecond, Location lobbySpawn, Map<Integer, Location> teamSpawns, Map<Integer, CapturePoint> capturePoints) {
        this.teamCount = teamCount;
        this.playersPerTeam = playersPerTeam;
        this.autoPlayersPerTeam = autoPlayersPerTeam;
        this.gameDurationMinutes = gameDurationMinutes;
        this.respawnsPerPlayer = respawnsPerPlayer;
        this.respawnDelaySeconds = respawnDelaySeconds;
        this.capturePercentPerPlayerPerSecond = capturePercentPerPlayerPerSecond;
        this.lobbySpawn = lobbySpawn;
        this.teamSpawns = teamSpawns;
        this.capturePoints = capturePoints;
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

    public Map<Integer, Location> getTeamSpawns() {
        return teamSpawns;
    }

    public Map<Integer, CapturePoint> getCapturePoints() {
        return capturePoints;
    }

    public double getCapturePercentPerPlayerPerSecond() {
        return capturePercentPerPlayerPerSecond;
    }
}
