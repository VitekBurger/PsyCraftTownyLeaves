package mao.psyCraftTowny.service;

import mao.psyCraftTowny.PsyCraftTowny;
import mao.psyCraftTowny.config.ConfigService;
import mao.psyCraftTowny.kits.KitSelectionService;
import mao.psyCraftTowny.kits.PlayerItemsService;
import mao.psyCraftTowny.maps.MapSelectionService;
import mao.psyCraftTowny.points.CapturePoint;
import mao.psyCraftTowny.config.Config;
import mao.psyCraftTowny.maps.GameMap;
import mao.psyCraftTowny.kits.KitType;
import mao.psyCraftTowny.stats.RoundStatsService;
import mao.psyCraftTowny.teams.TeamSelectionService;
import mao.psyCraftTowny.teams.TeamVisualService;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static mao.psyCraftTowny.utils.PlayerUtils.isObserver;

public class MiniGameService {
    private static final double CAPTURE_RADIUS = 8.0D;
    public static final int RED_TEAM = 1;
    public static final int BLUE_TEAM = 2;
    private final PsyCraftTowny plugin;
    private final PlayerItemsService playerItemsService;
    private final ConfigService configService;
    private final TeamSelectionService teamSelectionService;
    private final MapSelectionService mapSelectionService;
    private final KitSelectionService kitSelectionService;
    private final Set<UUID> pendingRunningKitChoice = ConcurrentHashMap.newKeySet();
    private final Set<UUID> alivePlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> remainingRespawns = new ConcurrentHashMap<>();
    private final RoundStatsService roundStatsService = new RoundStatsService();
    private final TeamVisualService teamVisualService = new TeamVisualService();
    private final Set<UUID> queuedRespawns = ConcurrentHashMap.newKeySet();
    private final Map<String, BlockState> changedBlocks = new ConcurrentHashMap<>();
    private final Set<String> changedChunks = ConcurrentHashMap.newKeySet();
    private final Map<String, BlockState> captureMarkerOriginalStates = new ConcurrentHashMap<>();
    private final Set<String> protectedPointBlocks = ConcurrentHashMap.newKeySet();
    private final BossBar statusBossBar;
    private Config config;
    private String currentMapCode;
    private int gameTimeLeftSeconds = 0;
    private Phase phase = Phase.WAITING;
    private int countdownLeft = 0;
    private int countdownInitial = 30;
    private BukkitTask restoreTask;
    private BukkitTask lobbyMonitorTask;
    private BukkitTask countdownTask;
    private BukkitTask runningTask;

    private final Map<UUID, Integer> spawnCampingSeconds = new ConcurrentHashMap<>();

    private GameMap.Mode currentMode;

    public MiniGameService(PsyCraftTowny plugin, PlayerItemsService playerItemsService, TeamSelectionService teamSelectionService, MapSelectionService mapSelectionService, KitSelectionService kitSelectionService) {
        this.plugin = plugin;
        this.playerItemsService = playerItemsService;
        this.teamSelectionService = teamSelectionService;
        this.mapSelectionService = mapSelectionService;
        this.kitSelectionService = kitSelectionService;
        this.statusBossBar = Bukkit.createBossBar("Ожидание игроков", BarColor.WHITE, BarStyle.SEGMENTED_10);
        this.statusBossBar.setVisible(true);
        this.configService = new ConfigService(plugin);
        this.config = configService.readConfig();
        updateBossBar();
    }

    public void startLobbyMonitorTask() {
        if (lobbyMonitorTask != null) {
            lobbyMonitorTask.cancel();
        }
        lobbyMonitorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            attachBossBarPlayers();
            healLobbyStateForPlayers();
            tickLobbyState();
        }, 20L, 20L);
    }

    public void reloadFromConfig() {
        this.config = configService.readConfig();
        if (phase == Phase.RUNNING) {
            clearCaptureOverlays();
            deployCaptureOverlays();
        }
        updateBossBar();
    }

    public void shutdown() {
        if (lobbyMonitorTask != null) {
            lobbyMonitorTask.cancel();
            lobbyMonitorTask = null;
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (runningTask != null) {
            runningTask.cancel();
            runningTask = null;
        }
        if (restoreTask != null) {
            restoreTask.cancel();
            restoreTask = null;
        }
        clearCaptureOverlays();
        statusBossBar.removeAll();
    }

    public PsyCraftTowny getPlugin() {
        return plugin;
    }

    public Phase getPhase() {
        return phase;
    }

    public void handlePlayerJoin(Player player) {
        statusBossBar.addPlayer(player);
        statusBossBar.setVisible(true);
        kitSelectionService.selectDefaultKitForNewPlayer(player);
        if (isObserver(player)) {
            teamSelectionService.removeTeamSelection(player);
            alivePlayers.remove(player.getUniqueId());
            remainingRespawns.remove(player.getUniqueId());
            queuedRespawns.remove(player.getUniqueId());
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
            Bukkit.getScheduler().runTask(plugin, () -> teleportToLobby(player));
            player.sendMessage("§7Вы зашли как наблюдатель.");
            applyPlayerTeamVisual(player);
            updateBossBar();
            return;
        }
        if (isRoundLive()) {
            UUID uuid = player.getUniqueId();
            Integer persistedRespawns = remainingRespawns.get(uuid);
            if (persistedRespawns != null && persistedRespawns <= 0 && currentMode != GameMap.Mode.FFA) {
                player.getInventory().clear();
                player.setGameMode(GameMode.SPECTATOR);
                teleportToLobby(player);
                player.sendMessage("§cВы уже выбыли в этом раунде. Ожидайте следующую игру.");
                closeMenuTracking(player);
                updateBossBar();
                return;
            }
            
            if (currentMode == GameMap.Mode.FFA) {
                teamSelectionService.removeTeamSelection(player);
                alivePlayers.add(uuid);
                remainingRespawns.put(uuid, Integer.MAX_VALUE);
                roundStatsService.ensurePlayer(uuid);
                preparePlayerForRound(player);
                applyPlayerTeamVisual(player);
                player.teleport(findRandomFfaSpawn(player.getWorld(), player.getWorld().getWorldBorder()));
                applySelectedKit(player);
                player.sendMessage("§aВы присоединились к FFA!");
            } else {
                int assigned = teamSelectionService.assignTeamForRunningJoin(player);
                if (assigned < 1) {
                    player.getInventory().clear();
                    player.setGameMode(GameMode.SPECTATOR);
                    player.sendMessage("§cИгра уже идет, свободной команды нет. Вы наблюдатель.");
                    closeMenuTracking(player);
                    updateBossBar();
                    return;
                }
                alivePlayers.add(uuid);
                remainingRespawns.put(uuid, persistedRespawns == null ? config.getRespawnsPerPlayer() : persistedRespawns);
                roundStatsService.ensurePlayer(uuid);
                preparePlayerForRound(player);
                applyPlayerTeamVisual(player);
                Location spawn = config.getTeamSpawns(currentMapCode).get(assigned);
                if (spawn != null) {
                    player.teleport(spawn);
                } else {
                    teleportToLobby(player);
                }
                applySelectedKit(player);
                player.sendMessage("§aИгра уже идет. Вы добавлены в " + teamSelectionService.coloredTeamName(assigned) + "§a.");
            }
            
            pendingRunningKitChoice.add(uuid);
            kitSelectionService.giveKitSelectorItem(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && phase == Phase.RUNNING && pendingRunningKitChoice.contains(uuid)) {
                    openKitSelector(player);
                }
            }, 10L);
            closeMenuTracking(player);
            updateBossBar();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> teleportToLobby(player));
        autoAssignTeamIfNeeded(player);
        prepareLobbyPlayer(player);
        applyPlayerTeamVisual(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && phase != Phase.RUNNING) {
                openTeamSelector(player);
            }
        }, 10L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && phase != Phase.RUNNING) {
                openKitSelector(player);
            }
        }, 30L);
        updateBossBar();
    }

    private boolean isRoundLive() {
        return phase == Phase.RUNNING && runningTask != null && gameTimeLeftSeconds > 0;
    }

    public void handlePlayerQuit(Player player) {
        statusBossBar.removePlayer(player);
        closeMenuTracking(player);
        pendingRunningKitChoice.remove(player.getUniqueId());
        teamVisualService.removePlayer(player);
        if (phase == Phase.RUNNING) {
            alivePlayers.remove(player.getUniqueId());
            checkAliveWinCondition();
        }
        queuedRespawns.remove(player.getUniqueId());
        updateBossBar();
    }

    public void handlePlayerDeath(Player player) {
        if (phase != Phase.RUNNING) {
            return;
        }
        if (isObserver(player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        final var team = teamSelectionService.getSelectedTeam(player);
        if (currentMode == GameMap.Mode.FFA || currentMode == GameMap.Mode.TDM) {
            player.sendMessage("§eВы возродитесь через " + config.getRespawnDelaySeconds() + " сек. Респавны в этом режиме бесконечные");
        } else {
            int left = remainingRespawns.getOrDefault(uuid, config.getRespawnsPerPlayer());
            if (left <= 0) {
                alivePlayers.remove(uuid);
                player.sendMessage("§cУ вас закончились респавны. Вы выбыли до конца игры.");
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Player online = Bukkit.getPlayer(uuid);
                    if (online != null && online.isOnline() && online.isDead()) {
                        online.spigot().respawn();
                    }
                }, 1L);
                checkAliveWinCondition();
                updateBossBar();
                return;
            }
            remainingRespawns.put(uuid, left - 1);
            player.sendMessage("§eВы возродитесь через " + config.getRespawnDelaySeconds() + " сек. Осталось респавнов: §f" + (left - 1));
        }

        queuedRespawns.add(uuid);
        // Убираем экран смерти как в BedWars: принудительный respawn сразу.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player online = Bukkit.getPlayer(uuid);
            if (online == null || !online.isOnline()) {
                return;
            }
            if (online.isDead()) {
                online.spigot().respawn();
            }
        }, 1L);
    }

    public void handlePlayerKill(Player killer) {
        if (phase != Phase.RUNNING || killer == null || isObserver(killer)) {
            return;
        }
        roundStatsService.recordKill(killer.getUniqueId());
    }

    public void handleRespawn(Player player) {
        if (phase == Phase.RUNNING) {
            UUID uuid = player.getUniqueId();
            if (currentMode != GameMap.Mode.FFA && !queuedRespawns.remove(uuid)) {
                player.setGameMode(GameMode.SPECTATOR);
                Bukkit.getScheduler().runTask(plugin, () -> teleportToLobby(player));
                return;
            }
            
            // For FFA, we don't need to check queuedRespawns as much, but we still use it for the delay logic
            if (currentMode == GameMap.Mode.FFA) {
                queuedRespawns.remove(uuid);
            }

            Integer teamId = teamSelectionService.getSelectedTeam(player);
            Location teamSpawn = (currentMode == GameMap.Mode.FFA) ? null : (teamId == null ? null : config.getTeamSpawns(currentMapCode).get(teamId));
            
            player.setGameMode(GameMode.SPECTATOR);
            if (currentMode == GameMap.Mode.FFA) {
                player.teleport(findRandomFfaSpawn(player.getWorld(), player.getWorld().getWorldBorder()));
            } else if (teamSpawn != null) {
                player.teleport(teamSpawn);
            } else {
                teleportToLobby(player);
            }

            for (int i = 1; i <= config.getRespawnDelaySeconds(); i++) {
                int left = config.getRespawnDelaySeconds() - i + 1;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Player online = Bukkit.getPlayer(uuid);
                    if (online != null && online.isOnline() && phase == Phase.RUNNING) {
                        online.sendActionBar("§eВозрождение через §f" + left + "§e сек.");
                    }
                }, (long) (i - 1) * 20L);
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online == null || !online.isOnline() || phase != Phase.RUNNING) {
                    return;
                }
                preparePlayerForRound(online);
                if (currentMode == GameMap.Mode.FFA) {
                    online.teleport(findRandomFfaSpawn(online.getWorld(), online.getWorld().getWorldBorder()));
                } else {
                    Location spawn = teamId == null ? null : config.getTeamSpawns(currentMapCode).get(teamId);
                    if (spawn != null) {
                        online.teleport(spawn);
                    }
                }
                applySelectedKit(online);
            }, config.getRespawnDelaySeconds() * 20L);
            return;
        }
        if (isObserver(player)) {
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
            Bukkit.getScheduler().runTask(plugin, () -> teleportToLobby(player));
            return;
        }
        prepareLobbyPlayer(player);
        Bukkit.getScheduler().runTask(plugin, () -> teleportToLobby(player));
    }

    public Location resolveRespawnLocation(Player player) {
        if (phase == Phase.RUNNING) {
            Integer teamId = teamSelectionService.getSelectedTeam(player);
            Location teamSpawn = teamId == null ? null : config.getTeamSpawns(currentMapCode).get(teamId);
            if (teamSpawn != null) {
                return teamSpawn.clone();
            }
        }
        return config.getLobbySpawn() == null ? player.getLocation() : config.getLobbySpawn().clone();
    }

    public void openTeamSelector(Player player) {
        if (phase == Phase.RUNNING) {
            player.sendMessage("§cВо время игры выбрать команду нельзя.");
            return;
        }
        kitSelectionService.closeTab(player);
        mapSelectionService.closeTab(player);
        teamSelectionService.openTab(player, config);
    }

    public void openKitSelector(Player player) {
        if (phase == Phase.RUNNING && !pendingRunningKitChoice.contains(player.getUniqueId())) {
            player.sendMessage("§cВо время игры выбрать кит нельзя.");
            return;
        }
        teamSelectionService.closeTab(player);
        mapSelectionService.closeTab(player);
        kitSelectionService.openTab(player);
    }

    public void openMapSelector(Player player) {
        if (phase == Phase.RUNNING) {
            player.sendMessage("§cВо время игры выбрать карту нельзя.");
            return;
        }
        kitSelectionService.closeTab(player);
        teamSelectionService.closeTab(player);
        mapSelectionService.openTab(player, config);
    }

    public void selectTeam(Player player, int teamId) {
        if (isObserver(player)) {
            player.sendMessage("§7Наблюдатели не участвуют в распределении команд.");
            return;
        }
        if (phase == Phase.RUNNING) {
            player.sendMessage("§cВо время игры смена команды недоступна.");
            return;
        }
        teamSelectionService.selectTeam(player, teamId, config);
        applyPlayerTeamVisual(player);
        openTeamSelector(player);
        updateBossBar();
    }

    public void selectKitByMenuItem(Player player, ItemStack stack) {
        KitType type = kitSelectionService.resolveKitTypeByMenuItem(stack);
        if (type == null) {
            return;
        }
        if (phase == Phase.RUNNING && !pendingRunningKitChoice.contains(player.getUniqueId())) {
            player.sendMessage("§cВо время игры смена кита недоступна.");
            return;
        }
        kitSelectionService.selectKit(player, type);
        if (phase == Phase.RUNNING) {
            pendingRunningKitChoice.remove(player.getUniqueId());
            preparePlayerForRound(player);
            applySelectedKit(player);
            applyPlayerTeamVisual(player);
            closeMenuTracking(player);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && phase != Phase.RUNNING) {
                openKitSelector(player);
            }
        }, 1L);
    }

    public void selectMapOrModeByMenuItem(Player player, ItemStack stack) {
        if (phase == Phase.RUNNING) {
            player.sendMessage("§cВо время игры голосование недоступно.");
            return;
        }
        mapSelectionService.handleGuiClick(player, stack, config);
    }

    public void closeMenuTracking(Player player) {
        if (player == null) {
            return;
        }
        teamSelectionService.closeTab(player);
        kitSelectionService.closeTab(player);
        mapSelectionService.closeTab(player);
    }

    public void setLobby(Player player) {
        this.config.setLobbySpawn(player.getLocation().clone());
        configService.saveConfig(this.config);
    }

    public boolean setTeamsCount(int count) {
        if (count != 2) {
            return false;
        }
        this.config.setTeamCount(count);
        configService.saveConfig(this.config);
        return true;
    }

    public boolean setPlayersPerTeam(int count) {
        if (count < 1) {
            return false;
        }
        this.config.setPlayersPerTeam(count);
        this.config.setAutoPlayersPerTeam(false);
        configService.saveConfig(this.config);
        return true;
    }

    public void setAutoPlayersPerTeam(boolean enabled) {
        this.config.setAutoPlayersPerTeam(enabled);
        configService.saveConfig(this.config);
        updateBossBar();
    }

    public boolean setGameDurationMinutes(int minutes) {
        if (minutes < 1) {
            return false;
        }
        this.config.setGameDurationMinutes(minutes);
        configService.saveConfig(this.config);
        updateBossBar();
        return true;
    }

    public boolean setTeamSpawn(String mapCode, int teamId, Location location) {
        if (teamId < 1 || teamId > config.getTeamCount()) {
            return false;
        }
        config.getTeamSpawns(mapCode).put(teamId, location.clone());
        configService.saveConfig(this.config);
        return true;
    }

    public int addCapturePoint(String mapCode, Location location, String displayName) {
        if (location == null || location.getWorld() == null) {
            return -1;
        }
        int id = nextCapturePointId(mapCode);
        int pointX = location.getBlockX();
        int pointY = location.getBlockY();
        int pointZ = location.getBlockZ();
        String world = location.getWorld().getName();
        CapturePoint point = new CapturePoint(id, displayName, world, pointX, pointY, pointZ, 0D, 0);
        config.getCapturePoints(mapCode).put(id, point);
        if (phase == Phase.RUNNING && Objects.equals(mapCode, currentMapCode)) {
            clearCaptureOverlays();
            deployCaptureOverlays();
        }
        configService.saveConfig(this.config);
        return id;
    }

    public void addMap(Location location, String code, GameMap.Mode mode, String itemKey, double worldBorderSize, String displayName) {
        config.getMaps().put(code, new GameMap(code, displayName, itemKey, location, worldBorderSize, mode, new ConcurrentHashMap<>(), new ConcurrentHashMap<>()));
        configService.saveConfig(this.config);
    }

    public boolean removeCapturePoint(String mapCode, int pointId) {
        CapturePoint removed = config.getCapturePoints(mapCode).remove(pointId);
        if (removed == null) {
            return false;
        }
        if (phase == Phase.RUNNING && Objects.equals(mapCode, currentMapCode)) {
            clearCaptureOverlays();
            deployCaptureOverlays();
        }
        configService.saveConfig(this.config);
        return true;
    }

    public boolean removeMap(String code) {
        if (Objects.equals(currentMapCode, code)) {
            return false;
        }
        GameMap removed = config.getMaps().remove(code);
        if (removed == null) {
            return false;
        }
        configService.saveConfig(this.config);
        return true;
    }

    public List<Integer> getCapturePointIds() {
        List<Integer> ids = new ArrayList<>(config.getCapturePoints(currentMapCode).keySet());
        ids.sort(Integer::compareTo);
        return ids;
    }

    public List<String> getMapCodes() {
        List<String> codes = new ArrayList<>(config.getMaps().keySet());
        codes.sort(Comparator.naturalOrder());
        return codes;
    }

    public List<String> describeCapturePoints(String mapCode) {
        List<CapturePoint> points = new ArrayList<>(config.getCapturePoints(mapCode).values());
        points.sort(Comparator.comparingInt(CapturePoint::id));
        List<String> out = new ArrayList<>();
        for (CapturePoint point : points) {
            out.add("§7#" + point.id() + " §f" + point.world() + " §7(" + point.pointX() + ", " + point.pointY() + ", " + point.pointZ() + ")");
        }
        return out;
    }

    public List<String> describeMap() {
        List<GameMap> maps = new ArrayList<>(config.getMaps().values());
        maps.sort(Comparator.comparing(GameMap::code, Comparator.naturalOrder()));
        List<String> out = new ArrayList<>();
        for (GameMap map : maps) {
            out.add("§7#" + map.code() + " §f" + map.displayName() + " §7(" + map.worldBorderCenter().getX() + ", " + map.worldBorderCenter().getZ() + " размер = " + map.worldBorderSize() + ")");
        }
        return out;
    }

    public void recordBlockChange(Block block) {
        if (block == null) {
            return;
        }
        recordBlockState(block.getState());
        
        // Handle multi-block structures
        Material type = block.getType();
        if (type.name().contains("DOOR")) {
            recordBlockState(block.getRelative(org.bukkit.block.BlockFace.UP).getState());
            recordBlockState(block.getRelative(org.bukkit.block.BlockFace.DOWN).getState());
        } else if (type.name().contains("BED")) {
            for (org.bukkit.block.BlockFace face : org.bukkit.block.BlockFace.values()) {
                if (face.isCartesian()) {
                    Block rel = block.getRelative(face);
                    if (rel.getType().name().contains("BED")) {
                        recordBlockState(rel.getState());
                    }
                }
            }
        }
    }

    public void recordBlockState(BlockState state) {
        if (state == null) {
            return;
        }
        Block block = state.getBlock();
        if (phase != Phase.RUNNING && phase != Phase.COUNTDOWN) {
            return;
        }
        String key = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
        changedBlocks.putIfAbsent(key, state);
        changedChunks.add(chunkKey(block));
    }

    public boolean isProtectedPointBlock(Block block) {
        if (block == null) {
            return false;
        }
        return protectedPointBlocks.contains(blockKey(block));
    }

    public boolean isInSpawnProtection(Block block) {
        if (block == null) {
            return false;
        }
        if (isLocationInSpawnProtection(block.getLocation(), config.getLobbySpawn())) {
            return true;
        }
        if (currentMapCode == null) {
            return false;
        }
        for (Location spawn : config.getTeamSpawns(currentMapCode).values()) {
            if (isLocationInSpawnProtection(block.getLocation(), spawn)) {
                return true;
            }
        }
        return false;
    }

    public int getReadyPlayersCount() {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                count++;
            }
        }
        return count;
    }

    private void tickLobbyState() {
        if (config.isAutoPlayersPerTeam()) {
            adjustPlayersPerTeamByOnline();
        }
        if (phase == Phase.RUNNING) {
            return;
        }
        int maxPlayers = config.getTeamCount() * config.getPlayersPerTeam();
        int minPlayers = (int) Math.ceil(maxPlayers * 0.5D);
        int ready = getReadyPlayersCount();

        if (ready < minPlayers) {
            cancelCountdown("§eНедостаточно игроков, отсчет остановлен.");
            return;
        }
        if (ready >= maxPlayers) {
            startOrAdjustCountdown(10);
        } else {
            startOrAdjustCountdown(30);
        }
        updateBossBar();
    }

    private void healLobbyStateForPlayers() {
        if (phase == Phase.RUNNING) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isObserver(player)) {
                continue;
            }
            if (player.getGameMode() == GameMode.SPECTATOR) {
                prepareLobbyPlayer(player);
                teleportToLobby(player);
                continue;
            }
            if (!teamSelectionService.isTeamSelectorItem(player.getInventory().getItem(8))) {
                teamSelectionService.giveTeamSelectorItem(player);
            }
            if (!kitSelectionService.isKitSelectorItem(player.getInventory().getItem(7))) {
                kitSelectionService.giveKitSelectorItem(player);
            }
        }
    }

    private void startOrAdjustCountdown(int seconds) {
        if (phase == Phase.WAITING) {
            phase = Phase.COUNTDOWN;
            countdownLeft = seconds;
            countdownInitial = seconds;
            broadcast("§aНабрано достаточно игроков. Старт через §f" + countdownLeft + "§a сек.");
            startCountdownTask();
            return;
        }
        if (phase == Phase.COUNTDOWN && seconds < countdownLeft) {
            countdownLeft = seconds;
            countdownInitial = seconds;
            broadcast("§aЛобби заполнено. Ускоренный старт через §f" + countdownLeft + "§a сек.");
        }
    }

    private void startCountdownTask() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (phase != Phase.COUNTDOWN) {
                return;
            }
            int maxPlayers = config.getTeamCount() * config.getPlayersPerTeam();
            int minPlayers = (int) Math.ceil(maxPlayers * 0.5D);
            int ready = getReadyPlayersCount();
            if (ready < minPlayers) {
                cancelCountdown("§eНедостаточно игроков, отсчет остановлен.");
                return;
            }
            if (ready >= maxPlayers && countdownLeft > 10) {
                countdownLeft = 10;
                countdownInitial = 10;
            }
            if (countdownLeft <= 0) {
                startGame();
                return;
            }
            if (countdownLeft <= 10 || countdownLeft % 5 == 0) {
                broadcast("§eСтарт через §f" + countdownLeft + "§e сек.");
            }
            countdownLeft--;
            updateBossBar();
        }, 20L, 20L);
    }

    private void cancelCountdown(String reason) {
        if (phase != Phase.COUNTDOWN) {
            return;
        }
        phase = Phase.WAITING;
        countdownLeft = 0;
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        broadcast(reason);
        updateBossBar();
    }

    public void forceStopGame() {
        if (phase != Phase.RUNNING && phase != Phase.COUNTDOWN) {
            return;
        }
        endGame(0, "§cИгра принудительно остановлена администратором.");
    }

    private void startGame() {
        if (phase != Phase.COUNTDOWN) {
            return;
        }
        if (config.getLobbySpawn() == null) {
            cancelCountdown("§cЛобби не задано. Используйте /pcta lobby");
            return;
        }
        final var maps = config.getMaps();
        if (maps == null || maps.isEmpty()) {
            cancelCountdown("§cКарты не созданы. Используйте /pcta map");
            return;
        }
        currentMapCode = mapSelectionService.resolveNextMap(config);
        currentMode = mapSelectionService.resolveNextMode();
        
        for (int teamId = 1; teamId <= config.getTeamCount(); teamId++) {
            if (!config.getTeamSpawns(currentMapCode).containsKey(teamId)) {
                cancelCountdown("§cСпавн команды " + teamSelectionService.coloredTeamName(teamId) + "§c не задан. Используйте /pcta spawn " + teamId);
                return;
            }
        }

        if (currentMode == GameMap.Mode.FFA) {
            startFfaGame();
            return;
        }

        Map<Integer, List<Player>> participantsByTeam = new HashMap<>();
        for (int teamId = 1; teamId <= config.getTeamCount(); teamId++) {
            participantsByTeam.put(teamId, new ArrayList<>());
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (isObserver(online)) {
                continue;
            }
            // НЕ сбрасываем команду здесь, чтобы сохранить выбор игрока!
            // Только авто-распределяем тех, кто не выбрал сам
            teamSelectionService.autoAssignTeamIfNeeded(online, config);
            
            Integer teamId = teamSelectionService.getSelectedTeam(online);
            if (teamId != null && teamId >= 1 && teamId <= config.getTeamCount()) {
                participantsByTeam.get(teamId).add(online);
            } else {
                // Если авто-распределение не сработало, принудительно кидаем в команду с меньшим числом игроков
                int t1 = participantsByTeam.get(RED_TEAM).size();
                int t2 = participantsByTeam.get(BLUE_TEAM).size();
                int target = t1 <= t2 ? RED_TEAM : BLUE_TEAM;
                teamSelectionService.selectTeam(online, target, config);
                participantsByTeam.get(target).add(online);
            }
        }

        if (participantsByTeam.get(RED_TEAM).isEmpty() || participantsByTeam.get(BLUE_TEAM).isEmpty()) {
            cancelCountdown("§cНужны игроки в командах Красных и Синих.");
            return;
        }

        phase = Phase.RUNNING;
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        countdownLeft = 0;
        alivePlayers.clear();
        remainingRespawns.clear();
        queuedRespawns.clear();
        roundStatsService.clear();
        resetCapturePointsForRound();
        clearCaptureOverlays();
        deployCaptureOverlays();
        gameTimeLeftSeconds = config.getGameDurationMinutes() * 60;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isObserver(player)) {
                player.getInventory().clear();
                player.setGameMode(GameMode.SPECTATOR);
                teleportToLobby(player);
                continue;
            }
            Integer teamId = teamSelectionService.getSelectedTeam(player);
            if (teamId == null || teamId < 1 || teamId > config.getTeamCount()) {
                // Если по какой-то причине игрок все еще без команды, кидаем его в SPECTATOR
                player.getInventory().clear();
                player.setGameMode(GameMode.SPECTATOR);
                teleportToLobby(player);
                continue;
            }
            alivePlayers.add(player.getUniqueId());
            remainingRespawns.put(player.getUniqueId(), config.getRespawnsPerPlayer());
            roundStatsService.ensurePlayer(player.getUniqueId());
            preparePlayerForRound(player);
            applyPlayerTeamVisual(player);
            
            Location spawn = config.getTeamSpawns(currentMapCode).get(teamId);
            if (spawn != null) {
                player.teleport(spawn);
            }
            
            // Гарантированная выдача кита
            applySelectedKit(player);
            player.sendMessage("§aИгра началась! Ваш кит: §e" + kitSelectionService.getSelectedKit(player).displayName());
        }
        final var map = config.getMaps().get(currentMapCode);
        broadcast("§aИгра началась! Карта: %s. Режим: %s. %s".formatted(map.displayName(), currentMode.getRuName(), currentMode.getTargetMessage()));
        WorldBorder border = Objects.requireNonNull(Bukkit.getWorld("world")).getWorldBorder();
        border.setSize(map.worldBorderSize());
        border.setCenter(map.worldBorderCenter().getX(), map.worldBorderCenter().getZ());
        startRunningTask();
        updateBossBar();
        mapSelectionService.clearVotes();
    }

    private void startRunningTask() {
        if (runningTask != null) {
            runningTask.cancel();
        }
        runningTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickRunningGame, 20L, 20L);
    }

    private void startFfaGame() {
        phase = Phase.RUNNING;
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        countdownLeft = 0;
        alivePlayers.clear();
        remainingRespawns.clear();
        queuedRespawns.clear();
        roundStatsService.clear();
        gameTimeLeftSeconds = 6 * 60; // 6 minutes for FFA

        // Полный сброс всех команд для FFA
        teamSelectionService.clearAll();

        World world = Bukkit.getWorld("world");
        GameMap mapData = config.getMaps().get(currentMapCode);
        WorldBorder border = world.getWorldBorder();
        border.setSize(mapData.worldBorderSize());
        border.setCenter(mapData.worldBorderCenter().getX(), mapData.worldBorderCenter().getZ());

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isObserver(player)) {
                player.getInventory().clear();
                player.setGameMode(GameMode.SPECTATOR);
                teleportToLobby(player);
                continue;
            }
            
            alivePlayers.add(player.getUniqueId());
            remainingRespawns.put(player.getUniqueId(), Integer.MAX_VALUE); // Infinite respawns in FFA
            roundStatsService.ensurePlayer(player.getUniqueId());
            preparePlayerForRound(player);
            applyPlayerTeamVisual(player); // Обновит визуализацию (уберет префиксы команд)
            
            Location spawn = findRandomFfaSpawn(world, border);
            player.teleport(spawn);
            
            // Принудительная выдача кита
            applySelectedKit(player);
            player.sendMessage("§aВам выдан кит: §e" + kitSelectionService.getSelectedKit(player).displayName());
        }

        broadcast("§aИгра началась! Режим: FFA (Каждый сам за себя). Цель: больше всех убийств за 6 минут!");
        startRunningTask();
        updateBossBar();
        mapSelectionService.clearVotes();
    }

    private Location findRandomFfaSpawn(World world, WorldBorder border) {
        double size = border.getSize() / 2.0D - 2.0D;
        double centerX = border.getCenter().getX();
        double centerZ = border.getCenter().getZ();

        for (int i = 0; i < 20; i++) {
            double x = centerX + (ThreadLocalRandom.current().nextDouble() * size * 2.0D - size);
            double z = centerZ + (ThreadLocalRandom.current().nextDouble() * size * 2.0D - size);
            int y = world.getHighestBlockYAt((int) x, (int) z);
            
            Block block = world.getBlockAt((int) x, y, (int) z);
            if (block.getType().isSolid() && y < world.getMaxHeight() - 2) {
                return new Location(world, x + 0.5D, y + 1.1D, z + 0.5D);
            }
        }
        return border.getCenter().clone().add(0, 10, 0); // Fallback
    }

    private void tickRunningGame() {
        if (phase != Phase.RUNNING) {
            return;
        }
        gameTimeLeftSeconds--;
        if (gameTimeLeftSeconds <= 0) {
            endByTime();
            return;
        }

        if (currentMode != GameMap.Mode.FFA) {
            if (checkAliveWinCondition()) {
                return;
            }
            tickCapturePoints();
            sendCaptureActionBars();
            checkCaptureWinCondition();
            spawnCaptureParticles();
        } else {
            sendFfaActionBars();
        }

        checkSpawnCamping();
        updateBossBar();
    }

    private void sendFfaActionBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isObserver(player)) continue;
            int kills = roundStatsService.getKills(player.getUniqueId());
            player.sendActionBar("§eВаши убийства: §f" + kills + " §8| §eДо конца: §f" + formatTime(gameTimeLeftSeconds));
        }
    }

    private void checkSpawnCamping() {
        if (currentMode == GameMap.Mode.FFA) return; // No spawn camping in FFA
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isObserver(player) || !alivePlayers.contains(player.getUniqueId())) {
                continue;
            }
            Integer myTeam = teamSelectionService.getSelectedTeam(player);
            if (myTeam == null) continue;

            int enemyTeam = myTeam == RED_TEAM ? BLUE_TEAM : RED_TEAM;
            Location enemySpawn = config.getTeamSpawns(currentMapCode).get(enemyTeam);

            if (enemySpawn != null && isLocationInSpawnProtection(player.getLocation(), enemySpawn)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false, false));
                int seconds = spawnCampingSeconds.getOrDefault(player.getUniqueId(), 0) + 1;
                spawnCampingSeconds.put(player.getUniqueId(), seconds);
                if (seconds >= 10) {
                    player.damage(1.0D); // 0.5 hearts
                    player.sendMessage("§cНе задерживайтесь на чужом спавне!");
                }
            } else {
                spawnCampingSeconds.remove(player.getUniqueId());
            }
        }
    }

    private boolean checkAliveWinCondition() {
        if (currentMode == GameMap.Mode.FFA) return false; // FFA wins by kills/time
        if (currentMode == GameMap.Mode.CP) return false; // CP wins by points or time
        int aliveTeamOne = countAliveOnlineInTeam(RED_TEAM);
        int aliveTeamTwo = countAliveOnlineInTeam(BLUE_TEAM);
        if (aliveTeamOne <= 0 && aliveTeamTwo <= 0) {
            endGame(0, "§eНичья: обе стороны выбыли.");
            return true;
        }
        if (aliveTeamOne <= 0) {
            endGame(BLUE_TEAM, "§aПобеда команды " + teamSelectionService.coloredTeamName(BLUE_TEAM) + "§a: все Красные выбыли.");
            return true;
        }
        if (aliveTeamTwo <= 0) {
            endGame(RED_TEAM, "§aПобеда команды " + teamSelectionService.coloredTeamName(RED_TEAM) + "§a: все Синие выбыли.");
            return true;
        }
        return false;
    }

    private void tickCapturePoints() {
        if (currentMode == GameMap.Mode.TDM || currentMode == GameMap.Mode.FFA || config.getCapturePoints(currentMapCode).isEmpty()) {
            return;
        }
        double perPlayer = config.getCapturePercentPerPlayerPerSecond();
        for (CapturePoint point : config.getCapturePoints(currentMapCode).values()) {
            if (currentMode != GameMap.Mode.FFA && currentMode != GameMap.Mode.TDM
                    && point.ownerTeam() == BLUE_TEAM) {
                // Захваченная точка в режиме "Захват" навсегда остаётся за нападающими (синими)
                continue;
            }
            Location center = getPointCenter(point);
            if (center == null) {
                continue;
            }
            int t1 = countAliveNear(RED_TEAM, center);
            int t2 = countAliveNear(BLUE_TEAM, center);
            int redTeamProgressDiff = t1 - t2;
            if (redTeamProgressDiff != 0) {
                point.setProgress(point.progress() + perPlayer * redTeamProgressDiff);
            }
            if (currentMode != GameMap.Mode.FFA && currentMode != GameMap.Mode.TDM && point.progress() < 0.0D) {
                final var allPreviousPointsCaptured = config.getCapturePoints(currentMapCode).values().stream()
                        .filter(aPoint -> aPoint.id() < point.id())
                        .allMatch(aPoint -> aPoint.ownerTeam() == BLUE_TEAM);
                if (!allPreviousPointsCaptured) {
                    // Синие могут захватывать точки только в последовательности их создания на карте
                    point.setProgress(0.0D);
                    updateCaptureOverlay(point);
                    continue;
                }
            }
            if (currentMode != GameMap.Mode.FFA && currentMode != GameMap.Mode.TDM && point.progress() > 0.0D) {
                // Красные не могут захватить точку
                point.setProgress(0.0D);
                updateCaptureOverlay(point);
                continue;
            }
            if (point.progress() >= 100D) {
                point.setProgress(100D);
                if (point.ownerTeam() != RED_TEAM) {
                    point.setOwnerTeam(RED_TEAM);
                    broadcast("§aТочка %s захвачена: §cКрасные§a.".formatted(point.displayName()));
                    rewardCaptureForPoint(point, RED_TEAM);
                }
            } else if (point.progress() <= -100D) {
                point.setProgress(-100D);
                if (point.ownerTeam() != BLUE_TEAM) {
                    point.setOwnerTeam(BLUE_TEAM);
                    broadcast("§aТочка %s захвачена: §9Синие§a.".formatted(point.displayName()));
                    rewardCaptureForPoint(point, BLUE_TEAM);
                }
            } else {
                point.setOwnerTeam(0);
            }
            updateCaptureOverlay(point);
        }
    }

    public boolean areTeammates(Player one, Player two) {
        if (one == null || two == null) {
            return false;
        }
        if (currentMode == GameMap.Mode.FFA) {
            return false; // No teammates in FFA
        }
        if (isObserver(one) || isObserver(two)) {
            return false;
        }
        Integer t1 = teamSelectionService.getSelectedTeam(one);
        Integer t2 = teamSelectionService.getSelectedTeam(two);
        return t1 != null && t1.equals(t2);
    }

    public String getColoredPlayerName(Player player) {
        if (player == null) {
            return "§7Игрок";
        }
        if (currentMode == GameMap.Mode.FFA) {
            return "§f" + player.getName(); // White name in FFA
        }
        return teamVisualService.colorizeName(player.getName(), teamSelectionService.getSelectedTeam(player), RED_TEAM, BLUE_TEAM);
    }

    private void sendCaptureActionBars() {
        int total = Math.max(1, config.getCapturePoints(currentMapCode).size());
        int owned1 = getOwnedPoints(RED_TEAM);
        int owned2 = getOwnedPoints(BLUE_TEAM);
        int p1 = getTeamControlPercent(RED_TEAM);
        int p2 = getTeamControlPercent(BLUE_TEAM);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isObserver(player)) {
                continue;
            }
            Integer team = teamSelectionService.getSelectedTeam(player);
            if (team != null && phase == Phase.RUNNING) {
                String extra = getPointStatusForPlayer(player);
                String bar = "§cКрасные: §f" + owned1 + "/" + total + " (" + p1 + "%) §8| §9Синие: §f" + owned2 + "/" + total + " (" + p2 + "%)" + extra;
                player.sendActionBar(bar);
            }
        }
    }

    private void endGame(int winnerTeam, String message) {
        WorldBorder border = Objects.requireNonNull(Bukkit.getWorld("world")).getWorldBorder();
        border.setSize(10000);
        border.setCenter(0, 0);
        phase = Phase.WAITING;
        
        // Очищаем списки живых игроков и респавнов
        alivePlayers.clear();
        remainingRespawns.clear();
        queuedRespawns.clear();
        pendingRunningKitChoice.clear();
        resetCapturePointsForRound();
        gameTimeLeftSeconds = 0;
        countdownLeft = 0;

        if (runningTask != null) {
            runningTask.cancel();
            runningTask = null;
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        clearCaptureOverlays();

        broadcast(message);
        roundStatsService.announceRoundLeaders(this::resolveColoredPlayerName);
        cleanupDroppedItems();
        
        // Сбрасываем команды ПЕРЕД восстановлением карты и лобби
        if (currentMode == GameMap.Mode.FFA) {
            teamSelectionService.clearAll();
        }

        restoreMapChanges(() -> {
            // Re-calculate observers based on current game mode before teleporting
            for (Player player : Bukkit.getOnlinePlayers()) {
                prepareLobbyPlayer(player);
                applyPlayerTeamVisual(player);
                teleportToLobby(player);
                
                if (winnerTeam > 0) {
                    Integer team = teamSelectionService.getSelectedTeam(player);
                    if (team != null && team == winnerTeam) {
                        player.sendMessage("§6Вы победили в раунде!");
                    } else if (team != null) {
                        player.sendMessage("§7Ваша команда проиграла раунд.");
                    }
                }
            }
            
            // Re-balance for next game
            if (currentMode != GameMap.Mode.FFA) {
                teamSelectionService.rebalanceTeamsAfterRound();
            }
            
            broadcast("§aКарта восстановлена в исходное состояние.");
        });
        updateBossBar();
    }

    private int countAliveOnlineInTeam(int teamId) {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!alivePlayers.contains(player.getUniqueId())) {
                continue;
            }
            if (isObserver(player)) {
                continue;
            }
            Integer team = teamSelectionService.getSelectedTeam(player);
            if (team != null && team == teamId) {
                count++;
            }
        }
        return count;
    }

    private int countAliveNear(int teamId, Location center) {
        if (center == null || center.getWorld() == null) {
            return 0;
        }
        int count = 0;
        double radiusSquared = MiniGameService.CAPTURE_RADIUS * MiniGameService.CAPTURE_RADIUS;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!alivePlayers.contains(player.getUniqueId())) {
                continue;
            }
            if (isObserver(player)) {
                continue;
            }
            Integer team = teamSelectionService.getSelectedTeam(player);
            if (team == null || team != teamId) {
                continue;
            }
            Location here = player.getLocation();
            if (here.getWorld() != center.getWorld()) {
                continue;
            }
            if (here.distanceSquared(center) <= radiusSquared) {
                count++;
            }
        }
        return count;
    }

    private void prepareLobbyPlayer(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20F);
        player.setFireTicks(0);
        player.setWalkSpeed(0.2F);
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        teamSelectionService.giveTeamSelectorItem(player);
        kitSelectionService.giveKitSelectorItem(player);
        mapSelectionService.giveMapSelectorItem(player);
    }

    private void teleportToLobby(Player player) {
        if (config.getLobbySpawn() != null) {
            player.teleport(config.getLobbySpawn());
        }
    }

    private void preparePlayerForRound(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20F);
        player.setFireTicks(0);
        player.setWalkSpeed(0.2F); // Сброс скорости до стандартной
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private void applySelectedKit(Player player) {
        playerItemsService.giveAllItems(
                player,
                kitSelectionService.getSelectedKit(player),
                teamSelectionService.getSelectedTeam(player)
        );
    }

    private void spawnChunkBorderParticles(World world, CapturePoint state, Particle.DustOptions dust) {
        int minX = state.pointX() - 8;
        int minZ = state.pointZ() - 8;
        int maxX = state.pointX() + 8;
        int maxZ = state.pointZ() + 8;
        for (int x = minX; x <= maxX; x++) {
            spawnBorderPoint(world, x, minZ, dust);
            spawnBorderPoint(world, x, maxZ, dust);
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            spawnBorderPoint(world, minX, z, dust);
            spawnBorderPoint(world, maxX, z, dust);
        }
    }

    private void spawnBorderPoint(World world, int x, int z, Particle.DustOptions dust) {
        int y = findGroundYIgnoringBarriers(world, x, z) + 1;
        world.spawnParticle(Particle.DUST, x + 0.5D, y + 0.05D, z + 0.5D, 1, 0.0, 0.0, 0.0, 0.0, dust, true);
    }

    private int findGroundYIgnoringBarriers(World world, int x, int z) {
        if (world == null) {
            return 64;
        }
        int min = world.getMinHeight();
        int start = Math.min(world.getMaxHeight() - 1, world.getHighestBlockYAt(x, z) + 16);
        for (int y = start; y >= min; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR || type == Material.BARRIER) {
                continue;
            }
            return y;
        }
        return min;
    }

    private void broadcast(String message) {
        Bukkit.broadcastMessage(message);
    }

    private void attachBossBarPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!statusBossBar.getPlayers().contains(player)) {
                statusBossBar.addPlayer(player);
            }
        }
    }

    private void updateBossBar() {
        attachBossBarPlayers();
        int maxPlayers = config.getTeamCount() * config.getPlayersPerTeam();
        int ready = getReadyPlayersCount();
        switch (phase) {
            case WAITING -> {
                statusBossBar.setColor(BarColor.WHITE);
                statusBossBar.setTitle("Ожидание игроков: " + ready + "/" + maxPlayers);
                double progress = maxPlayers <= 0 ? 0D : (double) ready / (double) maxPlayers;
                statusBossBar.setProgress(clamp01(progress));
            }
            case COUNTDOWN -> {
                statusBossBar.setColor(BarColor.YELLOW);
                statusBossBar.setTitle("Отсчет до начала игры: " + countdownLeft + "с");
                double progress = countdownInitial <= 0 ? 0D : (double) countdownLeft / (double) countdownInitial;
                statusBossBar.setProgress(clamp01(progress));
            }
            case RUNNING -> {
                statusBossBar.setColor(BarColor.PURPLE);
                if (currentMode == GameMap.Mode.FFA) {
                    statusBossBar.setTitle("§eFFA §8| §eДо конца: §f" + formatTime(gameTimeLeftSeconds));
                } else {
                    int p1 = getTeamControlPercent(RED_TEAM);
                    int p2 = getTeamControlPercent(BLUE_TEAM);
                    int total = Math.max(1, config.getCapturePoints(currentMapCode).size());
                    int owned1 = getOwnedPoints(RED_TEAM);
                    int owned2 = getOwnedPoints(BLUE_TEAM);
                    statusBossBar.setTitle("§eДо конца: §f" + formatTime(gameTimeLeftSeconds) + " §8| §cКрасные §f" + owned1 + "/" + total + " (" + p1 + "%) §8| §9Синие §f" + owned2 + "/" + total + " (" + p2 + "%)");
                }
                int totalTime = Math.max(1, config.getGameDurationMinutes() * 60);
                double progress = (double) gameTimeLeftSeconds / (double) totalTime;
                statusBossBar.setProgress(clamp01(progress));
            }
        }
    }

    private double clamp01(double value) {
        if (value < 0D) {
            return 0D;
        }
        return Math.min(value, 1D);
    }

    private String formatTime(int seconds) {
        int s = Math.max(0, seconds);
        int m = s / 60;
        int sec = s % 60;
        return String.format("%02d:%02d", m, sec);
    }

    private void endByTime() {
        if (currentMode == GameMap.Mode.FFA) {
            endFfaGame();
            return;
        }
        int owned1 = getOwnedPoints(RED_TEAM);
        int owned2 = getOwnedPoints(BLUE_TEAM);
        if (owned1 > owned2) {
            endGame(RED_TEAM, "§aВремя вышло. Победа Красных по прогрессу захвата.");
            return;
        }
        if (owned2 > owned1) {
            endGame(BLUE_TEAM, "§aВремя вышло. Победа Синих по прогрессу захвата.");
            return;
        }
        int alive1 = countAliveOnlineInTeam(RED_TEAM);
        int alive2 = countAliveOnlineInTeam(BLUE_TEAM);
        if (alive1 > alive2) {
            endGame(RED_TEAM, "§aВремя вышло. Победа Красных по числу выживших.");
            return;
        }
        if (alive2 > alive1) {
            endGame(BLUE_TEAM, "§aВремя вышло. Победа Синих по числу выживших.");
            return;
        }
        endGame(0, "§eВремя вышло. Ничья.");
    }

    private void endFfaGame() {
        UUID winner = null;
        int maxKills = -1;
        for (UUID uuid : alivePlayers) {
            int kills = roundStatsService.getKills(uuid);
            if (kills > maxKills) {
                maxKills = kills;
                winner = uuid;
            } else if (kills == maxKills && maxKills != -1) {
                // Potential tie, could handle but winner candidates logic is more complex
            }
        }

        String winnerName = winner == null ? "Никто" : Bukkit.getOfflinePlayer(winner).getName();
        endGame(0, "§aВремя вышло! Победитель FFA: §e" + winnerName + " §a(Убийств: " + maxKills + ")");
    }

    private void resetCapturePointsForRound() {
        for (CapturePoint state : config.getCapturePoints(currentMapCode).values()) {
            state.setProgress(0D);
            state.setOwnerTeam(0);
        }
    }

    private int getOwnedPoints(int teamId) {
        int count = 0;
        for (CapturePoint state : config.getCapturePoints(currentMapCode).values()) {
            if (state.ownerTeam() == teamId) {
                count++;
            }
        }
        return count;
    }

    private int getTeamControlPercent(int teamId) {
        if (config.getCapturePoints(currentMapCode).isEmpty()) {
            return 0;
        }
        double sum = 0D;
        for (CapturePoint point : config.getCapturePoints(currentMapCode).values()) {
            if (teamId == 1) {
                sum += Math.max(0D, point.progress());
            } else if (teamId == 2) {
                sum += Math.max(0D, -point.progress());
            }
        }
        double max = config.getCapturePoints(currentMapCode).size() * 100.0D;
        return (int) Math.round((sum / max) * 100.0D);
    }

    private void checkCaptureWinCondition() {
        if (config.getCapturePoints(currentMapCode).isEmpty()) {
            return;
        }
        int total = config.getCapturePoints(currentMapCode).size();
        if (getOwnedPoints(RED_TEAM) >= total) {
            endGame(RED_TEAM, "§aПобеда Красных: захвачены все точки.");
            return;
        }
        if (getOwnedPoints(BLUE_TEAM) >= total) {
            endGame(BLUE_TEAM, "§aПобеда Синих: захвачены все точки.");
        }
    }

    private void spawnCaptureParticles() {
        if (currentMode == GameMap.Mode.FFA || currentMode == GameMap.Mode.TDM) return;
        for (CapturePoint state : config.getCapturePoints(currentMapCode).values()) {
            World world = Bukkit.getWorld(state.world());
            if (world == null) {
                continue;
            }
            double baseX = (state.markerX() >= 0 ? state.markerX() + 0.5D : state.pointX() + 0.5D);
            double baseZ = (state.markerZ() >= 0 ? state.markerZ() + 0.5D : state.pointZ() + 0.5D);
            Color color = switch (state.ownerTeam()) {
                case RED_TEAM -> Color.fromRGB(255, 70, 70);
                case BLUE_TEAM -> Color.fromRGB(70, 130, 255);
                default -> Color.fromRGB(240, 240, 240);
            };
            Particle.DustOptions dust = new Particle.DustOptions(color, 1.4F);
            int groundY = state.groundY() > 0
                    ? state.groundY()
                    : findGroundYIgnoringBarriers(world, (int) Math.floor(baseX), (int) Math.floor(baseZ)) + 1;
            int topY = state.markerY() >= 0 ? state.markerY() + 9 : groundY + 8;
            double height = Math.max(2.5D, topY - groundY + 0.8D);
            int steps = 38;
            for (int i = 0; i < steps; i++) {
                double t = (double) i / (double) (steps - 1);
                double angle = t * Math.PI * 7.0D;
                double radius = 2.0D - (t * 1.2D);
                double x = baseX + Math.cos(angle) * radius;
                double z = baseZ + Math.sin(angle) * radius;
                double y = groundY + t * height;
                world.spawnParticle(Particle.DUST, x, y, z, 1, 0.0, 0.0, 0.0, 0.0, dust, true);
                world.spawnParticle(Particle.DUST, baseX - (x - baseX), y, baseZ - (z - baseZ), 1, 0.0, 0.0, 0.0, 0.0, dust, true);
            }
            spawnChunkBorderParticles(world, state, dust);
        }
    }

    private String getPointStatusForPlayer(Player player) {
        if (config.getCapturePoints(currentMapCode).isEmpty()) {
            return "";
        }
        CapturePoint point = findPointForPlayer(player);
        if (point == null) {
            return "";
        }
        int pct = (int) Math.round(Math.abs(point.progress()));
        String bar = buildProgressBar(pct);
        if (point.ownerTeam() == RED_TEAM) {
            return " §8| §cТочка: Красные " + pct + "% " + bar;
        }
        if (point.ownerTeam() == BLUE_TEAM) {
            return " §8| §9Точка: Синие " + pct + "% " + bar;
        }
        if (point.progress() > 0) {
            return " §8| §cЗахват Красных: " + pct + "% " + bar;
        }
        if (point.progress() < 0) {
            return " §8| §9Захват Синих: " + pct + "% " + bar;
        }
        return " §8| §7Точка нейтральна " + buildProgressBar(0);
    }

    private String buildProgressBar(int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        int full = clamped / 10;
        StringBuilder sb = new StringBuilder("§f[");
        for (int i = 0; i < 10; i++) {
            sb.append(i < full ? "§a|" : "§7|");
        }
        sb.append("§f]");
        return sb.toString();
    }

    private void deployCaptureOverlays() {
        if (currentMode == GameMap.Mode.FFA || currentMode == GameMap.Mode.TDM) return;
        for (CapturePoint state : config.getCapturePoints(currentMapCode).values()) {
            World world = Bukkit.getWorld(state.world());
            if (world == null) {
                continue;
            }
            int x = state.pointX();
            int z = state.pointZ();
            int maxY = world.getMaxHeight() - 1;
            int ground = findGroundYIgnoringBarriers(world, x, z);
            int baseY = Math.min(maxY - 6, ground + 26);
            state.setMarkerY(baseY);
            state.setMarkerX(x);
            state.setMarkerZ(z);
            state.setGroundY(ground + 1);
            placeCaptureMarkerBlock(world, x, baseY, z, Material.GLOWSTONE);
            placeCaptureMarkerBlock(world, x + 1, baseY, z, Material.GLOWSTONE);
            placeCaptureMarkerBlock(world, x - 1, baseY, z, Material.GLOWSTONE);
            placeCaptureMarkerBlock(world, x, baseY, z + 1, Material.GLOWSTONE);
            placeCaptureMarkerBlock(world, x, baseY, z - 1, Material.GLOWSTONE);
            for (int level = 1; level <= 9; level++) {
                int radius = crystalRadius(level);
                placeCrystalLayer(world, x, baseY + level, z, Material.WHITE_STAINED_GLASS, radius);
            }
        }
    }

    private void clearCaptureOverlays() {
        for (BlockState state : captureMarkerOriginalStates.values()) {
            if (!state.getChunk().isLoaded()) {
                state.getChunk().load();
            }
            state.update(true, true);
        }
        captureMarkerOriginalStates.clear();
        protectedPointBlocks.clear();
        if (currentMapCode == null) {
            return;
        }
        for (CapturePoint state : config.getCapturePoints(currentMapCode).values()) {
            state.setMarkerY(-1);
            state.setMarkerX(-1);
            state.setMarkerZ(-1);
            state.setGroundY(-1);
        }
    }

    private void placeCrystalLayer(World world, int centerX, int y, int centerZ, Material type, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.abs(x) + Math.abs(z) > radius) {
                    continue;
                }
                placeCaptureMarkerBlock(world, centerX + x, y, centerZ + z, type);
            }
        }
    }

    private void placeCaptureMarkerBlock(World world, int x, int y, int z, Material type) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return;
        }
        Block block = world.getBlockAt(x, y, z);
        String key = blockKey(block);
        captureMarkerOriginalStates.putIfAbsent(key, block.getState());
        block.setType(type, true);
        protectedPointBlocks.add(key);
    }

    private void updateCaptureOverlay(CapturePoint state) {
        if (state.markerY() < 0 || state.markerX() < 0 || state.markerZ() < 0) {
            return;
        }
        World world = Bukkit.getWorld(state.world());
        if (world == null) {
            return;
        }
        List<Block> crystal = getCrystalBlocks(world, state);
        Material targetColor = Material.WHITE_STAINED_GLASS;
        if (state.ownerTeam() == RED_TEAM) {
            targetColor = Material.RED_STAINED_GLASS;
        } else if (state.ownerTeam() == BLUE_TEAM) {
            targetColor = Material.BLUE_STAINED_GLASS;
        }
        for (Block block : crystal) {
            if (block.getType() == Material.GLOWSTONE) {
                continue;
            }
            if (block.getType() != targetColor) {
                block.setType(targetColor, true);
            }
        }
        Block top = world.getBlockAt(state.markerX(), state.markerY() + 9, state.markerZ());
        Block upperCore = world.getBlockAt(state.markerX(), state.markerY() + 8, state.markerZ());
        Material topTarget = targetColor;
        if (upperCore.getType() != topTarget) {
            upperCore.setType(topTarget, true);
        }
        if (top.getType() != topTarget) {
            top.setType(topTarget, true);
        }
    }

    private List<Block> getCrystalBlocks(World world, CapturePoint state) {
        List<Block> blocks = new ArrayList<>();
        int x = state.markerX();
        int z = state.markerZ();
        int y = state.markerY();
        for (int level = 1; level <= 9; level++) {
            appendLayer(world, blocks, x, y + level, z, crystalRadius(level));
        }
        return blocks;
    }

    private int crystalRadius(int level) {
        return switch (level) {
            case 1 -> 4;
            case 2 -> 4;
            case 3 -> 3;
            case 4 -> 3;
            case 5 -> 2;
            case 6 -> 2;
            case 7 -> 1;
            case 8 -> 1;
            default -> 0;
        };
    }

    private void appendLayer(World world, List<Block> out, int centerX, int y, int centerZ, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.abs(x) + Math.abs(z) > radius) {
                    continue;
                }
                out.add(world.getBlockAt(centerX + x, y, centerZ + z));
            }
        }
    }

    private String blockKey(Block block) {
        return blockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    private String blockKey(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    private void cleanupDroppedItems() {
        Set<String> worlds = ConcurrentHashMap.newKeySet();
        if (config.getLobbySpawn() != null && config.getLobbySpawn().getWorld() != null) {
            worlds.add(config.getLobbySpawn().getWorld().getName());
        }
        for (Location loc : config.getTeamSpawns(currentMapCode).values()) {
            if (loc != null && loc.getWorld() != null) {
                worlds.add(loc.getWorld().getName());
            }
        }
        for (CapturePoint point : config.getCapturePoints(currentMapCode).values()) {
            worlds.add(point.world());
        }
        for (String worldName : worlds) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }
            world.getEntitiesByClass(org.bukkit.entity.Item.class).forEach(org.bukkit.entity.Item::remove);
        }
    }

    private void restoreMapChanges(Runnable done) {
        if (restoreTask != null) {
            restoreTask.cancel();
            restoreTask = null;
        }
        if (changedBlocks.isEmpty()) {
            done.run();
            return;
        }
        List<BlockState> snapshot = new ArrayList<>(changedBlocks.values());
        Set<String> chunkSnapshot = Set.copyOf(changedChunks);
        changedBlocks.clear();
        changedChunks.clear();
        AtomicInteger index = new AtomicInteger(0);
        restoreTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int restored = 0;
            while (restored < 500 && index.get() < snapshot.size()) {
                BlockState state = snapshot.get(index.getAndIncrement());
                if (!state.getChunk().isLoaded()) {
                    state.getChunk().load();
                }
                restoreBlockWithPhysics(state);
                restored++;
            }
            if (index.get() >= snapshot.size()) {
                if (restoreTask != null) {
                    restoreTask.cancel();
                    restoreTask = null;
                }
                clearResidualFire(chunkSnapshot);
                refreshChangedChunks(chunkSnapshot);
                done.run();
            }
        }, 1L, 1L);
    }

    private void restoreBlockWithPhysics(BlockState state) {
        Material type = state.getType();

        if (isMultiBlockPart(type)) {
            restoreMultiBlockStructure(state, type);
        } else {
            state.update(true, true);
        }
    }

    private boolean isMultiBlockPart(Material type) {
        String name = type.name();
        return name.contains("DOOR") || name.contains("BED") ||
               type == Material.FARMLAND || type == Material.DIRT_PATH ||
               name.contains("CHEST") || type == Material.BREWING_STAND ||
               name.contains("GATE") || name.contains("STAIRS") ||
               name.contains("SLAB") || name.contains("WALL") ||
               name.contains("PISTON") || name.contains("DISPENSER") ||
               name.contains("DROPPER") || name.contains("HOPPER") ||
               name.contains("REPEATER") || name.contains("COMPARATOR") ||
               name.contains("BUTTON") || name.contains("LEVER") ||
               name.contains("PRESSURE_PLATE") || name.contains("TRIPWIRE") ||
               name.contains("SIGN") || name.contains("BANNER") ||
               name.contains("LANTERN") || name.contains("TORCH") ||
               name.contains("FENCE") || name.contains("PANE");
    }

    private void restoreMultiBlockStructure(BlockState state, Material type) {
        if (type.name().contains("DOOR")) {
            restoreDoor(state.getBlock());
        } else if (type.name().contains("BED")) {
            restoreBed(state.getBlock());
        } else {
            state.update(true, true);
        }
    }

    private void restoreDoor(Block block) {
        // Doors are tricky because they have two parts.
        // We hope both parts were recorded and will be restored individually.
        // But to be safe, we can try to restore the current block state if it was a door.
        block.getState().update(true, true);
    }

    private void restoreBed(Block block) {
        block.getState().update(true, true);
    }

    private String chunkKey(Block block) {
        return block.getWorld().getName() + ":" + block.getChunk().getX() + ":" + block.getChunk().getZ();
    }

    private void clearResidualFire(Set<String> chunkKeys) {
        for (String key : chunkKeys) {
            String[] parts = key.split(":");
            if (parts.length != 3) {
                continue;
            }
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                continue;
            }
            int cx;
            int cz;
            try {
                cx = Integer.parseInt(parts[1]);
                cz = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (!world.isChunkLoaded(cx, cz)) {
                world.loadChunk(cx, cz);
            }
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int wx = (cx << 4) + lx;
                    int wz = (cz << 4) + lz;
                    int top = world.getHighestBlockYAt(wx, wz);
                    int from = Math.min(world.getMaxHeight() - 1, top + 6);
                    int to = Math.max(world.getMinHeight(), top - 30);
                    for (int y = from; y >= to; y--) {
                        Block block = world.getBlockAt(wx, y, wz);
                        Material type = block.getType();
                        if (type == Material.FIRE || type == Material.SOUL_FIRE) {
                            block.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }
    }

    private void refreshChangedChunks(Set<String> chunkKeys) {
        for (String key : chunkKeys) {
            String[] parts = key.split(":");
            if (parts.length != 3) {
                continue;
            }
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                continue;
            }
            try {
                int cx = Integer.parseInt(parts[1]);
                int cz = Integer.parseInt(parts[2]);
                if (!world.isChunkLoaded(cx, cz)) {
                    world.loadChunk(cx, cz);
                }
                world.refreshChunk(cx, cz);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private Location getPointCenter(CapturePoint state) {
        if (state == null) {
            return null;
        }
        World world = Bukkit.getWorld(state.world());
        if (world == null) {
            return null;
        }
        return new Location(world, state.pointX() + 0.5D, state.pointY() + 1.0D, state.pointZ() + 0.5D);
    }

    private void autoAssignTeamIfNeeded(Player player) {
        if (phase == Phase.RUNNING || currentMode == GameMap.Mode.FFA) {
            return;
        }
        teamSelectionService.autoAssignTeamIfNeeded(player, config);
        applyPlayerTeamVisual(player);
    }

    private void adjustPlayersPerTeamByOnline() {
        int online = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isObserver(player)) {
                continue;
            }
            online++;
        }
        int target = 8 + Math.max(0, (online - 15) / 2);
        if (config.getPlayersPerTeam() != target) {
            config.setPlayersPerTeam(target);
            updateBossBar();
        }
    }

    private boolean isLocationInSpawnProtection(Location loc, Location spawn) {
        if (loc == null || spawn == null || loc.getWorld() == null || spawn.getWorld() == null) {
            return false;
        }
        if (!loc.getWorld().equals(spawn.getWorld())) {
            return false;
        }
        double dx = Math.abs(loc.getX() - spawn.getX());
        double dz = Math.abs(loc.getZ() - spawn.getZ());
        double dy = Math.abs(loc.getY() - spawn.getY());
        return dx <= 5.0D && dz <= 5.0D && dy <= 40.0D;
    }

    private void rewardCaptureForPoint(CapturePoint state, int teamId) {
        Location center = getPointCenter(state);
        if (center == null) {
            return;
        }
        double radiusSq = CAPTURE_RADIUS * CAPTURE_RADIUS;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isObserver(player)) {
                continue;
            }
            if (!alivePlayers.contains(player.getUniqueId())) {
                continue;
            }
            Integer team = teamSelectionService.getSelectedTeam(player);
            if (team == null || team != teamId) {
                continue;
            }
            if (!player.getWorld().getName().equals(state.world())) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) <= radiusSq) {
                roundStatsService.recordCapture(player.getUniqueId());
            }
        }
    }

    private CapturePoint findPointForPlayer(Player player) {
        if (player == null) {
            return null;
        }
        CapturePoint nearest = null;
        double best = Double.MAX_VALUE;
        for (CapturePoint point : config.getCapturePoints(currentMapCode).values()) {
            if (!player.getWorld().getName().equals(point.world())) {
                continue;
            }
            Location center = getPointCenter(point);
            if (center == null) {
                continue;
            }
            double dist = player.getLocation().distanceSquared(center);
            if (dist <= CAPTURE_RADIUS * CAPTURE_RADIUS && dist < best) {
                best = dist;
                nearest = point;
            }
        }
        return nearest;
    }

    private void applyPlayerTeamVisual(Player player) {
        if (currentMode == GameMap.Mode.FFA) {
            teamVisualService.removePlayer(player);
            return;
        }
        Integer teamId = player == null ? null : teamSelectionService.getSelectedTeam(player);
        teamVisualService.applyPlayerTeamVisual(player, teamId, RED_TEAM, BLUE_TEAM);
    }

    private String resolveColoredPlayerName(UUID playerId) {
        if (playerId == null) {
            return "§7Игрок";
        }
        if (currentMode == GameMap.Mode.FFA) {
            String name = Bukkit.getOfflinePlayer(playerId).getName();
            return "§f" + (name != null ? name : "§7Игрок");
        }
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            return getColoredPlayerName(online);
        }
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return teamVisualService.colorizeName(name, teamSelectionService.getSelectedTeam(playerId), RED_TEAM, BLUE_TEAM);
    }

    private int nextCapturePointId(String mapCode) {
        int max = 0;
        for (Integer id : config.getCapturePoints(mapCode).keySet()) {
            if (id != null && id > max) {
                max = id;
            }
        }
        return max + 1;
    }

    public boolean isMapWithCodeExistent(String mapCode) {
        return config.getMaps().containsKey(mapCode);
    }

    public enum Phase {
        WAITING,
        COUNTDOWN,
        RUNNING
    }
}


