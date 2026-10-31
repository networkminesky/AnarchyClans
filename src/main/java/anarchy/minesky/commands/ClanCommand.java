package anarchy.minesky.commands;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.commands.subcommands.clans.CreateSubCommand;
import anarchy.minesky.commands.subcommands.clans.*;
import anarchy.minesky.commands.subcommands.clans.admin.*;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.dialogs.ClanDialog;
import anarchy.minesky.utils.Utils;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ClanCommand implements CommandExecutor {

    private final Set<SubCommand> subCommands = new HashSet<>();

    public ClanCommand(AnarchyClans plugin) {
        register(
                new AcceptSubCommand(),
                new AllySubCommand(),
                new BankSubCommand(),
                new BannerSubCommand(),
                new BaseSubCommand(),
                new CreateSubCommand(),
                new DemoteSubCommand(),
                new DisbandSubCommand(),
                new EditSubCommand(),
                new InviteSubCommand(),
                new KickSubCommand(),
                new LeaveSubCommand(),
                new MembersSubCommand(),
                new PromoteSubCommand(),
                new ReagroupSubCommand(),
                new RejectSubCommand(),
                new RivalSubCommand(),
                new BankAdminSubCommand(),
                new DemoteAdminSubCommand(),
                new ForceJoinSubCommand(),
                new PromoteAdminSubCommand(),
                new ReloadSubCommand(),
                new SpySubCommand(),
                new isconnectedSubCommand(),
                new DadosAdminSubCommand(),
                new TrustSubCommand()
        );
    }

    private void register(SubCommand... commands) {
        Collections.addAll(this.subCommands, commands);
    }

    private Optional<SubCommand> getSubCommand(String name) {
        return subCommands.stream().filter(cmd -> cmd.matches(name)).findFirst();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String lbl, @NotNull String[] args) {
        if (!(s instanceof Player player)) {
            s.sendMessage(Utils.c("&cComando exclusivo para jogadores."));
            return true;
        }

        if (args.length == 0) {
            ClanDialog.open(player);
            return true;
        }

        String subName = args[0].toLowerCase();

        if (subName.equals("help") || subName.equals("ajuda")) {
            commandList(player);
            return true;
        }

        if (subName.equals("admin")) {
            if (!player.hasPermission("anarchyclans.admin")) {
                sendError(player, "&cVocê não tem permissão.");
                return true;
            }

            if (args.length < 2) {
                sendError(player, "&cUse: /clan admin <subcomando>");
                return true;
            }

            String adminCmd = args[1];
            Optional<SubCommand> sub = getSubCommand(adminCmd).filter(SubCommand::getAdminCommand);

            if (sub.isEmpty()) {
                sendError(player, "&cSubcomando de admin não encontrado.");
                return true;
            }

            sub.get().perform(player, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }

        getSubCommand(subName).ifPresentOrElse(sub -> {
            if (sub.getAdminCommand()) {
                sendError(player, "&cComando não encontrado. Use /clan ajuda.");
                return;
            }
            sub.perform(player, args);
        }, () -> sendError(player, "&cComando não encontrado. Use /clan ajuda."));

        return true;
    }

    public void commandList(CommandSender s) {
        s.sendMessage(Utils.c("&8----------------------------------------"));
        s.sendMessage(Utils.c("&6&lComandos do Clã:"));
        subCommands.stream()
                .filter(sub -> !sub.getAdminCommand() || s.hasPermission("anarchyclans.admin"))
                .forEach(sub -> s.sendMessage(Utils.c(" &e" + sub.getUsage() + " &7- " + sub.getDescription())));
        s.sendMessage(Utils.c("&8----------------------------------------"));
    }

    public static void sendError(Player player, String message) {
        FeedbackAPI.display(player, message, FeedbackPriority.HIGH);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
    }
}