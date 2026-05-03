package mao.psyCraftTowny.service;

import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

final class RoundStatsService {
    private final Map<UUID, Integer> killStats = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> captureStats = new ConcurrentHashMap<>();

    void clear() {
        killStats.clear();
        captureStats.clear();
    }

    void ensurePlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        killStats.putIfAbsent(playerId, 0);
        captureStats.putIfAbsent(playerId, 0);
    }

    void recordKill(UUID playerId) {
        if (playerId == null) {
            return;
        }
        killStats.merge(playerId, 1, Integer::sum);
    }

    void recordCapture(UUID playerId) {
        if (playerId == null) {
            return;
        }
        captureStats.merge(playerId, 1, Integer::sum);
    }

    void announceRoundLeaders(Function<UUID, String> nameResolver) {
        Bukkit.broadcastMessage("§6----- Итоги раунда -----");
        broadcastTopThree("§eТоп-3 по киллам", killStats, "§7киллов", nameResolver);
        broadcastTopThree("§bТоп-3 по захвату точек", captureStats, "§7очков захвата", nameResolver);
    }

    private void broadcastTopThree(String title, Map<UUID, Integer> stats, String suffix, Function<UUID, String> nameResolver) {
        Bukkit.broadcastMessage(title);
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(stats.entrySet());
        sorted.sort(Comparator.<Map.Entry<UUID, Integer>>comparingInt(Map.Entry::getValue).reversed());
        int limit = Math.min(3, sorted.size());
        if (limit == 0 || sorted.get(0).getValue() <= 0) {
            Bukkit.broadcastMessage("§8• Нет данных");
            return;
        }
        for (int i = 0; i < limit; i++) {
            Map.Entry<UUID, Integer> entry = sorted.get(i);
            String name = nameResolver.apply(entry.getKey());
            Bukkit.broadcastMessage("§f" + (i + 1) + ". " + name + " §8- §f" + entry.getValue() + " " + suffix);
        }
    }
}
