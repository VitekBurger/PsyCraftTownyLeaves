package mao.psyCraftTowny.utils;

import org.bukkit.entity.Player;

public final class PlayerUtils {

    private PlayerUtils() {
        throw new UnsupportedOperationException();
    }

    public static boolean isObserver(Player player) {
        return player != null && player.getGameMode() == org.bukkit.GameMode.SPECTATOR;
    }
}
