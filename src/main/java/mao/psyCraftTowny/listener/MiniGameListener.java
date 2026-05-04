package mao.psyCraftTowny.listener;

import mao.psyCraftTowny.service.KitSelectionService;
import mao.psyCraftTowny.service.MapSelectionService;
import mao.psyCraftTowny.service.MiniGameService;
import mao.psyCraftTowny.service.TeamSelectionService;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MiniGameListener implements Listener {
    private final MiniGameService miniGameService;
    private final TeamSelectionService teamSelectionService;
    private final MapSelectionService mapSelectionService;
    private final KitSelectionService kitSelectionService;

    public MiniGameListener(MiniGameService miniGameService, TeamSelectionService teamSelectionService, MapSelectionService mapSelectionService, KitSelectionService kitSelectionService) {
        this.miniGameService = miniGameService;
        this.teamSelectionService = teamSelectionService;
        this.mapSelectionService = mapSelectionService;
        this.kitSelectionService = kitSelectionService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        miniGameService.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        miniGameService.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();
        if (killer != null) {
            miniGameService.handlePlayerKill(killer);
        }
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        miniGameService.handlePlayerDeath(player);
        event.setDroppedExp(0);
        event.getDrops().clear();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        event.setRespawnLocation(miniGameService.resolveRespawnLocation(player));
        miniGameService.handleRespawn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (miniGameService.isProtectedPointBlock(event.getBlock()) || miniGameService.isInSpawnProtection(event.getBlock())) {
            event.setCancelled(true);
            return;
        }
        miniGameService.recordBlockState(event.getBlockReplacedState());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (miniGameService.isProtectedPointBlock(event.getBlock()) || miniGameService.isInSpawnProtection(event.getBlock())) {
            event.setCancelled(true);
            return;
        }
        miniGameService.recordBlockState(event.getBlock().getState());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        org.bukkit.block.Block target = event.getBlockClicked().getRelative(event.getBlockFace());
        if (miniGameService.isProtectedPointBlock(target) || miniGameService.isInSpawnProtection(target)) {
            event.setCancelled(true);
            return;
        }
        miniGameService.recordBlockChange(target);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (miniGameService.isProtectedPointBlock(event.getBlockClicked()) || miniGameService.isInSpawnProtection(event.getBlockClicked())) {
            event.setCancelled(true);
            return;
        }
        miniGameService.recordBlockChange(event.getBlockClicked());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> miniGameService.isProtectedPointBlock(block) || miniGameService.isInSpawnProtection(block));
        for (org.bukkit.block.Block block : event.blockList()) {
            miniGameService.recordBlockChange(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> miniGameService.isProtectedPointBlock(block) || miniGameService.isInSpawnProtection(block));
        for (org.bukkit.block.Block block : event.blockList()) {
            miniGameService.recordBlockChange(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (miniGameService.isProtectedPointBlock(event.getBlock()) || miniGameService.isInSpawnProtection(event.getBlock())) {
            event.setCancelled(true);
            return;
        }
        miniGameService.recordBlockState(event.getBlock().getState());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (miniGameService.isProtectedPointBlock(event.getBlock()) || miniGameService.isInSpawnProtection(event.getBlock())) {
            event.setCancelled(true);
            return;
        }
        miniGameService.recordBlockState(event.getBlock().getState());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (miniGameService.isProtectedPointBlock(event.getBlock()) || miniGameService.isInSpawnProtection(event.getBlock())) {
            event.setCancelled(true);
            return;
        }
        miniGameService.recordBlockState(event.getBlock().getState());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        if (miniGameService.isProtectedPointBlock(event.getToBlock()) || miniGameService.isInSpawnProtection(event.getToBlock())) {
            event.setCancelled(true);
            return;
        }
        miniGameService.recordBlockState(event.getToBlock().getState());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (miniGameService.isProtectedPointBlock(event.getBlock()) || miniGameService.isInSpawnProtection(event.getBlock())) {
            event.setCancelled(true);
            return;
        }
        miniGameService.recordBlockState(event.getBlock().getState());
    }

    @EventHandler
    public void onSelectorUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (teamSelectionService.isTeamSelectorItem(item)) {
            event.setCancelled(true);
            miniGameService.openTeamSelector(player);
            return;
        }
        if (kitSelectionService.isKitSelectorItem(item)) {
            event.setCancelled(true);
            miniGameService.openKitSelector(player);
        }
        if (mapSelectionService.isMapSelectorItem(item)) {
            event.setCancelled(true);
            miniGameService.openMapSelector(player);
        }
    }

    @EventHandler
    public void onTeamSelectClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        boolean teamGui = teamSelectionService.isTeamMenuOpen(player);
        boolean kitGui = kitSelectionService.isKitMenuOpen(player);
        boolean mapGui = mapSelectionService.isMapMenuOpen(player);
        if (!teamGui && !kitGui && !mapGui) {
            String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
            teamGui = title.contains(teamSelectionService.getTeamGuiTitle());
            kitGui = title.contains(kitSelectionService.getKitGuiTitle());
            mapGui = title.contains(mapSelectionService.getMapGuiTitle());
            if (!teamGui && !kitGui && !mapGui) {
                return;
            }
        }
        event.setCancelled(true);
        Inventory clicked = event.getClickedInventory();
        Inventory top = event.getView().getTopInventory();
        if (clicked == null || !clicked.equals(top)) {
            return;
        }
        int slot = event.getSlot();
        if (teamGui && slot == 11) {
            miniGameService.selectTeam(player, 1);
            return;
        }
        if (teamGui && slot == 15) {
            miniGameService.selectTeam(player, 2);
            return;
        }
        if (kitGui && event.getCurrentItem() != null) {
            miniGameService.selectKitByMenuItem(player, event.getCurrentItem());
            return;
        }
        if (mapGui && event.getCurrentItem() != null) {
            miniGameService.selectMapByMenuItem(player, event.getCurrentItem());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            miniGameService.closeMenuTracking(player);
        }
    }

    @EventHandler
    public void onFriendlyPreStartDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player damager = null;
        if (event.getDamager() instanceof Player direct) {
            damager = direct;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            damager = shooter;
        }
        if (damager == null) {
            return;
        }

        if (miniGameService.areTeammates(damager, victim)) {
            event.setCancelled(true);
            damager.sendActionBar("§cНельзя бить союзников.");
            return;
        }

        if (miniGameService.getPhase() != MiniGameService.Phase.RUNNING) {
            event.setCancelled(true);
            if (damager.getGameMode() != GameMode.CREATIVE) {
                damager.sendActionBar("§7Урон отключен до старта игры.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            player.sendMessage("§cНаблюдатели не могут писать в чат.");
            return;
        }
        String colored = miniGameService.getColoredPlayerName(player);
        event.setFormat(colored + "§7: §f%2$s");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        event.setCancelled(true);
    }
}
