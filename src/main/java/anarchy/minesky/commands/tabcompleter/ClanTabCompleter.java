package anarchy.minesky.commands.tabcompleter;

import anarchy.minesky.commands.ClanCommand;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.utils.Utils;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClanTabCompleter implements TabCompleter {

    private final ClanCommand clanCommand;

    private static final List<String> ROLES = List.of("Sub-líder", "Capitão", "Recrutador", "Leal", "Membro");

    public ClanTabCompleter(ClanCommand clanCommand) {
        this.clanCommand = clanCommand;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) return List.of();

        if (s instanceof Player player) {
            player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 1f);
        }

        Player player = s instanceof Player p ? p : null;
        String sub = canonical(args[0]);

        return switch (args.length) {
            case 1 -> match(args[0], visibleCommands(s));

            case 2 -> {
                if (!sub.equals("admin") && !canUsePublic(s, sub)) yield List.of();
                yield switch (sub) {
                    case "membros" -> match(args[1], Utils.getClansTags());
                    case "banco" -> match(args[1], List.of("depositar", "sacar", "saldo"));
                    case "reagrupar" -> match(args[1], List.of("base"));
                    case "base" -> match(args[1], List.of("setar", "teleportar"));
                    case "convidar" -> match(args[1], Utils.getOnlinePlayerNames());
                    case "aliado", "rival" -> match(args[1], List.of("adicionar", "remover", "lista"));
                    case "promover", "rebaixar", "expulsar", "confiar" ->
                            player != null ? match(args[1], Utils.getClanMembersNames(player)) : List.of();
                    case "admin" -> s.hasPermission("anarchyclans.admin") ? match(args[1], adminCommands()) : List.of();
                    default -> List.of();
                };
            }

            case 3 -> {
                if (!sub.equals("admin") && !canUsePublic(s, sub)) yield List.of();
                yield switch (sub) {
                    case "aliado", "rival" -> player != null ? match(args[2], Utils.getClansTags(player)) : List.of();
                    case "promover" -> match(args[2], ROLES);
                    case "banco" -> match(args[2], List.of("<quantidade>"));
                    case "admin" -> {
                        if (!s.hasPermission("anarchyclans.admin")) yield List.of();
                        yield switch (canonical(args[1])) {
                            case "banco-admin" -> match(args[2], List.of("setar", "tirar", "resetar", "dar", "saldo"));
                            case "promover-admin", "rebaixar-admin" -> match(args[2], Utils.getOnlinePlayerNames());
                            case "dados" -> match(args[2], List.of("editar", "deletar", "resetar"));
                            case "forçar-entrada" -> match(args[2], Utils.getClansTags());
                            default -> List.of();
                        };
                    }
                    default -> List.of();
                };
            }

            case 4 -> {
                if (sub.equals("admin") && s.hasPermission("anarchyclans.admin")) {
                    yield switch (canonical(args[1])) {
                        case "dados" -> match(args[3], List.of("jogadores", "clans"));
                        case "banco-admin" -> match(args[3], Utils.getClansTags());
                        case "forçar-entrada" -> match(args[3], Utils.getOnlinePlayerNames());
                        case "promover-admin" -> match(args[3], ROLES);
                        default -> List.of();
                    };
                }
                yield List.of();
            }

            case 5 -> {
                if (sub.equals("admin") && s.hasPermission("anarchyclans.admin")) {
                    if (canonical(args[1]).equals("dados")) {
                        if (args[3].equalsIgnoreCase("clans")) {
                            List<String> clanOptions = new ArrayList<>(Utils.getClansTags());
                            clanOptions.add("todos");
                            yield match(args[4], clanOptions);
                        } else if (args[3].equalsIgnoreCase("jogadores")) {
                            yield match(args[4], Utils.getOnlinePlayerNames());
                        }
                    } else if (canonical(args[1]).equals("banco-admin")) {
                        yield match(args[4], List.of("<quantidade>"));
                    }
                }
                yield List.of();
            }

            case 6 -> {
                if (sub.equals("admin") && s.hasPermission("anarchyclans.admin")
                        && canonical(args[1]).equals("dados") && args[2].equalsIgnoreCase("editar")) {
                    if (args[3].equalsIgnoreCase("jogadores")) {
                        yield match(args[5], List.of("kills", "role"));
                    } else if (args[3].equalsIgnoreCase("clans")) {
                        yield match(args[5], List.of("level", "xp", "balance", "mudar-lider"));
                    }
                }
                yield List.of();
            }

            case 7 -> {
                if (sub.equals("admin") && s.hasPermission("anarchyclans.admin")
                        && canonical(args[1]).equals("dados") && args[2].equalsIgnoreCase("editar")
                        && args[3].equalsIgnoreCase("clans") && args[5].equalsIgnoreCase("mudar-lider")) {
                    yield match(args[6], Utils.getOnlinePlayerNames());
                }
                yield List.of();
            }

            default -> List.of();
        };
    }

    private List<String> visibleCommands(CommandSender sender) {
        List<String> available = new ArrayList<>();
        available.add("ajuda");
        for (SubCommand sub : clanCommand.getSubCommands()) {
            if (sub.getAdminCommand()) continue;
            if (ClanCommand.canUse(sender, sub)) available.add(sub.getName());
        }
        if (sender.hasPermission("anarchyclans.admin")
                || sender.hasPermission("anarchyclans.reload")
                || sender.hasPermission("anarchyclans.spy")) {
            available.add("admin");
        }
        available.sort(String.CASE_INSENSITIVE_ORDER);
        return available;
    }

    private boolean canUsePublic(CommandSender sender, String name) {
        return clanCommand.getSubCommand(name)
                .filter(sub -> !sub.getAdminCommand() && ClanCommand.canUse(sender, sub))
                .isPresent();
    }

    private List<String> adminCommands() {
        List<String> names = new ArrayList<>();
        for (SubCommand sub : clanCommand.getSubCommands()) {
            if (sub.getAdminCommand()) names.add(sub.getName());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private String canonical(String input) {
        return clanCommand.getSubCommand(input)
                .map(SubCommand::getName)
                .orElse(input.toLowerCase());
    }

    private List<String> match(String input, Collection<String> possibilities) {
        return StringUtil.copyPartialMatches(input, possibilities, new ArrayList<>());
    }
}