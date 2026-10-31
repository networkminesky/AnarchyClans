package anarchy.minesky.commands.subcommands.clans;

import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.data.Clan;
import anarchy.minesky.data.MemberData;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.hooks.AnarchyVanishHook;
import anarchy.minesky.plugin.AnarchyVanish;
import anarchy.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class MembersSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "membros";
    }

    @Override
    public String getDescription() {
        return "Lista os membros de um clã";
    }

    @Override
    public String getUsage() {
        return "/clan membros [tag]";
    }

    @Override
    public List<String> getAliases() {
        return List.of("membro", "members");
    }

    @Override
    public boolean getAdminCommand() {
        return false;
    }

    @Override
    public void perform(Player player, String[] args) {
        Clan clan;

        if (args.length > 1 && args[1] != null) {
            clan = ClanHandler.getClanByTag(args[1]);
            if (clan == null) {
                sendError(player, "&cClã com a tag '" + args[1] + "' não foi encontrado.");
                return;
            }
        } else {
            clan = ClanHandler.getClanByPlayer(player.getUniqueId());
            if (clan == null) {
                sendError(player, "&cVocê não pertence a um clã. Use: /clan membros <tag>");
                return;
            }
        }

        player.sendMessage(Utils.c("&7Membros de &f" + clan.getName() + " &8[" + clan.getTag() + "&8]&7:"));

        List<Map.Entry<UUID, MemberData>> memberList = new ArrayList<>(clan.getMembers().entrySet());
        memberList.sort(Comparator.comparingInt(e -> e.getValue().getRole().ordinal()));

        int index = 1;
        for (Map.Entry<UUID, MemberData> entry : memberList) {
            UUID memberId = entry.getKey();
            MemberData data = entry.getValue();

            OfflinePlayer op = Bukkit.getOfflinePlayer(memberId);
            Player onlinePlayer = op.getPlayer();

            boolean isOnline = onlinePlayer != null
                    && onlinePlayer.isOnline()
                    && !AnarchyVanishHook.isPlayerVanished(memberId);

            String statusDot = isOnline ? "&a●" : "&7○";
            String name = op.getName() != null ? op.getName() : "Desconhecido";
            String roleName = data.getRole().getLabel();

            player.sendMessage(Utils.c(" &7" + (index++) + ". " + statusDot + " &f" + name + " &8- &7" + roleName));
        }
    }
}