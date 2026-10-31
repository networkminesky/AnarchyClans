package anarchy.minesky.commands.subcommands.clans;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.config.Config;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.hooks.AnarchyEconomyHook;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class InviteSubCommand extends SubCommand {

    private final AnarchyClans plugin = AnarchyClans.getInstance();

    @Override
    public String getName() {
        return "convidar";
    }

    @Override
    public String getDescription() {
        return "Convida um jogador para o seu clã";
    }

    @Override
    public String getUsage() {
        return "/clan convidar <jogador>";
    }

    @Override
    public List<String> getAliases() {
        return List.of("invite");
    }

    @Override
    public boolean getAdminCommand() {
        return false;
    }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 2) {
            sendError(player, "&cUso: " + getUsage());
            return;
        }

        Clan clan = ClanHandler.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            sendError(player, "&cVocê não pertence a um clã.");
            return;
        }

        if (!ClanRoles.isLeadershipAndRecruiter(clan.getRole(player.getUniqueId()))) {
            sendError(player, "&cVocê não tem permissão para convidar membros.");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            sendError(player, "&cJogador não encontrado ou offline.");
            return;
        }

        if (target.equals(player)) {
            sendError(player, "&cVocê não pode convidar a si mesmo.");
            return;
        }

        if (clan.getMembers().containsKey(target.getUniqueId())) {
            sendError(player, "&cEste jogador já está no seu clã.");
            return;
        }

        if (ClanHandler.hasClan(target)) {
            sendError(player, "&cEste jogador já pertence a um clã.");
            return;
        }

//        var traitorHandler = plugin.getTraitorHandler();
//        if (traitorHandler != null && traitorHandler.isTraitor(target.getUniqueId())) {
//            sendError(player, "&cO jogador " + target.getName() + " é um traidor e não pode entrar em clãs.");
//            return;
//        }

        if (clan.getMembers().size() >= clan.getMemberLimit()) {
            sendError(player, "&cO clã atingiu o limite de membros (" + clan.getMemberLimit() + ").");
            return;
        }

        if (plugin.getInviteHandler().hasInvite(target.getUniqueId())) {
            Clan invitedClan = plugin.getInviteHandler().getInviteClan(target.getUniqueId());
            if (invitedClan != null && invitedClan.getId().equals(clan.getId())) {
                sendError(player, "&cEste jogador já possui um convite pendente do seu clã.");
                return;
            }
        }

        double price = Config.InvitePrice;

        if (price > 0) {
            AnarchyEconomyHook.withdraw(player, price, success -> {
                if (!success) {
                    sendError(player, "&cVocê precisa de &a$" + price + " &cpara enviar um convite.");
                    return;
                }
                finishInvite(player, target, clan, price);
            });
        } else {
            finishInvite(player, target, clan, 0);
        }
    }

    private void finishInvite(Player player, Player target, Clan clan, double price) {
        plugin.getInviteHandler().sendInvite(target, clan);

        AnarchyClans.l.info("[CLÃS] " + player.getName() + " convidou " + target.getName() + " para o clã " + clan.getName());

        ClanHandler.broadcastClanMessage(clan, "&f" + target.getName() + " &7foi convidado para o clã por &f" + player.getName() + "&7.");

        String costText = price > 0 ? " &7(Custo: &a$" + price + "&7)" : "";
        FeedbackAPI.display(player, "&aConvite enviado para &f" + target.getName() + "&a!" + costText);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
    }
}