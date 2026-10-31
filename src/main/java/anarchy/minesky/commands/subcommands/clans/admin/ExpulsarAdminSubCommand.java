package anarchy.minesky.commands.subcommands.clans.admin;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.data.Clan;
import anarchy.minesky.handlers.ClanHandler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class ExpulsarAdminSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "expulsar-admin";
    }

    @Override
    public String getDescription() {
        return "Expulsa um membro de qualquer clã";
    }

    @Override
    public String getUsage() {
        return "/clan admin expulsar-admin <jogador>";
    }

    @Override
    public List<String> getAliases() {
        return List.of("kick-admin", "expulsarglobal");
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

        if (clan.getLeader().equals(targetUUID)) {
            sendError(player, "&cVocê não pode expulsar o líder. Transfira a liderança ou delete o clã.");
            return;
        }

        ClanHandler.removeMember(targetUUID, clan);

        String targetName = target.getName() != null ? target.getName() : args[1];
        String clanName = clan.getName();

        AnarchyClans.l.info("[ADMIN] " + player.getName() + " expulsou " + targetName + " do clã " + clanName);

        ClanHandler.broadcastClanMessage(clan, "&c" + targetName + " foi expulso por um administrador.");

        FeedbackAPI.display(player, "&aVocê expulsou &f" + targetName + " &ado clã &f" + clanName + "&a.");

        Player targetOnline = target.getPlayer();
        if (targetOnline != null && targetOnline.isOnline()) {
            FeedbackAPI.display(targetOnline, "&cVocê foi expulso do clã &f" + clanName + "&c por um administrador.");
        }
    }
}