package anarchy.minesky.commands.subcommands.clans;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.handlers.requests.ClanRequestHandler;
import anarchy.minesky.handlers.requests.ClanRequestType;
import anarchy.minesky.handlers.requests.ReagroupHandler;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class RejectSubCommand extends SubCommand {

    private final AnarchyClans plugin = AnarchyClans.getInstance();

    @Override
    public String getName() {
        return "rejeitar";
    }

    @Override
    public String getDescription() {
        return "Rejeita convites, pedidos de aliança, paz ou reagrupamentos";
    }

    @Override
    public String getUsage() {
        return "/clan rejeitar";
    }

    @Override
    public List<String> getAliases() {
        return List.of("recusar", "reject");
    }

    @Override
    public boolean getAdminCommand() {
        return false;
    }

    @Override
    public void perform(Player player, String[] args) {
        UUID playerId = player.getUniqueId();
        Clan ownClan = ClanHandler.getClanByPlayer(playerId);

        ClanRequestHandler allyHandler = plugin.getRequestManager().getHandler(ClanRequestType.ALLY);
        ClanRequestHandler rivalHandler = plugin.getRequestManager().getHandler(ClanRequestType.RIVAL);
        ReagroupHandler reagroupHandler = plugin.getReagroupHandler();

        boolean hasInvite = plugin.getInviteHandler().hasInvite(playerId);
        boolean hasAllyRequest = ownClan != null && allyHandler.hasRequest(ownClan);
        boolean hasRivalRequest = ownClan != null && rivalHandler.hasRequest(ownClan);
        boolean hasReagroup = reagroupHandler.hasRequest(playerId);

        if (!hasInvite && !hasAllyRequest && !hasRivalRequest && !hasReagroup) {
            sendError(player, "&cVocê não tem pedidos pendentes para rejeitar.");
            return;
        }

        if (hasReagroup) {
            reagroupHandler.reject(player);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return;
        }

        if (hasAllyRequest) {
            if (!ClanRoles.isLeaders(ownClan.getRole(playerId))) {
                sendError(player, "&cApenas líderes podem rejeitar pedidos de aliança.");
                return;
            }

            Clan allyClan = allyHandler.getRequestClan(ownClan);
            allyHandler.removeRequest(ownClan);

            if (allyClan != null) {
                ClanHandler.broadcastClanMessage(allyClan, "&cO pedido de aliança enviado para &f" + ownClan.getName() + " &cfoi rejeitado.");
                ClanHandler.broadcastClanMessage(ownClan, "&f" + player.getName() + " &crejeitou o pedido de aliança do clã &f" + allyClan.getName() + "&c.");
                FeedbackAPI.display(player, "&cVocê rejeitou a aliança com &f" + allyClan.getName() + "&c.");
                AnarchyClans.l.info("[CLÃS] " + player.getName() + " recusou o pedido de aliança de " + allyClan.getName());
            } else {
                FeedbackAPI.display(player, "&cPedido de aliança descartado.");
            }

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return;
        }

        if (hasRivalRequest) {
            if (!ClanRoles.isLeaders(ownClan.getRole(playerId))) {
                sendError(player, "&cApenas líderes podem rejeitar pedidos de paz.");
                return;
            }

            Clan rivalClan = rivalHandler.getRequestClan(ownClan);
            rivalHandler.removeRequest(ownClan);

            if (rivalClan != null) {
                ClanHandler.broadcastClanMessage(rivalClan, "&cO pedido de paz enviado para &f" + ownClan.getName() + " &cfoi rejeitado.");
                ClanHandler.broadcastClanMessage(ownClan, "&f" + player.getName() + " &crejeitou o pedido de paz do clã &f" + rivalClan.getName() + "&c.");
                FeedbackAPI.display(player, "&cVocê rejeitou o pedido de paz com &f" + rivalClan.getName() + "&c.");
                AnarchyClans.l.info("[CLÃS] " + player.getName() + " recusou o pedido de paz de " + rivalClan.getName());
            } else {
                FeedbackAPI.display(player, "&cPedido de paz descartado.");
            }

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return;
        }

        if (hasInvite) {
            Clan clan = plugin.getInviteHandler().getInviteClan(playerId);
            plugin.getInviteHandler().removeInvite(playerId);

            if (clan != null) {
                ClanHandler.broadcastClanMessage(clan, "&f" + player.getName() + " &crejeitou o convite do clã.");
                FeedbackAPI.display(player, "&cVocê rejeitou o convite do clã &f" + clan.getName() + "&c.");
                AnarchyClans.l.info("[CLÃS] " + player.getName() + " recusou o convite do clã " + clan.getName());
            } else {
                FeedbackAPI.display(player, "&cConvite de clã descartado.");
            }

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
        }
    }
}