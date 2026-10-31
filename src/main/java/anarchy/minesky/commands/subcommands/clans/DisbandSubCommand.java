package anarchy.minesky.commands.subcommands.clans;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class DisbandSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "desbandar";
    }

    @Override
    public String getDescription() {
        return "Dissolve e encerra seu clã permanentemente";
    }

    @Override
    public String getUsage() {
        return "/clan desbandar";
    }

    @Override
    public List<String> getAliases() {
        return List.of("acabar", "disband");
    }

    @Override
    public boolean getAdminCommand() {
        return false;
    }

    @Override
    public void perform(Player player, String[] args) {
        Clan clan = ClanHandler.getClanByPlayer(player.getUniqueId());

        if (clan == null) {
            sendError(player, "&cVocê não pertence a nenhum clã.");
            return;
        }

        if (clan.getRole(player.getUniqueId()) != ClanRoles.LEADER) {
            sendError(player, "&cApenas o líder supremo pode desbandar o clã.");
            return;
        }

//        var warHandler = AnarchyClans.getInstance().getWarHandler();
//        if (warHandler != null && (warHandler.isClanInActiveWar(clan.getId()) || warHandler.isClanInWarOrPending(clan.getId()))) {
//            sendError(player, "&cSeu clã está em guerra e não pode ser dissolvido no momento.");
//            return;
//        }

        openConfirmDialog(player, clan);
    }

    private void openConfirmDialog(Player player, Clan clan) {
        MineSkyDialog.builder()
                .title("&cDissolver Clã")
                .body("&7Tem certeza de que deseja acabar com o clã &f" + clan.getName() + " &8[" + clan.getTag() + "&8]&7?\n\n"
                        + "&cEsta ação é permanente e irreversível!\n"
                        + "&cTodos os membros, base, saldo e estatísticas serão apagados.")
                .button("&cConfirmar Dissolução", p -> handleDisband(p, clan))
                .cancelButton("&7Cancelar", p -> p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f))
                .show(player);
    }

    private void handleDisband(Player player, Clan clan) {
        if (!clan.getLeader().equals(player.getUniqueId())) {
            sendError(player, "&cVocê não tem mais permissão para dissolver este clã.");
            return;
        }

        String clanName = clan.getName();
        String clanTag = clan.getTag();

        ClanHandler.deleteClan(clan.getId());
        AnarchyClans.l.info("[CLÃS] " + player.getName() + " dissolveu o clã " + clanName);
        Bukkit.broadcastMessage(Utils.c("&7O clã &f" + clanName + " &8[" + clanTag + "&8] &cfoi dissolvido pelo seu líder."));
        FeedbackAPI.display(player, "&aO clã foi dissolvido com sucesso.");
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1f, 1f);
    }
}