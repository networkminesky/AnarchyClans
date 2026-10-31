package anarchy.minesky.commands.tabcompleter;

import anarchy.minesky.data.Clan;
import anarchy.minesky.handlers.ClanHandler;
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

    private static final List<String> SUB_COMMANDS = List.of(
            "criar", "editar", "acabar", "sair", "expulsar", "promover", "rebaixar",
            "fogo-amigo", "base", "banco", "convidar", "aceitar", "rejeitar", "estandarte",
            "aliado", "rival", "anunciar", "mural", "ajuda", "lista", "membros",
            "coordenadas", "reagrupar", "guerra", "traidor", "traidores", "lista-negra",
            "confiar"
    );

    private static final List<String> ADMIN_SUB_COMMANDS = List.of(
            "banco-admin", "promover-admin", "rebaixar-admin", "forçar-entrada",
            "resetar-kdr", "dados", "fogo-amigo-global", "spy", "reload", "reconnect", "mural-admin"
    );

    private static final List<String> ROLES = List.of("Sub-líder", "Capitão", "Recrutador", "Leal", "Membro");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (s instanceof Player player) {
            player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 1f);
        }

        Player player = s instanceof Player p ? p : null;
        String sub = args[0].toLowerCase();

        return switch (args.length) {
            case 1 -> {
                boolean hasAdmin = s.hasPermission("anarchyclans.admin") ||
                        s.hasPermission("anarchyclans.reload") ||
                        s.hasPermission("anarchyclans.spy");

                List<String> available = new ArrayList<>(SUB_COMMANDS);
                if (hasAdmin) available.add("admin");

                yield match(args[0], available);
            }

            case 2 -> switch (sub) {
                case "membros" -> match(args[1], Utils.getClansTags());
                case "banco" -> match(args[1], List.of("depositar", "sacar", "saldo"));
                case "reagrupar" -> match(args[1], List.of("base"));
                case "base" -> match(args[1], List.of("setar", "teleportar"));
                case "convidar" -> match(args[1], Utils.getOnlinePlayerNames());
                case "aliado", "rival" -> match(args[1], List.of("adicionar", "remover", "lista"));
                case "promover", "rebaixar", "expulsar", "traidor" ->
                        player != null ? match(args[1], Utils.getClanMembersNames(player)) : List.of();
                case "guerra", "war" -> {
                    List<String> warOptions = new ArrayList<>(List.of("aceitar", "recusar"));
                    if (player != null) {
                        Clan clan = ClanHandler.getClanByPlayer(player);
                        if (clan != null) warOptions.addAll(ClanHandler.getRivalsTags(clan));
                    }
                    yield match(args[1], warOptions);
                }
                case "admin" -> s.hasPermission("anarchyclans.admin") ? match(args[1], ADMIN_SUB_COMMANDS) : List.of();
                default -> List.of();
            };

            case 3 -> switch (sub) {
                case "aliado", "rival" -> player != null ? match(args[2], Utils.getClansTags(player)) : List.of();
                case "promover" -> match(args[2], ROLES);
                case "banco" -> match(args[2], List.of("<quantidade>"));
                case "guerra", "war" -> {
                    String action = args[1].toLowerCase();
                    yield (action.equals("aceitar") || action.equals("recusar"))
                            ? match(args[2], Utils.getClansTags())
                            : List.of();
                }
                case "admin" -> {
                    if (!s.hasPermission("anarchyclans.admin")) yield List.of();
                    yield switch (args[1].toLowerCase()) {
                        case "banco-admin" -> match(args[2], List.of("setar", "tirar", "resetar", "dar", "saldo"));
                        case "fogo-amigo-global" -> match(args[2], List.of("ativar", "desativar"));
                        case "promover-admin", "rebaixar-admin" -> match(args[2], Utils.getOnlinePlayerNames());
                        case "dados" -> match(args[2], List.of("editar", "deletar", "resetar"));
                        case "forçar-entrada", "mural-admin" -> match(args[2], Utils.getClansTags());
                        default -> List.of();
                    };
                }
                default -> List.of();
            };

            case 4 -> {
                if (sub.equals("admin") && s.hasPermission("anarchyclans.admin")) {
                    yield switch (args[1].toLowerCase()) {
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
                    if (args[1].equalsIgnoreCase("dados")) {
                        if (args[3].equalsIgnoreCase("clans")) {
                            List<String> clanOptions = new ArrayList<>(Utils.getClansTags());
                            clanOptions.add("todos");
                            yield match(args[4], clanOptions);
                        } else if (args[3].equalsIgnoreCase("jogadores")) {
                            yield match(args[4], Utils.getOnlinePlayerNames());
                        }
                    } else if (args[1].equalsIgnoreCase("banco-admin")) {
                        yield match(args[4], List.of("<quantidade>"));
                    }
                }
                yield List.of();
            }

            case 6 -> {
                if (sub.equals("admin") && s.hasPermission("anarchyclans.admin")
                        && args[1].equalsIgnoreCase("dados") && args[2].equalsIgnoreCase("editar")) {
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
                        && args[1].equalsIgnoreCase("dados") && args[2].equalsIgnoreCase("editar")
                        && args[3].equalsIgnoreCase("clans") && args[5].equalsIgnoreCase("mudar-lider")) {
                    yield match(args[6], Utils.getOnlinePlayerNames());
                }
                yield List.of();
            }

            default -> List.of();
        };
    }

    private List<String> match(String input, Collection<String> possibilities) {
        return StringUtil.copyPartialMatches(input, possibilities, new ArrayList<>());
    }
}