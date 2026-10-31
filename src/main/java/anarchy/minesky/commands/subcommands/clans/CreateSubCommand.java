package anarchy.minesky.commands.subcommands.clans;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.dialogs.ClanCreateDialog;
import anarchy.minesky.handlers.ClanHandler;
import org.bukkit.entity.Player;

import java.util.List;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class CreateSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "criar";
    }

    @Override
    public String getDescription() {
        return "Criar um novo clã";
    }

    @Override
    public String getUsage() {
        return "/clan criar";
    }

    @Override
    public List<String> getAliases() {
        return List.of("create");
    }

    @Override
    public boolean getAdminCommand() {
        return false;
    }

    @Override
    public void perform(Player player, String[] args) {
        if (ClanHandler.hasClan(player)) {
            sendError(player, "&cVocê já pertence a um clã.");
            return;
        }

        ClanCreateDialog.open(player);
    }
}
