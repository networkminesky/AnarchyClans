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
import anarchy.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class AcceptSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "aceitar";
    }

    @Override
    public String getDescription() {
        return "Aceita convites, alianças, paz ou reagrupamentos";
    }

    @Override
    public String getUsage() {
        return "/clan aceitar";
    }

    @Override
    public List<String> getAliases() {
        return List.of("accept");
    }

    @Override
    public boolean getAdminCommand() {
        return false;
    }

    @Override
    public void perform(Player player, String[] args) {
        AnarchyClans plugin = AnarchyClans.getInstance();
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
            sendError(player, "&cVocê não tem pedidos pendentes.");
            return;
        }

        if (hasReagroup) {
            reagroupHandler.accept(player);
            return;
        }

        if (hasAllyRequest) {
            if (!ClanRoles.isLeaders(ownClan.getRole(playerId))) {
                sendError(player, "&cApenas líderes podem aceitar alianças.");
                return;
            }

            Clan allyClan = allyHandler.getRequestClan(ownClan);
            if (allyClan == null) {
                allyHandler.removeRequest(ownClan);
                sendError(player, "&cO clã solicitante não foi encontrado.");
                return;
            }

            ClanHandler.addAlly(allyClan, ownClan, player);
            allyHandler.removeRequest(ownClan);

            AnarchyClans.l.info("[Clãs] " + player.getName() + " aceitou aliança com " + allyClan.getName());
            FeedbackAPI.display(player, "&aAliança selada com &f" + allyClan.getName() + "&a!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
            return;
        }

        if (hasRivalRequest) {
            if (!ClanRoles.isLeaders(ownClan.getRole(playerId))) {
                sendError(player, "&cApenas líderes podem aceitar pedidos de paz.");
                return;
            }

            Clan rivalClan = rivalHandler.getRequestClan(ownClan);
            if (rivalClan == null) {
                rivalHandler.removeRequest(ownClan);
                sendError(player, "&cO clã solicitante não foi encontrado.");
                return;
            }

            ClanHandler.removeRival(rivalClan, ownClan, player);
            rivalHandler.removeRequest(ownClan);

            AnarchyClans.l.info("[Clãs] " + player.getName() + " aceitou pedido de paz com " + rivalClan.getName());
            FeedbackAPI.display(player, "&aPaz selada com &f" + rivalClan.getName() + "&a!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
            return;
        }

        if (hasInvite) {
            if (ClanHandler.hasClan(player)) {
                sendError(player, "&cVocê já pertence a um clã. Saia dele antes de aceitar outro convite.");
                return;
            }

//            var traitorHandler = plugin.getTraitorHandler();
//            if (traitorHandler != null && traitorHandler.isTraitor(playerId)) {
//                sendError(player, "&cVocê é um traidor e não pode entrar em clãs.");
//                return;
//            }

            Clan clan = plugin.getInviteHandler().getInviteClan(playerId);
            if (clan == null) {
                plugin.getInviteHandler().removeInvite(playerId);
                sendError(player, "&cO clã do convite não existe mais.");
                return;
            }

            if (clan.getMembers().size() >= clan.getMemberLimit()) {
                plugin.getInviteHandler().removeInvite(playerId);
                sendError(player, "&cO clã atingiu o limite de membros (" + clan.getMemberLimit() + ").");
                return;
            }

            ClanHandler.addMember(player, clan);
            plugin.getInviteHandler().removeInvite(playerId);
            AnarchyClans.l.info("[Clãs] " + player.getName() + " aceitou o convite do clã " + clan.getName());
            Bukkit.broadcastMessage(Utils.c("&b" + player.getName() + " &7entrou para o clã &f" + clan.getName() + " &8[" + clan.getTag() + "&8]"));

            FeedbackAPI.display(player, "&aVocê entrou para o clã &f" + clan.getName() + "&a!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
        }
    }
}