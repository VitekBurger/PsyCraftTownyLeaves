package mao.psyCraftTowny.command;

import mao.psyCraftTowny.PsyCraftTowny;
import mao.psyCraftTowny.service.MiniGameService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PctaCommand implements CommandExecutor, TabCompleter {
    private final MiniGameService miniGameService;

    public PctaCommand(PsyCraftTowny plugin, MiniGameService miniGameService) {
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

        int offset = 0;
        if (args[0].equalsIgnoreCase("set")) {
            offset = 1;
        }
        if (offset >= args.length) {
            sendUsage(sender);
            return true;
        }

        String sub = args[offset].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "lobby" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cКоманда только для игрока.");
                    return true;
                }
                miniGameService.setLobby(player);
                sender.sendMessage("§aЛобби-спавн установлен.");
                return true;
            }
            case "commands" -> {
                if (offset + 1 >= args.length) {
                    sender.sendMessage("§eИспользование: /pcta commands 2");
                    return true;
                }
                int teams;
                try {
                    teams = Integer.parseInt(args[offset + 1]);
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
            case "playersonecommand" -> {
                if (offset + 1 >= args.length) {
                    sender.sendMessage("§eИспользование: /pcta playersonecommand <число>");
                    return true;
                }
                int players;
                try {
                    players = Integer.parseInt(args[offset + 1]);
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
            case "autosize" -> {
                if (offset + 1 >= args.length) {
                    sender.sendMessage("§eИспользование: /pcta autosize <on|off>");
                    return true;
                }
                String mode = args[offset + 1].toLowerCase(Locale.ROOT);
                if (!mode.equals("on") && !mode.equals("off")) {
                    sender.sendMessage("§cДоступно только: on или off.");
                    return true;
                }
                boolean enabled = mode.equals("on");
                miniGameService.setAutoPlayersPerTeam(enabled);
                sender.sendMessage("§aАвторазмер игроков в команде: " + (enabled ? "§aвключен" : "§cвыключен"));
                return true;
            }
            case "spawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cКоманда только для игрока.");
                    return true;
                }
                if (offset + 1 >= args.length) {
                    sender.sendMessage("§eИспользование: /pcta spawn <номер команды>");
                    return true;
                }
                int teamId;
                try {
                    teamId = Integer.parseInt(args[offset + 1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage("§cНомер команды должен быть целым.");
                    return true;
                }
                if (!miniGameService.setTeamSpawn(teamId, player.getLocation())) {
                    sender.sendMessage("§cНеверный номер команды.");
                    return true;
                }
                String teamName = teamId == 1 ? "§cКрасные" : "§9Синие";
                sender.sendMessage("§aСпавн команды " + teamName + "§a установлен.");
                return true;
            }
            case "kit" -> {
                sender.sendMessage("§eКиты теперь выбираются игроками в лобби через меню выбора.");
                sender.sendMessage("§7Доступные киты: Мечник, Лучник, Инженер.");
                return true;
            }
            case "time" -> {
                if (offset + 1 >= args.length) {
                    sender.sendMessage("§eИспользование: /pcta time <минуты>");
                    return true;
                }
                int minutes;
                try {
                    minutes = Integer.parseInt(args[offset + 1]);
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
            case "point" -> {
                if (offset + 1 >= args.length) {
                    sender.sendMessage("§eИспользование: /pcta point <add|list|remove>");
                    return true;
                }
                String pointSub = args[offset + 1].toLowerCase(Locale.ROOT);
                if (pointSub.equals("add")) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("§cКоманда только для игрока.");
                        return true;
                    }
                    int pointId = miniGameService.addCapturePoint(player.getLocation());
                    sender.sendMessage("§aТочка захвата добавлена: id §f#" + pointId + "§a (радиус 8).");
                    return true;
                }
                if (pointSub.equals("list")) {
                    List<String> lines = miniGameService.describeCapturePoints();
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
                if (pointSub.equals("remove")) {
                    if (offset + 2 >= args.length) {
                        sender.sendMessage("§eИспользование: /pcta point remove <id>");
                        return true;
                    }
                    int pointId;
                    try {
                        pointId = Integer.parseInt(args[offset + 2]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage("§cID точки должен быть числом.");
                        return true;
                    }
                    if (!miniGameService.removeCapturePoint(pointId)) {
                        sender.sendMessage("§cТочка с id #" + pointId + " не найдена.");
                        return true;
                    }
                    sender.sendMessage("§aТочка #" + pointId + " удалена.");
                    return true;
                }
                sender.sendMessage("§cНеизвестная подкоманда point. Доступно: add, list, remove.");
                return true;
            }
            case "reload" -> {
                miniGameService.reloadFromConfig();
                sender.sendMessage("§aКонфиг перезагружен, новые значения применены.");
                return true;
            }
            default -> {
                sendUsage(sender);
                return true;
            }
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§e/pcta lobby §7- поставить лобби спавн");
        sender.sendMessage("§e/pcta commands 2 §7- число команд (сейчас только 2)");
        sender.sendMessage("§e/pcta playersonecommand 5 §7- игроков в одной команде");
        sender.sendMessage("§e/pcta autosize <on|off> §7- автообновление размера команды");
        sender.sendMessage("§e/pcta spawn 1 §7- поставить спавн команды");
        sender.sendMessage("§e/pcta time 20 §7- время игры в минутах");
        sender.sendMessage("§e/pcta point add §7- добавить точку захвата в текущем блоке");
        sender.sendMessage("§e/pcta point list §7- список точек захвата с id");
        sender.sendMessage("§e/pcta point remove <id> §7- удалить точку по id");
        sender.sendMessage("§e/pcta kit §7- информация о доступных китах");
        sender.sendMessage("§e/pcta reload §7- перезагрузить config.yml");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("lobby", "commands", "playersonecommand", "autosize", "spawn", "time", "point", "kit", "reload")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("point")) {
            for (String s : List.of("add", "list", "remove")) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
            return out;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("point") && args[1].equalsIgnoreCase("remove")) {
            String typed = args[2].toLowerCase(Locale.ROOT);
            for (Integer id : miniGameService.getCapturePointIds()) {
                String value = String.valueOf(id);
                if (value.startsWith(typed)) {
                    out.add(value);
                }
            }
            return out;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("commands") || args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("time") || args[0].equalsIgnoreCase("autosize"))) {
            if ("2".startsWith(args[1])) {
                out.add("2");
            }
            if (args[0].equalsIgnoreCase("spawn") && "1".startsWith(args[1])) {
                out.add("1");
            }
            if (args[0].equalsIgnoreCase("spawn") && "2".startsWith(args[1])) {
                out.add("2");
            }
            if (args[0].equalsIgnoreCase("time")) {
                if ("20".startsWith(args[1])) out.add("20");
                if ("15".startsWith(args[1])) out.add("15");
                if ("10".startsWith(args[1])) out.add("10");
            }
            if (args[0].equalsIgnoreCase("autosize")) {
                if ("on".startsWith(args[1].toLowerCase(Locale.ROOT))) out.add("on");
                if ("off".startsWith(args[1].toLowerCase(Locale.ROOT))) out.add("off");
            }
            return out;
        }
        return out;
    }
}
