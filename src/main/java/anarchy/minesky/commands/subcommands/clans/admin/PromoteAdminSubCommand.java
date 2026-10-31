package anarchy.minesky.commands.subcommands.clans.admin;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.data.Clan;
import anarchy.minesky.data.MemberData;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class PromoteAdminSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "promover-admin";
    }

    @Override
    public String getDescription() {
        return "Promove o cargo de um membro de qualquer clã";
    }

    @Override
    public String getUsage() {
        return "/clan admin promover-admin <jogador> <cargo>";
    }

    @Override
    public List<String> getAliases() {
        return List.of("promote-admin", "promoverglobal");
    }

    @Override
    public boolean getAdminCommand() {
        return true;
    }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 3) {
            sendError(player, "&cUso: " + getUsage());
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendError(player, "&cJogador '" + args[1] + "' não encontrado.");
            return;
        }

        UUID targetUUID = target.getUniqueId();
        Clan clan = ClanHandler.getClanByPlayer(targetUUID);

        if (clan == null) {
            sendError(player, "&cEste jogador não pertence a nenhum clã.");
            return;
        }

        ClanRoles newRole = ClanRoles.getRole(args[2]);
        if (newRole == null) {
            sendError(player, "&cCargo inválido. Use: Líder, Sub-Líder, Capitão, Recrutador, Leal, Membro ou Recruta.");
            return;
        }

        ClanRoles currentRole = clan.getRole(targetUUID);
        if (currentRole == newRole) {
            sendError(player, "&cEste jogador já possui o cargo " + newRole.getLabel() + ".");
            return;
        }

        if (newRole == ClanRoles.LEADER) {
            UUID oldLeaderUuid = clan.getLeader();
            if (!oldLeaderUuid.equals(targetUUID)) {
                MemberData oldLeaderData = clan.getMemberData(oldLeaderUuid);
                if (oldLeaderData != null) {
                    oldLeaderData.setRole(ClanRoles.SUB_LEADER);
                }
                clan.setLeader(targetUUID);
            }
        }

        clan.getMemberData(targetUUID).setRole(newRole);
        ClanHandler.saveClan(clan);

        String targetName = target.getName() != null ? target.getName() : args[1];
        String roleLabel = newRole.getLabel();

        AnarchyClans.l.info("[ADMIN] " + player.getName() + " promoveu " + targetName + " para " + roleLabel + " no clã " + clan.getName());

        ClanHandler.broadcastClanMessage(clan, "&f" + targetName + " &afoi promovido a &f" + roleLabel + "&a.");

        FeedbackAPI.display(player, "&aVocê promoveu &f" + targetName + " &apara &f" + roleLabel + "&a no clã &f" + clan.getName() + "&a.");

        Player targetOnline = target.getPlayer();
        if (targetOnline != null && targetOnline.isOnline()) {
            FeedbackAPI.display(targetOnline, "&aVocê foi promovido a &f" + roleLabel + "&a no clã!");
        }
    }
}