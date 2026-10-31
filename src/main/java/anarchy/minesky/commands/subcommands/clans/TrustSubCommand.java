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

public class TrustSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "confiar";
    }

    @Override
    public String getDescription() {
        return "Promove um recruta para membro confiável";
    }

    @Override
    public String getUsage() {
        return "/clan confiar <jogador>";
    }

    @Override
    public List<String> getAliases() {
        return List.of("trust");
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
            sendError(player, "&cApenas capitães ou superiores podem confiar membros.");
            return;
        }

        if (args.length < 2) {
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
            sendError(player, "&cVocê não pode confiar a si mesmo.");
            return;
        }

        if (!clan.getMembers().containsKey(targetUUID)) {
            sendError(player, "&cEste jogador não é membro do seu clã.");
            return;
        }

        ClanRoles targetRole = clan.getRole(targetUUID);
        if (targetRole != ClanRoles.RECRUIT) {
            sendError(player, "&cEste jogador já é confiado (cargo atual: " + targetRole.getLabel() + ").");
            return;
        }

        clan.getMemberData(targetUUID).setRole(ClanRoles.MEMBER);
        ClanHandler.saveClan(clan);

        String targetName = target.getName() != null ? target.getName() : args[1];
        AnarchyClans.l.info("[CLÃS] " + player.getName() + " confiou em " + targetName + " no clã " + clan.getName());
        ClanHandler.broadcastClanMessage(clan, "&f" + targetName + " &7foi confiado e promovido a &fMembro &7por &f" + player.getName() + "&7.");
        FeedbackAPI.display(player, "&aVocê confiou em &f" + targetName + "&a!");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);

        Player targetOnline = target.getPlayer();
        if (targetOnline != null && targetOnline.isOnline()) {
            FeedbackAPI.display(targetOnline, "&aVocê foi confiado e promovido a &fMembro &ano clã!");
            targetOnline.playSound(targetOnline.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        }
    }
}