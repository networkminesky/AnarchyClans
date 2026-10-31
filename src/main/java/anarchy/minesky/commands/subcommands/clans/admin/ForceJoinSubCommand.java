package anarchy.minesky.commands.subcommands.clans.admin;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class ForceJoinSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "forçar-entrada";
    }

    @Override
    public String getDescription() {
        return "Força a entrada de um jogador em um clã";
    }

    @Override
    public String getUsage() {
        return "/clan admin forçar-entrada <tag> [jogador] [cargo]";
    }

    @Override
    public List<String> getAliases() {
        return List.of("forcarentrada", "forcejoin");
    }

    @Override
    public boolean getAdminCommand() {
        return true;
    }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 2) {
            sendError(player, "&cUso: " + getUsage());
            return;
        }

        String tag = args[1];
        String targetNick = args.length >= 3 ? args[2] : player.getName();
        String roleArg = args.length >= 4 ? args[3] : "RECRUTA";

        Clan clan = ClanHandler.getClanByTag(tag);
        if (clan == null) {
            sendError(player, "&cClã com a tag '" + tag + "' não encontrado.");
            return;
        }

        Player targetOnline = Bukkit.getPlayerExact(targetNick);
        OfflinePlayer target = targetOnline != null ? targetOnline : Bukkit.getOfflinePlayer(targetNick);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendError(player, "&cJogador '" + targetNick + "' não encontrado.");
            return;
        }

        UUID targetUUID = target.getUniqueId();
        String targetName = target.getName() != null ? target.getName() : targetNick;

        if (clan.getMembers().containsKey(targetUUID)) {
            sendError(player, "&cO jogador " + targetName + " já está no clã " + clan.getName() + ".");
            return;
        }

        Clan oldClan = ClanHandler.getClanByPlayer(targetUUID);
        if (oldClan != null) {
            oldClan.removeMember(targetUUID);
            ClanHandler.saveClan(oldClan);
            FeedbackAPI.display(player, "&7Removido do clã anterior &f" + oldClan.getName() + "&7.");
        }

        ClanRoles newRole = ClanRoles.getRole(roleArg);
        if (newRole == null || newRole == ClanRoles.LEADER) {
            sendError(player, "&cCargo inválido. Use: Sub-Líder, Capitão, Recrutador, Leal, Membro ou Recruta.");
            return;
        }

        clan.addMember(targetUUID, newRole, 0, System.currentTimeMillis());
        ClanHandler.saveClan(clan);

        var locator = AnarchyClans.getInstance().getLocator();
        if (locator != null) {
            if (targetOnline != null) locator.updatePlayer(targetOnline);
            locator.updateClan(clan);
        }

        String roleLabel = newRole.getLabel();
        AnarchyClans.l.info("[ADMIN] " + player.getName() + " forçou a entrada de " + targetName + " no clã " + clan.getName() + " como " + roleLabel);

        Bukkit.broadcastMessage(Utils.c("&7" + targetName + " entrou para o clã &f" + clan.getName() + " &8[" + clan.getTag() + "&8]"));

        FeedbackAPI.display(player, "&aVocê adicionou &f" + targetName + " &ano clã &f" + clan.getName() + " &acomo &f" + roleLabel + "&a.");

        if (targetOnline != null && targetOnline.isOnline()) {
            FeedbackAPI.display(targetOnline, "&aVocê foi adicionado ao clã &f" + clan.getName() + " &acomo &f" + roleLabel + "&a.");
        }
    }
}