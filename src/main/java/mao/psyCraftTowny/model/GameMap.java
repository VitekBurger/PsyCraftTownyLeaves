package mao.psyCraftTowny.model;

import org.bukkit.Location;

import java.util.Map;

public class GameMap {
    private final String code;
    private final String displayName;
    private Location worldBorderCenter;
    private double worldBorderSize;
    private final Map<Integer, Location> teamSpawns;
    private final Map<Integer, CapturePoint> capturePoints;

    public GameMap(String code, String displayName, Location worldBorderCenter, double worldBorderSize, Map<Integer, Location> teamSpawns, Map<Integer, CapturePoint> capturePoints) {
        this.code = code;
        this.displayName = displayName;
        this.worldBorderCenter = worldBorderCenter;
        this.worldBorderSize = worldBorderSize;
        this.teamSpawns = teamSpawns;
        this.capturePoints = capturePoints;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Location getWorldBorderCenter() {
        return worldBorderCenter;
    }

    public double getWorldBorderSize() {
        return worldBorderSize;
    }

    public Map<Integer, Location> getTeamSpawns() {
        return teamSpawns;
    }

    public Map<Integer, CapturePoint> getCapturePoints() {
        return capturePoints;
    }
}
