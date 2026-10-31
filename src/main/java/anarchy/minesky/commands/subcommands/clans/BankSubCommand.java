package anarchy.minesky.commands.subcommands.clans;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.hooks.AnarchyEconomyHook;
import org.bukkit.entity.Player;

import java.util.List;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class BankSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "banco";
    }

    @Override
    public String getDescription() {
        return "Gerencia o banco do clã";
    }

    @Override
    public String getUsage() {
        return "/clan banco <depositar,sacar,saldo> [quantidade]";
    }

    @Override
    public List<String> getAliases() {
        return List.of("bank");
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

        if (args.length < 2 || args[1].equalsIgnoreCase("saldo")) {
            FeedbackAPI.display(player, "&7Saldo do clã: &a$" + clan.getBalance());
            return;
        }

        if (args.length < 3) {
            sendError(player, "&cUso: " + getUsage());
            return;
        }

        int value;
        try {
            value = Integer.parseInt(args[2]);
            if (value <= 0) {
                sendError(player, "&cO valor deve ser maior que zero.");
                return;
            }
        } catch (NumberFormatException e) {
            sendError(player, "&cValor inválido: insira um número inteiro.");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "depositar", "deposit" -> handleDeposit(player, clan, value);
            case "sacar", "withdraw" -> handleWithdraw(player, clan, value);
            default -> sendError(player, "&cOpção inválida. Use /clan banco <depositar,sacar,saldo> [quantidade]");
        }
    }

    private void handleDeposit(Player player, Clan clan, int value) {
//        var warHandler = AnarchyClans.getInstance().getWarHandler();
//        if (warHandler != null && warHandler.isClanInActiveWar(clan.getId())) {
//            sendError(player, "&cSeu clã está em guerra e o banco está bloqueado para depósitos.");
//            return;
//        }

        AnarchyEconomyHook.withdraw(player, value, success -> {
            if (!success) {
                sendError(player, "&cVocê não possui saldo suficiente.");
                return;
            }

            clan.deposit(value);
            ClanHandler.saveClan(clan);

            ClanHandler.broadcastClanMessage(clan, "&f" + player.getName() + " &7depositou &a$" + value + " &7no banco do clã.");
            FeedbackAPI.display(player, "&aVocê depositou &f$" + value + " &ano banco do clã.");
            AnarchyClans.l.info("[CLÃS] " + player.getName() + " depositou $" + value + " no clã " + clan.getName());
        });
    }

    private void handleWithdraw(Player player, Clan clan, int value) {
        if (!ClanRoles.isLeaders(clan.getRole(player.getUniqueId()))) {
            sendError(player, "&cApenas líderes podem sacar do banco do clã.");
            return;
        }

//        var warHandler = AnarchyClans.getInstance().getWarHandler();
//        if (warHandler != null && (warHandler.isClanInActiveWar(clan.getId()) || warHandler.isClanInWarOrPending(clan.getId()))) {
//            sendError(player, "&cSeu clã está em guerra e o banco está bloqueado para saques.");
//            return;
//        }

        if (!clan.withdraw(value)) {
            sendError(player, "&cO clã não possui saldo suficiente.");
            return;
        }

        AnarchyEconomyHook.deposit(player, value, success -> {
            if (!success) {
                clan.deposit(value);
                sendError(player, "&cErro ao depositar em sua conta.");
                return;
            }

            ClanHandler.saveClan(clan);

            ClanHandler.broadcastClanMessage(clan, "&f" + player.getName() + " &7sacou &c$" + value + " &7do banco do clã.");
            FeedbackAPI.display(player, "&aVocê sacou &f$" + value + " &ado banco do clã.");
            AnarchyClans.l.info("[CLÃS] " + player.getName() + " sacou $" + value + " do clã " + clan.getName());
        });
    }
}