package anarchy.minesky.commands.subcommands.clans.admin;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.utils.Utils;
import org.bukkit.entity.Player;

import java.util.List;

public class isconnectedSubCommand extends SubCommand {
    @Override
    public String getName() {
        return "isconnected";
    }

    @Override
    public String getDescription() {
        return "Verificar as conexões como MONGODB e MAINFRAME";
    }

    @Override
    public String getUsage() {
        return "/clan admin isconnected";
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public boolean getAdminCommand() {
        return true;
    }

    @Override
    public void perform(Player player, String[] args) {
        String mongo = (AnarchyClans.getInstance().mm.isConnected() ? Utils.c("&aConectado e operando!") : Utils.c("Não conectado."));
        player.sendMessage(mongo);
        //player.sendMessage("" + FindOneCallback.class.getProtectionDomain().getCodeSource().getLocation());
        //player.sendMessage("" + PlayerDatabase.class.getProtectionDomain().getCodeSource().getLocation());
    }
}
