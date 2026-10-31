package anarchy.minesky.commands.subcommands.clans;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.handlers.requests.ClanRequestHandler;
import anarchy.minesky.handlers.requests.ClanRequestType;
import anarchy.minesky.utils.Utils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class AllySubCommand extends SubCommand {

    private final AnarchyClans plugin = AnarchyClans.getInstance();

    @Override
    public String getName() {
        return "aliado";
    }

    @Override
    public String getDescription() {
        return "Gerenciar alianças do clã";
    }

    @Override
    public String getUsage() {
        return "/clan aliado <adicionar,remover,lista> [clã]";
    }

    @Override
    public List<String> getAliases() {
        return List.of("ally");
    }

    @Override
    public boolean getAdminCommand() {
        return false;
    }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 2) {
            sendError(player, "&cUso: " + getUsage());
            return;
        }

        Clan clan = ClanHandler.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            sendError(player, "&cVocê não pertence a nenhum clã.");
            return;
        }

        String action = args[1].toLowerCase();

        boolean isListAction = action.equals("listar") || action.equals("list") || action.equals("lista");
        if (!ClanRoles.isLeaders(clan.getRole(player.getUniqueId())) && !isListAction) {
            sendError(player, "&cApenas líderes podem gerenciar alianças.");
            return;
        }

        ClanRequestHandler allyHandler = plugin.getRequestManager().getHandler(ClanRequestType.ALLY);

        switch (action) {
            case "adicionar", "add" -> {
                if (args.length < 3) {
                    sendError(player, "&cUso: /clan aliado adicionar <tag>");
                    return;
                }

                String targetTag = args[2];
                Clan targetClan = ClanHandler.getClanByTag(targetTag);

                if (targetClan == null) {
                    sendError(player, "&cClã '" + targetTag + "' não encontrado.");
                    return;
                }

                if (targetClan.getId().equals(clan.getId())) {
                    sendError(player, "&cVocê não pode formar aliança com seu próprio clã.");
                    return;
                }

                if (clan.isRival(targetClan)) {
                    sendError(player, "&cVocê não pode se aliar a um clã rival. Remova a rivalidade antes.");
                    return;
                }

                if (clan.isAlly(targetClan)) {
                    sendError(player, "&cSeu clã já possui aliança com " + targetClan.getName() + ".");
                    return;
                }

                if (allyHandler.hasRequest(targetClan) && clan.equals(allyHandler.getRequestClan(targetClan))) {
                    sendError(player, "&cO pedido de aliança já foi enviado para este clã.");
                    return;
                }

                allyHandler.sendRequest(clan, targetClan, player);
                ClanHandler.broadcastClanMessage(clan, "&f" + player.getName() + " &7enviou um pedido de aliança para &f" + targetClan.getName() + "&7.");
                AnarchyClans.l.info("[CLÃS] " + player.getName() + " enviou pedido de aliança para " + targetClan.getName());

                FeedbackAPI.display(player, "&aPedido de aliança enviado para &f" + targetClan.getName() + "&a!");
            }

            case "remover", "remove" -> {
                if (args.length < 3) {
                    sendError(player, "&cUso: /clan aliado remover <tag>");
                    return;
                }

                String targetTag = args[2];
                Clan targetClan = ClanHandler.getClanByTag(targetTag);

                if (targetClan == null) {
                    sendError(player, "&cClã '" + targetTag + "' não encontrado.");
                    return;
                }

                if (targetClan.getId().equals(clan.getId())) {
                    sendError(player, "&cVocê não pode remover aliança com seu próprio clã.");
                    return;
                }

                if (!clan.isAlly(targetClan)) {
                    sendError(player, "&cSeu clã não possui aliança com " + targetClan.getName() + ".");
                    return;
                }

                ClanHandler.removeAlly(clan, targetClan, player);
                AnarchyClans.l.info("[CLÃS] " + player.getName() + " rompeu a aliança com " + targetClan.getName());

                FeedbackAPI.display(player, "&cAliança com &f" + targetClan.getName() + " &crompida.");
            }

            case "listar", "list", "lista" -> {
                Clan targetClan = clan;
                if (args.length >= 3) {
                    targetClan = ClanHandler.getClanByTag(args[2]);
                    if (targetClan == null) {
                        sendError(player, "&cClã '" + args[2] + "' não encontrado.");
                        return;
                    }
                }

                List<Clan> allies = targetClan.getAllies().stream()
                        .map(ClanHandler::getClanByID)
                        .filter(Objects::nonNull)
                        .toList();

                player.sendMessage(Utils.c("&7Alianças de &f" + targetClan.getName() + " &8[" + targetClan.getTag() + "&8]&7:"));

                if (allies.isEmpty()) {
                    player.sendMessage(Utils.c("&7(Nenhuma aliança ativa)"));
                } else {
                    for (Clan ally : allies) {
                        player.sendMessage(Utils.c(" &7• &f" + ally.getName() + " &8[" + ally.getTag() + "&8]"));
                    }
                }

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
            }

            default -> sendError(player, "&cSubcomando inválido. Uso: " + getUsage());
        }
    }
}