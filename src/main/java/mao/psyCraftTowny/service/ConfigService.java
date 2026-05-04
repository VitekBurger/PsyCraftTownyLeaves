package mao.psyCraftTowny.service;

import mao.psyCraftTowny.PsyCraftTowny;
import mao.psyCraftTowny.model.CapturePoint;
import mao.psyCraftTowny.model.Config;
import mao.psyCraftTowny.model.GameMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigService {
    private static final int DEFAULT_TEAMS = 2;
    private static final int DEFAULT_PLAYERS_PER_TEAM = 5;
    private static final int DEFAULT_GAME_DURATION_MINUTES = 20;
    private static final int DEFAULT_RESPAWNS_PER_PLAYER = 2;
    private static final int DEFAULT_RESPAWN_DELAY_SECONDS = 5;
    private static final double DEFAULT_PERCENT_PER_PLAYER_PER_SECOND = 2.0D;
    private final PsyCraftTowny plugin;

    public ConfigService(PsyCraftTowny plugin) {
        this.plugin = plugin;
    }

    public void saveConfig(Config config) {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("minigame.teams", config.getTeamCount());
        cfg.set("minigame.players-per-team", config.getPlayersPerTeam());
        cfg.set("minigame.auto-players-per-team", config.isAutoPlayersPerTeam());
        cfg.set("minigame.game-duration-minutes", config.getGameDurationMinutes());
        cfg.set("minigame.respawns-per-player", config.getRespawnsPerPlayer());
        cfg.set("minigame.respawn-delay-seconds", config.getRespawnDelaySeconds());
        cfg.set("minigame.capture.percent-per-player-per-second", config.getCapturePercentPerPlayerPerSecond());
        writeLocation(cfg, "minigame.lobby", config.getLobbySpawn());
        writeMaps(cfg, config.getMaps());
        plugin.saveConfig();
    }

    public Config readConfig() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();
        final var teamCount = Math.max(2, cfg.getInt("minigame.teams", DEFAULT_TEAMS));
        final var playersPerTeam = Math.max(1, cfg.getInt("minigame.players-per-team", DEFAULT_PLAYERS_PER_TEAM));
        final var autoPlayersPerTeam = cfg.getBoolean("minigame.auto-players-per-team", true);
        final var gameDurationMinutes = Math.max(1, cfg.getInt("minigame.game-duration-minutes", DEFAULT_GAME_DURATION_MINUTES));
        final var respawnsPerPlayer = Math.max(0, cfg.getInt("minigame.respawns-per-player", DEFAULT_RESPAWNS_PER_PLAYER));
        final var respawnDelaySeconds = Math.max(1, cfg.getInt("minigame.respawn-delay-seconds", DEFAULT_RESPAWN_DELAY_SECONDS));
        final var capturePercentPerPlayerPerSecond = Math.max(1.0D, cfg.getDouble("minigame.capture.percent-per-player-per-second", DEFAULT_PERCENT_PER_PLAYER_PER_SECOND));
        final var lobbySpawn = readLocation(cfg.getConfigurationSection("minigame.lobby"));
        final var maps = readMaps(cfg);

        return new Config(
                teamCount,
                playersPerTeam,
                autoPlayersPerTeam,
                gameDurationMinutes,
                respawnsPerPlayer,
                respawnDelaySeconds,
                capturePercentPerPlayerPerSecond,
                lobbySpawn,
                maps
        );
    }

    private Map<String, GameMap> readMaps(FileConfiguration cfg) {
        final var maps = new ConcurrentHashMap<String, GameMap>();
        final var mapsSection = cfg.getConfigurationSection("minigame.maps");
        if (mapsSection == null) {
            return maps;
        }
        for (final var mapCode : mapsSection.getKeys(false)) {
            final var teamSpawns = new ConcurrentHashMap<Integer, Location>();
            final var mapDisplayName = cfg.getString("minigame.maps.%s.display-name".formatted(mapCode), mapCode);
            final var menuItemKey = cfg.getString("minigame.maps.%s.menu-item-key".formatted(mapCode), "");
            final var mode = GameMap.Mode.valueOf(cfg.getString("minigame.maps.%s.mode".formatted(mapCode), ""));
            final var worldBorderCenter = readLocation(cfg.getConfigurationSection("minigame.maps.%s.world-border-center".formatted(mapCode)));
            final var worldBorderSize = cfg.getDouble("minigame.maps.%s.world-border-size".formatted(mapCode));
            ConfigurationSection teamsSection = cfg.getConfigurationSection("minigame.maps.%s.team-spawns".formatted(mapCode));
            if (teamsSection != null) {
                for (String key : teamsSection.getKeys(false)) {
                    try {
                        int teamId = Integer.parseInt(key);
                        Location spawn = readLocation(teamsSection.getConfigurationSection(key));
                        if (spawn != null) {
                            teamSpawns.put(teamId, spawn);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            final var capturePoints = new ConcurrentHashMap<Integer, CapturePoint>();
            ConfigurationSection pointsSection = cfg.getConfigurationSection("minigame.maps.%s.capture.points".formatted(mapCode));
            if (pointsSection != null) {
                int maxId = 0;
                for (String key : pointsSection.getKeys(false)) {
                    ConfigurationSection section = pointsSection.getConfigurationSection(key);
                    if (section == null) {
                        continue;
                    }
                    int pointId;
                    try {
                        pointId = Integer.parseInt(key);
                    } catch (NumberFormatException ignored) {
                        pointId = section.getInt("id", 0);
                    }
                    String world = section.getString("world");
                    if (world == null || Bukkit.getWorld(world) == null) {
                        continue;
                    }
                    int pointX;
                    int pointY;
                    int pointZ;
                    if (section.contains("x") && section.contains("y") && section.contains("z")) {
                        pointX = section.getInt("x");
                        pointY = section.getInt("y");
                        pointZ = section.getInt("z");
                    } else {
                        int chunkX = section.getInt("chunk-x");
                        int chunkZ = section.getInt("chunk-z");
                        pointX = (chunkX << 4) + 8;
                        pointY = Bukkit.getWorld(world).getHighestBlockYAt(pointX, (chunkZ << 4) + 8);
                        pointZ = (chunkZ << 4) + 8;
                    }
                    if (pointId <= 0) {
                        pointId = ++maxId;
                    } else {
                        maxId = Math.max(maxId, pointId);
                    }
                    while (capturePoints.containsKey(pointId)) {
                        pointId++;
                        maxId = Math.max(maxId, pointId);
                    }
                    final var displayName = section.getString("display-name", String.valueOf(pointId));
                    CapturePoint point = new CapturePoint(pointId, displayName, world, pointX, pointY, pointZ, 0D, 0);
                    capturePoints.put(pointId, point);
                }
            }
            maps.put(mapCode, new GameMap(mapCode, mapDisplayName, menuItemKey, worldBorderCenter, worldBorderSize, mode, teamSpawns, capturePoints));
        }
        return maps;
    }

    private void writeMaps(FileConfiguration cfg, Map<String, GameMap> maps) {
        maps.forEach((mapCode, gameMap) -> {
            cfg.set("minigame.maps.%s.team-spawns".formatted(mapCode), null);
            cfg.set("minigame.maps.%s.menu-item-key".formatted(mapCode), gameMap.menuItemKey());
            cfg.set("minigame.maps.%s.mode".formatted(mapCode), gameMap.mode().name());
            cfg.set("minigame.maps.%s.display-name".formatted(mapCode), gameMap.displayName());
            writeLocation(cfg, "minigame.maps.%s.world-border-center".formatted(mapCode), gameMap.worldBorderCenter());
            cfg.set("minigame.maps.%s.world-border-size".formatted(mapCode), gameMap.worldBorderSize());
            for (Map.Entry<Integer, Location> entry : gameMap.teamSpawns().entrySet()) {
                writeLocation(cfg, "minigame.maps.%s.team-spawns.%s".formatted(mapCode, entry.getKey()), entry.getValue());
            }
            cfg.set("minigame.maps.%s.capture.points".formatted(mapCode), null);
            List<Integer> ids = new ArrayList<>(gameMap.capturePoints().keySet());
            ids.sort(Integer::compareTo);
            for (Integer id : ids) {
                CapturePoint point = gameMap.capturePoints().get(id);
                if (point == null) {
                    continue;
                }
                String path = "minigame.maps.%s.capture.points.%s".formatted(mapCode, id);
                cfg.set(path + ".id", point.id());
                cfg.set(path + ".display-name", point.displayName());
                cfg.set(path + ".world", point.world());
                cfg.set(path + ".x", point.pointX());
                cfg.set(path + ".y", point.pointY());
                cfg.set(path + ".z", point.pointZ());
            }
        });
    }

    private Location readLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String world = section.getString("world");
        if (world == null || Bukkit.getWorld(world) == null) {
            return null;
        }
        return new Location(
                Bukkit.getWorld(world),
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );
    }

    private void writeLocation(FileConfiguration cfg, String path, Location location) {
        if (location == null || location.getWorld() == null) {
            cfg.set(path, null);
            return;
        }
        cfg.set(path + ".world", location.getWorld().getName());
        cfg.set(path + ".x", location.getX());
        cfg.set(path + ".y", location.getY());
        cfg.set(path + ".z", location.getZ());
        cfg.set(path + ".yaw", location.getYaw());
        cfg.set(path + ".pitch", location.getPitch());
    }
}
