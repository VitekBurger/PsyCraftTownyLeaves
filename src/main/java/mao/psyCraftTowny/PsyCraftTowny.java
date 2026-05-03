package mao.psyCraftTowny;

import mao.psyCraftTowny.command.PctaCommand;
import mao.psyCraftTowny.listener.MiniGameListener;
import mao.psyCraftTowny.service.MiniGameService;
import org.bukkit.plugin.java.JavaPlugin;

public final class PsyCraftTowny extends JavaPlugin {
    private MiniGameService miniGameService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.miniGameService = new MiniGameService(this);

        PctaCommand pctaCommand = new PctaCommand(miniGameService);
        if (getCommand("pcta") != null) {
            getCommand("pcta").setExecutor(pctaCommand);
            getCommand("pcta").setTabCompleter(pctaCommand);
        }

        getServer().getPluginManager().registerEvents(new MiniGameListener(miniGameService), this);
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
