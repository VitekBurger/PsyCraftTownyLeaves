package mao.psyCraftTowny;

import mao.psyCraftTowny.command.PctaCommand;
import mao.psyCraftTowny.listener.MiniGameListener;
import mao.psyCraftTowny.service.KitSelectionService;
import mao.psyCraftTowny.service.PlayerItemsService;
import mao.psyCraftTowny.service.MapSelectionService;
import mao.psyCraftTowny.service.MiniGameService;
import mao.psyCraftTowny.service.TeamSelectionService;
import org.bukkit.plugin.java.JavaPlugin;

public final class PsyCraftTowny extends JavaPlugin {
    private MiniGameService miniGameService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final var teamSelectionService = new TeamSelectionService();
        final var mapSelectionService = new MapSelectionService();
        final var playerItemsService = new PlayerItemsService(this);
        final var kitSelectionService = new KitSelectionService();
        this.miniGameService = new MiniGameService(this, playerItemsService, teamSelectionService, mapSelectionService, kitSelectionService);

        PctaCommand pctaCommand = new PctaCommand(miniGameService);
        if (getCommand("pcta") != null) {
            getCommand("pcta").setExecutor(pctaCommand);
            getCommand("pcta").setTabCompleter(pctaCommand);
        }

        getServer().getPluginManager().registerEvents(new MiniGameListener(miniGameService, teamSelectionService, mapSelectionService, kitSelectionService), this);
        miniGameService.startLobbyMonitorTask();
    }

    @Override
    public void onDisable() {
        if (miniGameService != null) {
            miniGameService.shutdown();
        }
    }

    public MiniGameService getMiniGameService() {
        return miniGameService;
    }
}
