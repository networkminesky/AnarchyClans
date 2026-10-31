package anarchy.minesky.commands.subcommands;

import org.bukkit.entity.Player;

import java.util.List;

public abstract class SubCommand {
    public abstract String getName();
    public abstract String getDescription();
    public abstract String getUsage();
    public abstract List<String> getAliases();
    public abstract boolean getAdminCommand();
    public abstract void perform(Player player, String[] args);

    public boolean matches(String input) {
        if (input.equalsIgnoreCase(getName())) return true;
        for (String alias : getAliases()) {
            if (input.equalsIgnoreCase(alias)) return true;
        }
        return false;
    }
}
