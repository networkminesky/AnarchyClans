package anarchy.minesky.commands.subcommands.clans;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class DemoteSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "rebaixar";
    }

    @Override
    public String getDescription() {
        return "Rebaixa o cargo de um membro do seu clã";
    }

    @Override
    public String getUsage() {
        return "/clan rebaixar <jogador>";
    }

    @Override
    public List<String> getAliases() {
        return List.of("demote");
    }

    @Override
    public boolean getAdminCommand() {
        return false;
    }

    @Override
    public void perform(Player player, String[] args) {
        Clan clan = ClanHandler.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            sendError(player, "&cVocê não pertence a um clã.");
            return;
        }

        ClanRoles playerRole = clan.getRole(player.getUniqueId());

        if (!ClanRoles.isLeaders(playerRole)) {
            sendError(player, "&cApenas líderes podem rebaixar membros.");
            return;
        }

        if (args.length < 2) {
            sendError(player, "&cUso: " + getUsage());
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        UUID targetUUID = target.getUniqueId();

        if (targetUUID.equals(player.getUniqueId())) {
            sendError(player, "&cVocê não pode rebaixar a si mesmo.");
            return;
        }

        if (!clan.getMembers().containsKey(targetUUID)) {
            sendError(player, "&cEste jogador não pertence ao seu clã.");
            return;
        }

        ClanRoles targetRole = clan.getRole(targetUUID);

        if (targetRole == ClanRoles.LEADER) {
            sendError(player, "&cVocê não pode rebaixar o líder supremo do clã.");
            return;
        }

        if (targetRole == ClanRoles.RECRUIT) {
            sendError(player, "&cEste membro já está no cargo mais baixo (Recruta).");
            return;
        }

        if (!playerRole.isHigherThan(targetRole)) {
            sendError(player, "&cVocê só pode rebaixar membros com cargo inferior ao seu.");
            return;
        }

        ClanRoles newRole = ClanRoles.values()[targetRole.ordinal() + 1];

        clan.getMemberData(targetUUID).setRole(newRole);
        ClanHandler.saveClan(clan);

        String targetName = target.getName() != null ? target.getName() : args[1];
        String roleLabel = newRole.getLabel();

        AnarchyClans.l.info("[CLÃS] " + player.getName() + " rebaixou " + targetName + " para " + roleLabel + " no clã " + clan.getName());

        ClanHandler.broadcastClanMessage(clan, "&f" + targetName + " &cfoi rebaixado para &f" + roleLabel + "&c por &f" + player.getName() + "&c.");

        FeedbackAPI.display(player, "&aVocê rebaixou &f" + targetName + " &apara &f" + roleLabel + "&a.");

        Player targetOnline = target.getPlayer();
        if (targetOnline != null && targetOnline.isOnline()) {
            FeedbackAPI.display(targetOnline, "&cVocê foi rebaixado para &f" + roleLabel + "&c.");
        }
    }
}