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

public class RivalSubCommand extends SubCommand {

    private final AnarchyClans plugin = AnarchyClans.getInstance();

    @Override
    public String getName() {
        return "rival";
    }

    @Override
    public String getDescription() {
        return "Gerencia rivalidades do clã";
    }

    @Override
    public String getUsage() {
        return "/clan rival <adicionar,remover,lista> [clã]";
    }

    @Override
    public List<String> getAliases() {
        return List.of("rivals", "inimigo");
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
            sendError(player, "&cVocê não pertence a um clã.");
            return;
        }

        String action = args[1].toLowerCase();

        boolean isListAction = action.equals("listar") || action.equals("list") || action.equals("lista");
        if (!ClanRoles.isLeaders(clan.getRole(player.getUniqueId())) && !isListAction) {
            sendError(player, "&cApenas líderes podem gerenciar rivalidades.");
            return;
        }

        switch (action) {
            case "adicionar", "add" -> {
                if (args.length < 3) {
                    sendError(player, "&cUso: /clan rival adicionar <tag>");
                    return;
                }

                String targetTag = args[2];
                Clan targetClan = ClanHandler.getClanByTag(targetTag);

                if (targetClan == null) {
                    sendError(player, "&cClã '" + targetTag + "' não encontrado.");
                    return;
                }

                if (targetClan.getId().equals(clan.getId())) {
                    sendError(player, "&cVocê não pode declarar rivalidade com seu próprio clã.");
                    return;
                }

                if (clan.isAlly(targetClan)) {
                    sendError(player, "&cO clã " + targetClan.getName() + " é seu aliado. Rompa a aliança antes.");
                    return;
                }

                if (clan.isRival(targetClan)) {
                    sendError(player, "&cSeu clã já é rival de " + targetClan.getName() + ".");
                    return;
                }

                ClanHandler.addRival(clan, targetClan, player);
                AnarchyClans.l.info("[CLÃS] " + player.getName() + " declarou rivalidade com " + targetClan.getName());

                FeedbackAPI.display(player, "&cRivalidade declarada com &f" + targetClan.getName() + "&c!");
            }

            case "remover", "remove" -> {
                if (args.length < 3) {
                    sendError(player, "&cUso: /clan rival remover <tag>");
                    return;
                }

                String targetTag = args[2];
                Clan targetClan = ClanHandler.getClanByTag(targetTag);

                if (targetClan == null) {
                    sendError(player, "&cClã '" + targetTag + "' não encontrado.");
                    return;
                }

                if (targetClan.getId().equals(clan.getId())) {
                    sendError(player, "&cVocê não pode alterar rivalidade com seu próprio clã.");
                    return;
                }

                if (!clan.isRival(targetClan)) {
                    sendError(player, "&cSeu clã não possui rivalidade com " + targetClan.getName() + ".");
                    return;
                }

                ClanRequestHandler rivalHandler = plugin.getRequestManager().getHandler(ClanRequestType.RIVAL);

                if (rivalHandler.hasRequest(targetClan) && clan.equals(rivalHandler.getRequestClan(targetClan))) {
                    sendError(player, "&cO pedido de paz já foi enviado para este clã.");
                    return;
                }

                rivalHandler.sendRequest(clan, targetClan, player);
                ClanHandler.broadcastClanMessage(clan, "&f" + player.getName() + " &7enviou um pedido de paz para &f" + targetClan.getName() + "&7.");
                AnarchyClans.l.info("[CLÃS] " + player.getName() + " enviou pedido de paz para " + targetClan.getName());

                FeedbackAPI.display(player, "&aPedido de paz enviado para &f" + targetClan.getName() + "&a!");
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

                List<Clan> rivals = targetClan.getRivals().stream()
                        .map(ClanHandler::getClanByID)
                        .filter(Objects::nonNull)
                        .toList();

                player.sendMessage(Utils.c("&7Rivais de &f" + targetClan.getName() + " &8[" + targetClan.getTag() + "&8]&7:"));

                if (rivals.isEmpty()) {
                    player.sendMessage(Utils.c("&7(Nenhuma rivalidade ativa)"));
                } else {
                    for (Clan rival : rivals) {
                        player.sendMessage(Utils.c(" &7• &f" + rival.getName() + " &8[" + rival.getTag() + "&8]"));
                    }
                }

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
            }

            default -> sendError(player, "&cSubcomando inválido. Uso: " + getUsage());
        }
    }
}