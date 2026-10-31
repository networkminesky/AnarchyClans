package anarchy.minesky.commands.subcommands.clans.admin;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.data.Clan;
import anarchy.minesky.handlers.ClanHandler;
import org.bukkit.entity.Player;

import java.util.List;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class BankAdminSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "banco-admin";
    }

    @Override
    public String getDescription() {
        return "Gerenciar banco de um clã";
    }

    @Override
    public String getUsage() {
        return "/clan admin banco-admin <dar,tirar,resetar,saldo,setar> <tag> [quantidade]";
    }

    @Override
    public List<String> getAliases() {
        return List.of("bank", "banco");
    }

    @Override
    public boolean getAdminCommand() {
        return true;
    }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 3) {
            sendError(player, "&cUso: " + getUsage());
            return;
        }

        String action = args[1].toLowerCase();
        String tag = args[2];

        Clan clan = ClanHandler.getClanByTag(tag);
        if (clan == null) {
            sendError(player, "&cClã com a tag '" + tag + "' não foi encontrado.");
            return;
        }

        switch (action) {
            case "saldo" -> {
                FeedbackAPI.display(player, "&7Saldo do clã &f" + clan.getName() + "&7: &a$" + clan.getBalance());
            }

            case "dar" -> {
                Integer amount = parsePositiveAmount(player, args, 3);
                if (amount == null) return;

                clan.deposit(amount);
                ClanHandler.saveClan(clan);

                ClanHandler.broadcastClanMessage(clan, "&a$" + amount + " &7foram adicionados ao banco do clã.");
                FeedbackAPI.display(player, "&aAdicionado &f$" + amount + " &ano banco do clã &f" + clan.getName() + "&a.");
                AnarchyClans.l.info("[ADMIN] " + player.getName() + " depositou $" + amount + " no clã " + clan.getName());
            }

            case "tirar" -> {
                Integer amount = parsePositiveAmount(player, args, 3);
                if (amount == null) return;

                if (!clan.withdraw(amount)) {
                    sendError(player, "&cO clã não possui saldo suficiente.");
                    return;
                }
                ClanHandler.saveClan(clan);

                ClanHandler.broadcastClanMessage(clan, "&c$" + amount + " &7foram retirados do banco do clã.");
                FeedbackAPI.display(player, "&aRemovido &f$" + amount + " &ado banco do clã &f" + clan.getName() + "&a.");
                AnarchyClans.l.info("[ADMIN] " + player.getName() + " retirou $" + amount + " do clã " + clan.getName());
            }

            case "setar" -> {
                Integer amount = parsePositiveAmount(player, args, 3);
                if (amount == null) return;

                clan.setBalance(amount);
                ClanHandler.saveClan(clan);

                ClanHandler.broadcastClanMessage(clan, "&7O saldo do clã foi definido para &a$" + amount + "&7.");
                FeedbackAPI.display(player, "&aSaldo do clã &f" + clan.getName() + " &adefinido para &f$" + amount + "&a.");
                AnarchyClans.l.info("[ADMIN] " + player.getName() + " definiu o banco do clã " + clan.getName() + " para $" + amount);
            }

            case "resetar" -> {
                clan.setBalance(0);
                ClanHandler.saveClan(clan);

                ClanHandler.broadcastClanMessage(clan, "&cO saldo do banco do clã foi resetado.");
                FeedbackAPI.display(player, "&aSaldo do clã &f" + clan.getName() + " &aresetado.");
                AnarchyClans.l.info("[ADMIN] " + player.getName() + " resetou o banco do clã " + clan.getName());
            }

            default -> sendError(player, "&cAção inválida. Uso: " + getUsage());
        }
    }

    private Integer parsePositiveAmount(Player player, String[] args, int index) {
        if (args.length <= index) {
            sendError(player, "&cIndique a quantidade. Uso: " + getUsage());
            return null;
        }

        try {
            int value = Integer.parseInt(args[index]);
            if (value < 0) {
                sendError(player, "&cO valor não pode ser negativo.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            sendError(player, "&cQuantidade inválida. Insira um número inteiro.");
            return null;
        }
    }
}