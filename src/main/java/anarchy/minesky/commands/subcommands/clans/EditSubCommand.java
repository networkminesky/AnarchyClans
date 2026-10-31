package anarchy.minesky.commands.subcommands.clans;

import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.data.Clan;
import anarchy.minesky.dialogs.ClanEditDialog;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import org.bukkit.entity.Player;

import java.util.List;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class EditSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "editar";
    }

    @Override
    public String getDescription() {
        return "Altera as informações e configurações do clã";
    }

    @Override
    public String getUsage() {
        return "/clan editar";
    }

    @Override
    public List<String> getAliases() {
        return List.of("edit");
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

        if (!ClanRoles.isLeaders(clan.getRole(player.getUniqueId()))) {
            sendError(player, "&cApenas líderes podem editar o clã.");
            return;
        }

        ClanEditDialog.open(player);
    }
}