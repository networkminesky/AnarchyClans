package anarchy.minesky.commands.subcommands.clans;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class PromoteSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "promover";
    }

    @Override
    public String getDescription() {
        return "Promove o cargo de um membro do seu clã";
    }

    @Override
    public String getUsage() {
        return "/clan promover <membro> <cargo>";
    }

    @Override
    public List<String> getAliases() {
        return List.of("promote");
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
            sendError(player, "&cApenas capitães ou superiores podem promover membros.");
            return;
        }

        if (args.length < 3) {
            sendError(player, "&cUso: " + getUsage());
            return;
        }

        OfflinePlayer target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            target = Bukkit.getOfflinePlayer(args[1]);
        }

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendError(player, "&cJogador '" + args[1] + "' não encontrado.");
            return;
        }

        UUID targetUUID = target.getUniqueId();

        if (targetUUID.equals(player.getUniqueId())) {
            sendError(player, "&cVocê não pode promover a si mesmo.");
            return;
        }

        if (!clan.getMembers().containsKey(targetUUID)) {
            sendError(player, "&cEste jogador não pertence ao seu clã.");
            return;
        }

        ClanRoles targetRole = clan.getRole(targetUUID);

        ClanRoles newRole = ClanRoles.getRole(args[2]);
        if (newRole == null || newRole == ClanRoles.LEADER) {
            sendError(player, "&cCargo inválido. Use: Sub-Líder, Capitão, Recrutador, Leal, Membro ou Recruta.");
            return;
        }

        if (targetRole == newRole) {
            sendError(player, "&cEste membro já possui o cargo " + newRole.getLabel() + ".");
            return;
        }

        if (!ClanRoles.canPromoteTo(playerRole, targetRole, newRole)) {
            sendError(player, "&cVocê não tem permissão para promover alguém para esse cargo.");
            return;
        }

        clan.getMemberData(targetUUID).setRole(newRole);
        ClanHandler.saveClan(clan);

        String targetName = target.getName() != null ? target.getName() : args[1];
        String roleLabel = newRole.getLabel();

        AnarchyClans.l.info("[CLÃS] " + player.getName() + " promoveu " + targetName + " para " + roleLabel + " no clã " + clan.getName());
        ClanHandler.broadcastClanMessage(clan, "&f" + targetName + " &afoi promovido a &f" + roleLabel + "&a por &f" + player.getName() + "&a.");
        FeedbackAPI.display(player, "&aVocê promoveu &f" + targetName + " &apara &f" + roleLabel + "&a.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);

        Player targetOnline = target.getPlayer();
        if (targetOnline != null && targetOnline.isOnline()) {
            FeedbackAPI.display(targetOnline, "&aVocê foi promovido a &f" + roleLabel + "&a no clã!");
            targetOnline.playSound(targetOnline.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        }
    }
}