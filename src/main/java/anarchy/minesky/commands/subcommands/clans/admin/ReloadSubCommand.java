package anarchy.minesky.commands.subcommands.clans.admin;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.utils.Utils;
import org.bukkit.entity.Player;

import java.util.List;

public class ReloadSubCommand extends SubCommand {
    private final AnarchyClans plugin = AnarchyClans.getInstance();

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Recarregar o plugin. (Não Recomendado)";
    }

    @Override
    public String getUsage() {
        return "/clan admin reload";
    }

    @Override
    public List<String> getAliases() {
        return List.of("reconnect");
    }

    @Override
    public boolean getAdminCommand() {
        return true;
    }

    @Override
    public void perform(Player player, String[] args) {
        plugin.loadConfigs();
        plugin.loadMongoDB();
        plugin.handler = new ClanHandler();
        player.sendMessage(Utils.c("&aPlugin recarregado."));
        AnarchyClans.l.info("[Clãs] " + player.getName() + " recarregou o plugin.");
    }
}
