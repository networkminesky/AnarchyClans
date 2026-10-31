package anarchy.minesky.commands;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanChatType;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.utils.Utils;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ClanChatCommand implements CommandExecutor, TabCompleter {

    private final AnarchyClans plugin;
    private final ClanChatType type;

    public ClanChatCommand(AnarchyClans plugin, ClanChatType type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!(s instanceof Player player)) {
            s.sendMessage(Utils.c("§c❌ Este comando só pode ser executado por jogadores."));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Utils.c("§c⚠ Uso correto: §f/" + label + " <mensagem>"));
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return true;
        }

        Clan guild = ClanHandler.getClanByPlayer(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(Utils.c("§c🚫 Você não faz parte de nenhum clã."));
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return true;
        }

        if (type == ClanChatType.ALLY && guild.getRole(player.getUniqueId()) == ClanRoles.RECRUIT) {
            player.sendMessage(Utils.c("&4⚠ &cVocê é um RECRUTA e não pode utilizar esse chat."));
            return true;
        }

        String message = String.join(" ", args);
//        PlayerChatEvent event = new PlayerChatEvent(player, guild, message, type);
//        plugin.getServer().getPluginManager().callEvent(event);
//        if (event.isCancelled()) {
//            player.sendMessage((event.CancelledMessage == null ? Utils.c("&c⚠ Ops! A entrada no clã foi interrompida pela API.") : event.CancelledMessage));
//            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
//            return true;
//        }
        switch (type) {
            case CLAN -> ClanHandler.broadcastClanChat(player, guild, Utils.c(message), false);
            case ALLY -> ClanHandler.broadcastClanChat(player, guild, Utils.c(message), true);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String lbl, @NotNull String[] args) {
        if (s instanceof Player player) {
            player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 1);
        }
        return Utils.getOnlinePlayerNames();
    }
}
