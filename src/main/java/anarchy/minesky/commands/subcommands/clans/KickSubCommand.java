package anarchy.minesky.commands.subcommands.clans;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class KickSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "expulsar";
    }

    @Override
    public String getDescription() {
        return "Expulsa um membro do seu clã";
    }

    @Override
    public String getUsage() {
        return "/clan expulsar <membro>";
    }

    @Override
    public List<String> getAliases() {
        return List.of("kick", "kickar");
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

        if (!ClanRoles.isLeadership(playerRole)) {
            sendError(player, "&cApenas capitães ou superiores podem expulsar membros.");
            return;
        }

        if (args.length < 2 || args[1].isBlank()) {
            sendError(player, "&cUso: " + getUsage());
            return;
        }

        String targetName = args[1];

        OfflinePlayer target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            for (UUID memberId : clan.getMembers().keySet()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(memberId);
                if (op.getName() != null && op.getName().equalsIgnoreCase(targetName)) {
                    target = op;
                    break;
                }
            }
        }

        if (target == null) {
            target = Bukkit.getOfflinePlayer(targetName);
        }

        UUID targetUUID = target.getUniqueId();
        if (!clan.getMembers().containsKey(targetUUID)) {
            sendError(player, "&cEste jogador não pertence ao seu clã.");
            return;
        }

        if (targetUUID.equals(player.getUniqueId())) {
            sendError(player, "&cVocê não pode expulsar a si mesmo. Use /clan sair.");
            return;
        }

        if (targetUUID.equals(clan.getLeader())) {
            sendError(player, "&cVocê não pode expulsar o líder supremo do clã.");
            return;
        }

        ClanRoles targetRole = clan.getRole(targetUUID);

        if (!playerRole.isHigherThan(targetRole)) {
            sendError(player, "&cVocê só pode expulsar membros com cargo inferior ao seu.");
            return;
        }

        ClanHandler.removeMember(targetUUID, clan);

        String name = target.getName() != null ? target.getName() : targetName;
        AnarchyClans.l.info("[CLÃS] " + player.getName() + " expulsou " + name + " do clã " + clan.getName());
        ClanHandler.broadcastClanMessage(clan, "&f" + name + " &cfoi expulso do clã por &f" + player.getName() + "&c.");
        FeedbackAPI.display(player, "&aVocê expulsou &f" + name + " &ado clã.");
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            FeedbackAPI.display(onlineTarget, "&cVocê foi expulso do clã &f" + clan.getName() + "&c.", FeedbackPriority.HIGH);
        }
    }
}