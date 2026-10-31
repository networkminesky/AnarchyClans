package anarchy.minesky.commands.subcommands.clans;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class BannerSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "estandarte";
    }

    @Override
    public String getDescription() {
        return "Altera o estandarte do clã com o item da sua mão";
    }

    @Override
    public String getUsage() {
        return "/clan estandarte";
    }

    @Override
    public List<String> getAliases() {
        return List.of("banner");
    }

    @Override
    public boolean getAdminCommand() {
        return false;
    }

    @Override
    public void perform(Player player, String[] args) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.getType().name().endsWith("_BANNER")) {
            sendError(player, "&cSegure um estandarte na mão principal.");
            return;
        }

        Clan clan = ClanHandler.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            sendError(player, "&cVocê não pertence a nenhum clã.");
            return;
        }

        if (!ClanRoles.isLeaders(clan.getRole(player.getUniqueId()))) {
            sendError(player, "&cApenas líderes podem alterar o estandarte do clã.");
            return;
        }

        ItemStack bannerItem = item.clone();
        bannerItem.setAmount(1);

        ClanHandler.setBanner(bannerItem, clan);
        AnarchyClans.l.info("[CLÃS] " + player.getName() + " alterou o estandarte do clã " + clan.getName());

        ClanHandler.broadcastClanMessage(clan, "&f" + player.getName() + " &7atualizou o estandarte do clã.");
        FeedbackAPI.display(player, "&aEstandarte do clã atualizado com sucesso!");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
    }
}