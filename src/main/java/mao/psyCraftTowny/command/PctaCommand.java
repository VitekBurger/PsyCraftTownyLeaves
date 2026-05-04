package mao.psyCraftTowny.command;

import mao.psyCraftTowny.model.GameMap;
import mao.psyCraftTowny.service.MiniGameService;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PctaCommand implements CommandExecutor, TabCompleter {
    private final MiniGameService miniGameService;

    public PctaCommand(MiniGameService miniGameService) {
        this.miniGameService = miniGameService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pct.admin")) {
            sender.sendMessage("§cНедостаточно прав.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "lobby" -> onLobby(sender);
            case "commands" -> onCommands(sender, args);
            case "playersonecommand" -> onPlayersOnCommand(sender, args);
            case "autosize" -> onAutosize(sender, args);
            case "spawn" -> onSpawn(sender, args);
            case "time" -> onTime(sender, args);
            case "point" -> onPoint(sender, args);
            case "map" -> onMap(sender, args);
            case "reload" -> onReload(sender);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean onReload(CommandSender sender) {
        miniGameService.reloadFromConfig();
        sender.sendMessage("§aКонфиг перезагружен, новые значения применены.");
        return true;
    }

    private boolean onPoint(CommandSender sender, String[] args) {
        if (args.length <= 2) {
            sender.sendMessage("§eИспользование: /pcta point <add|list|remove> <map_code>");
            return true;
        }
        final var pointSub = args[1].toLowerCase(Locale.ROOT);
        final var mapCode = args[2].toLowerCase(Locale.ROOT);
        if (!miniGameService.isMapWithCodeExistent(mapCode)) {
            sender.sendMessage("§eКарта с таким кодом не существует!");
            return true;
        }
        switch (pointSub) {
            case "add" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cКоманда только для игрока.");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage("§eИспользование: /pcta point add <display_name>");
                    return true;
                }
                final var displayName = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                int pointId = miniGameService.addCapturePoint(mapCode, player.getLocation(), displayName);
                sender.sendMessage("§aТочка захвата добавлена: id §f#" + pointId + "§a (радиус 8).");
                return true;
            }
            case "list" -> {
                List<String> lines = miniGameService.describeCapturePoints(mapCode);
                if (lines.isEmpty()) {
                    sender.sendMessage("§eТочек захвата пока нет.");
                    return true;
                }
                sender.sendMessage("§aСписок точек захвата:");
                for (String line : lines) {
                    sender.sendMessage(line);
                }
                return true;
            }
            case "remove" -> {
                if (args.length != 3) {
                    sender.sendMessage("§eИспользование: /pcta point remove <id>");
                    return true;
                }
                int pointId;
                try {
                    pointId = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage("§cID точки должен быть числом.");
                    return true;
                }
                if (!miniGameService.removeCapturePoint(mapCode, pointId)) {
                    sender.sendMessage("§cТочка с id #" + pointId + " не найдена.");
                    return true;
                }
                sender.sendMessage("§aТочка #" + pointId + " удалена.");
                return true;
            }
        }
        sender.sendMessage("§cНеизвестная подкоманда point. Доступно: add, list, remove.");
        return true;
    }

    private boolean onMap(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            sender.sendMessage("§eИспользование: /pcta map <add|list|remove>");
            return true;
        }
        String mapSub = args[1].toLowerCase(Locale.ROOT);
        switch (mapSub) {
            case "add" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cКоманда только для игрока.");
                    return true;
                }
                if (args.length < 5) {
                    sender.sendMessage("§eИспользование: /pcta map add <menu_item_key> <mode> <code> <world_border_size> <display_name>");
                    return true;
                }
                final var menuItemKey = args[2];
                if (!getItemKeys().contains(menuItemKey)) {
                    sender.sendMessage("§cПредмет для меню с таким названием не найден!");
                    return true;
                }
                final var stringMode = args[3].toLowerCase(Locale.ROOT);
                final var mode = Arrays.stream(GameMap.Mode.values())
                        .filter(v -> v.name().toLowerCase().equals(stringMode))
                        .findFirst()
                        .orElse(null);
                if (mode == null) {
                    sender.sendMessage("§cРежим игры с таким кодом не найден!");
                    return true;
                }
                final var code = args[4].toLowerCase(Locale.ROOT);
                final var worldBorderSize = Double.parseDouble(args[5]);
                final var displayName = String.join(" ", Arrays.copyOfRange(args, 6, args.length));
                miniGameService.addMap(player.getLocation(), code, mode, menuItemKey, worldBorderSize, displayName);
                sender.sendMessage("§aТочка захвата добавлена: code §f#" + code + "§a.");
                return true;
            }
            case "list" -> {
                List<String> lines = miniGameService.describeMap();
                if (lines.isEmpty()) {
                    sender.sendMessage("§eКарт пока нет.");
                    return true;
                }
                sender.sendMessage("§aСписок карт:");
                for (String line : lines) {
                    sender.sendMessage(line);
                }
                return true;
            }
            case "remove" -> {
                if (args.length != 3) {
                    sender.sendMessage("§eИспользование: /pcta point remove <id>");
                    return true;
                }
                final var code = args[2];
                if (!miniGameService.removeMap(code)) {
                    sender.sendMessage("§cКарта с code #" + code + " не найдена.");
                    return true;

                }
                sender.sendMessage("§aКарта #" + code + " удалена.");
                return true;
            }
        }
        sender.sendMessage("§cНеизвестная подкоманда map. Доступно: add, list, remove.");
        return true;
    }

    private boolean onTime(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            sender.sendMessage("§eИспользование: /pcta time <минуты>");
            return true;
        }
        int minutes;
        try {
            minutes = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cВремя должно быть целым числом минут.");
            return true;
        }
        if (!miniGameService.setGameDurationMinutes(minutes)) {
            sender.sendMessage("§cВремя должно быть больше 0 минут.");
            return true;
        }
        sender.sendMessage("§aДлительность игры установлена: " + minutes + " мин.");
        return true;
    }

    private boolean onSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cКоманда только для игрока.");
            return true;
        }
        if (args.length <= 2) {
            sender.sendMessage("§eИспользование: /pcta spawn <код карты> <номер команды>");
            return true;
        }
        if (!miniGameService.isMapWithCodeExistent(args[1])) {
            sender.sendMessage("§eКарта не найдена!");
            return true;
        }
        int teamId;
        try {
            teamId = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cНомер команды должен быть целым.");
            return true;
        }
        if (!miniGameService.setTeamSpawn(args[1], teamId, player.getLocation())) {
            sender.sendMessage("§cНеверный номер команды.");
            return true;
        }
        String teamName = teamId == 1 ? "§cКрасные" : "§9Синие";
        sender.sendMessage("§aСпавн команды " + teamName + "§a установлен.");
        return true;
    }

    private boolean onAutosize(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            sender.sendMessage("§eИспользование: /pcta autosize <on|off>");
            return true;
        }
        String mode = args[1].toLowerCase(Locale.ROOT);
        if (!mode.equals("on") && !mode.equals("off")) {
            sender.sendMessage("§cДоступно только: on или off.");
            return true;
        }
        boolean enabled = mode.equals("on");
        miniGameService.setAutoPlayersPerTeam(enabled);
        sender.sendMessage("§aАвторазмер игроков в команде: " + (enabled ? "§aвключен" : "§cвыключен"));
        return true;
    }

    private boolean onPlayersOnCommand(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            sender.sendMessage("§eИспользование: /pcta playersonecommand <число>");
            return true;
        }
        int players;
        try {
            players = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cКоличество игроков должно быть целым.");
            return true;
        }
        if (!miniGameService.setPlayersPerTeam(players)) {
            sender.sendMessage("§cКоличество игроков должно быть больше 0.");
            return true;
        }
        sender.sendMessage("§aИгроков в команде установлено: " + players + " §7(авторазмер отключен)");
        return true;
    }

    private boolean onCommands(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            sender.sendMessage("§eИспользование: /pcta commands 2");
            return true;
        }
        int teams;
        try {
            teams = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cЧисло команд должно быть целым.");
            return true;
        }
        if (!miniGameService.setTeamsCount(teams)) {
            sender.sendMessage("§cСейчас поддерживаются только 2 команды (две стороны войны).");
            return true;
        }
        sender.sendMessage("§aКоличество команд установлено: " + teams);
        return true;
    }

    private boolean onLobby(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cКоманда только для игрока.");
            return true;
        }
        miniGameService.setLobby(player);
        sender.sendMessage("§aЛобби-спавн установлен.");
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§e/pcta lobby §7- поставить лобби спавн");
        sender.sendMessage("§e/pcta commands 2 §7- число команд (сейчас только 2)");
        sender.sendMessage("§e/pcta playersonecommand 5 §7- игроков в одной команде");
        sender.sendMessage("§e/pcta autosize <on|off> §7- автообновление размера команды");
        sender.sendMessage("§e/pcta spawn <map_code> <command_id> §7- поставить спавн команды");
        sender.sendMessage("§e/pcta time 20 §7- время игры в минутах");
        sender.sendMessage("§e/pcta map add <menu_item_key> <mode> <code> <world_border_size> <display_name> §7- Добавить карту с кодом и именем с центром в текущей позиции и указанным размером");
        sender.sendMessage("§e/pcta map remove <map_code> §7- удалить карту по code");
        sender.sendMessage("§e/pcta map list §7- список карт");
        sender.sendMessage("§e/pcta point add <map_code> <point_display_name> §7- добавить точку захвата в текущем блоке");
        sender.sendMessage("§e/pcta point list <map_code> §7- список точек захвата с id");
        sender.sendMessage("§e/pcta point remove <map_code> <id> §7- удалить точку по id");
        sender.sendMessage("§e/pcta reload §7- перезагрузить config.yml");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final var out = new ArrayList<String>();
        final var arg0 = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        final var arg1 = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        final var arg2 = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "";
        final var arg3 = args.length > 3 ? args[3].toLowerCase(Locale.ROOT) : "";
        if (args.length == 1) {
            return getCompletions(
                    arg0,
                    List.of("lobby", "commands", "playersonecommand", "autosize", "spawn", "time", "point", "reload", "map")
            );
        }
        if (args.length == 2 && Set.of("point", "map").contains(arg0)) {
            return getCompletions(
                    arg1,
                    List.of("add", "list", "remove")
            );
        }
        if (args.length == 3 && arg0.equals("point") && Set.of("remove", "add", "list").contains(arg1)) {
            return getCompletions(
                    arg2,
                    miniGameService.getMapCodes()
            );
        }
        if (args.length == 4 && arg0.equals("point") && arg1.equals("remove")) {
            return getCompletions(
                    arg3,
                    miniGameService.getCapturePointIds().stream()
                            .map(String::valueOf)
                            .toList()
            );
        }
        if (args.length == 3 && arg0.equals("map") && arg1.equals("remove")) {
            return getCompletions(
                    arg2,
                    miniGameService.getMapCodes()
            );
        }
        if (args.length == 3 && arg0.equals("map") && arg1.equals("add")) {
            return getCompletions(
                    arg2,
                    getItemKeys()
            );
        }
        if (args.length == 4 && arg0.equals("map") && arg1.equals("add")) {
            return getCompletions(
                    arg3,
                    Arrays.stream(GameMap.Mode.values())
                            .map(Enum::name)
                            .toList()
            );
        }
        if (args.length == 2 && "autosize".equals(arg0)) {
            return getCompletions(
                    arg1,
                    List.of("on", "off")
            );
        }
        if (args.length == 2 && "spawn".equals(arg0)) {
            return getCompletions(
                    arg1,
                    miniGameService.getMapCodes()
            );
        }
        if (args.length == 3 && "spawn".equals(arg0)) {
            return getCompletions(
                    arg2,
                    List.of("1", "2")
            );
        }
        if (args.length == 2 && "time".equals(arg0)) {
            return getCompletions(
                    arg2,
                    List.of("10", "15", "20")
            );
        }
        return out;
    }

    private List<String> getCompletions(String typed, List<String> possibleCompletions) {
        return possibleCompletions.stream()
                .filter(completion -> completion.startsWith(typed))
                .toList();
    }

    private List<String> getItemKeys() {
        List<String> keys = new ArrayList<>();

        for (Material material : Registry.MATERIAL) {
            if (!material.isItem()) continue;

            NamespacedKey key = material.getKey();
            keys.add(key.toString());
        }
        return keys;
    }
}
