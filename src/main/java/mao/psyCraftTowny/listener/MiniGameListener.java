package mao.psyCraftTowny.listener;

import mao.psyCraftTowny.model.KitType;
import mao.psyCraftTowny.service.KitSelectionService;
import mao.psyCraftTowny.service.MapSelectionService;
import mao.psyCraftTowny.service.MiniGameService;
import mao.psyCraftTowny.service.TeamSelectionService;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
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
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MiniGameListener implements Listener {
    private final MiniGameService miniGameService;
    private final TeamSelectionService teamSelectionService;
    private final MapSelectionService mapSelectionService;
    private final KitSelectionService kitSelectionService;
    private final Map<UUID, Long> medicFeedingCooldown = new HashMap<>();

    public MiniGameListener(MiniGameService miniGameService, TeamSelectionService teamSelectionService, MapSelectionService mapSelectionService, KitSelectionService kitSelectionService) {
        this.miniGameService = miniGameService;
        this.teamSelectionService = teamSelectionService;
        this.mapSelectionService = mapSelectionService;
        this.kitSelectionService = kitSelectionService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        miniGameService.handlePlayerJoin(player);
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
        
        // If placing a tall block, record the block above too
        Material placedType = event.getBlockPlaced().getType();
        if (placedType.name().contains("DOOR") || placedType == Material.TALL_GRASS || placedType == Material.LARGE_FERN || placedType.name().contains("BANNER")) {
            miniGameService.recordBlockState(event.getBlock().getRelative(org.bukkit.block.BlockFace.UP).getState());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (miniGameService.isProtectedPointBlock(event.getBlock()) || miniGameService.isInSpawnProtection(event.getBlock())) {
            event.setCancelled(true);
            return;
        }
        miniGameService.recordBlockChange(event.getBlock());
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
            miniGameService.selectMapOrModeByMenuItem(player, event.getCurrentItem());
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

        // Medic feeding logic
        if (miniGameService.areTeammates(damager, victim) && kitSelectionService.getSelectedKit(damager) == KitType.MEDIC) {
            ItemStack item = damager.getInventory().getItemInMainHand();
            if (item.getType() == Material.GOLDEN_APPLE) {
                long now = System.currentTimeMillis();
                long last = medicFeedingCooldown.getOrDefault(damager.getUniqueId(), 0L);
                if (now - last >= 5000) {
                    if (victim.getHealth() < victim.getMaxHealth()) {
                        event.setCancelled(true);
                        item.setAmount(item.getAmount() - 1);
                        victim.setHealth(Math.min(victim.getMaxHealth(), victim.getHealth() + 4.0D)); // 2 hearts
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                        victim.sendMessage("§aМедик " + damager.getName() + " покормил вас золотым яблоком!");
                        damager.sendMessage("§aВы покормили " + victim.getName() + " золотым яблоком!");
                        medicFeedingCooldown.put(damager.getUniqueId(), now);
                        return;
                    }
                }
            }
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTankSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        if (kitSelectionService.getSelectedKit(player) == KitType.TANK && event.isSprinting()) {
            event.setCancelled(true);
            player.setSprinting(false);
            player.setWalkSpeed(0.08F);
        }
    }

    @EventHandler
    public void onTankMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (kitSelectionService.getSelectedKit(player) != KitType.TANK) return;

        // Если танк в воздухе (прыгнул), сбрасываем его горизонтальную инерцию
        if (!player.isOnGround() && event.getFrom().getY() < event.getTo().getY()) {
            Vector vec = player.getVelocity();
            if (vec.getX() != 0 || vec.getZ() != 0) {
                player.setVelocity(new Vector(vec.getX() * 0.5, vec.getY(), vec.getZ() * 0.5));
            }
        }
        
        // Постоянно форсим скорость, если вдруг она сбросилась
        if (player.getWalkSpeed() != 0.08F) {
            player.setWalkSpeed(0.08F);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage("§cКрафт запрещен!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        if (event.getBlock().getType() == Material.LAVA || event.getBlock().getType() == Material.LAVA_CAULDRON) {
            event.setCancelled(true);
            return;
        }
        if (miniGameService.isProtectedPointBlock(event.getToBlock()) || miniGameService.isInSpawnProtection(event.getToBlock())) {
            event.setCancelled(true);
            return;
        }
        miniGameService.recordBlockState(event.getToBlock().getState());
    }

    @EventHandler
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework firework && firework.getShooter() instanceof Player shooter) {
            if (kitSelectionService.getSelectedKit(shooter) == KitType.CROSSBOWMAN) {
                if (shooter.hasPotionEffect(PotionEffectType.STRENGTH)) {
                    event.setDamage(event.getDamage() * 1.5);
                }
            }
        }
    }

    @EventHandler
    public void onFishing(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) return;
        if (!(event.getCaught() instanceof Player victim)) return;
        
        Player fisher = event.getPlayer();
        boolean isTeammate = miniGameService.areTeammates(fisher, victim);
        
        // Сильное замедление только для врагов
        if (!isTeammate) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 4, false, false, true), true); // Замедление 5 на 4 сек (force: true)
            victim.sendMessage("§cВас поймали удочкой!");
        }
        
        // Увеличение силы притягивания (работает на всех)
        Bukkit.getScheduler().runTaskLater(miniGameService.getPlugin(), () -> {
            if (victim.isOnline() && fisher.isOnline()) {
                Vector direction = fisher.getLocation().toVector().subtract(victim.getLocation().toVector()).normalize();
                double strength = isTeammate ? 1.8D : 2.8D; // Врагов тянет сильнее
                victim.setVelocity(direction.multiply(strength).setY(0.6));
            }
        }, 1L);
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        if (!(event.getPotion().getShooter() instanceof Player shooter)) {
            return;
        }

        boolean hasHarmful = false;
        boolean hasBeneficial = false;
        for (PotionEffect effect : event.getPotion().getEffects()) {
            if (effect.getType().getCategory() == org.bukkit.potion.PotionEffectTypeCategory.HARMFUL) {
                hasHarmful = true;
            } else if (effect.getType().getCategory() == org.bukkit.potion.PotionEffectTypeCategory.BENEFICIAL) {
                hasBeneficial = true;
            }
        }

        for (org.bukkit.entity.LivingEntity entity : event.getAffectedEntities()) {
            if (!(entity instanceof Player victim)) {
                continue;
            }

            boolean isTeammate = miniGameService.areTeammates(shooter, victim);
            if (isTeammate) {
                // If shooter and victim are teammates, don't apply harmful effects
                if (hasHarmful && !hasBeneficial) {
                    event.setIntensity(entity, 0.0D);
                }
            } else {
                // If shooter and victim are enemies, don't apply beneficial effects
                if (hasBeneficial && !hasHarmful) {
                    event.setIntensity(entity, 0.0D);
                }
            }
        }
    }

    @EventHandler
    public void onCloudApply(AreaEffectCloudApplyEvent event) {
        if (!(event.getEntity().getSource() instanceof Player shooter)) {
            return;
        }

        boolean isHarmfulCheck = event.getEntity().getBasePotionType() != null && 
                           (event.getEntity().getBasePotionType().toString().contains("POISON") || 
                            event.getEntity().getBasePotionType().toString().contains("HARM") || 
                            event.getEntity().getBasePotionType().toString().contains("SLOWNESS") || 
                            event.getEntity().getBasePotionType().toString().contains("WEAKNESS"));
        
        // Alternatively check custom effects
        if (!isHarmfulCheck) {
            for (PotionEffect effect : event.getEntity().getCustomEffects()) {
                if (effect.getType().getCategory() == org.bukkit.potion.PotionEffectTypeCategory.HARMFUL) {
                    isHarmfulCheck = true;
                    break;
                }
            }
        }

        final boolean finalIsHarmful = isHarmfulCheck;
        final boolean finalIsBeneficial = !isHarmfulCheck; // Simplified for cloud

        event.getAffectedEntities().removeIf(entity -> {
            if (!(entity instanceof Player victim)) {
                return false;
            }
            boolean isTeammate = miniGameService.areTeammates(shooter, victim);
            if (isTeammate) {
                return finalIsHarmful;
            } else {
                return finalIsBeneficial;
            }
        });
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
