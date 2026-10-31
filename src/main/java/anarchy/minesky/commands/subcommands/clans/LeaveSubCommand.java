package anarchy.minesky.commands.subcommands.clans;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class LeaveSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "abandonar";
    }

    @Override
    public String getDescription() {
        return "Abandona o seu clã atual";
    }

    @Override
    public String getUsage() {
        return "/clan abandonar";
    }

    @Override
    public List<String> getAliases() {
        return List.of("sair", "leave");
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

        if (clan.getRole(player.getUniqueId()) == ClanRoles.LEADER) {
            sendError(player, "&cVocê é o líder. Transfira a liderança ou use /clan desbandar.");
            return;
        }

        openConfirmDialog(player, clan);
    }

    private void openConfirmDialog(Player player, Clan clan) {
//        var warHandler = AnarchyClans.getInstance().getWarHandler();
//        boolean inWar = warHandler != null && warHandler.isClanInActiveWar(clan.getId());

        StringBuilder body = new StringBuilder();
        body.append("&7Tem certeza de que deseja sair do clã &f").append(clan.getName())
                .append(" &8[").append(clan.getTag()).append("&8]&7?\n\n");

//        if (inWar) {
//            body.append("&cAVISO CRÍTICO:\n")
//                    .append("&cSeu clã está em guerra! Sair agora fará você se tornar um TRAIDOR GLOBAL!\n\n");
//        }

        body.append("&7Você perderá seu cargo, base e todos os benefícios do clã.");

        MineSkyDialog.builder()
                .title("&cAbandonar Clã")
                .body(body.toString())
                .button("&cConfirmar Saída", p -> handleLeave(p, clan))
                .cancelButton("&7Cancelar", p -> p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f))
                .show(player);
    }

    private void handleLeave(Player player, Clan clan) {
        if (!clan.getMembers().containsKey(player.getUniqueId())) {
            sendError(player, "&cVocê não faz mais parte deste clã.");
            return;
        }

        String clanName = clan.getName();
        String clanTag = clan.getTag();

        ClanHandler.broadcastClanMessage(clan, "&f" + player.getName() + " &csaiu do clã.");
        ClanHandler.removeMember(player, clan);

        AnarchyClans.l.info("[CLÃS] " + player.getName() + " saiu do clã " + clanName);
        FeedbackAPI.display(player, "&aVocê saiu do clã.");

//        if (inWar) {
//            var traitorHandler = AnarchyClans.getInstance().getTraitorHandler();
//            if (traitorHandler != null) {
//                traitorHandler.addTraitor(player.getUniqueId(), clanName);
//            }
//
//            Location loc = player.getLocation();
//            if (loc.getWorld() != null) {
//                loc.getWorld().strikeLightningEffect(loc);
//            }
//
//            // Notificação global no estilo Vanilla
//            Bukkit.broadcastMessage(Utils.c("&c[Traidor] &f" + player.getName() + " &7desertou o clã &f" + clanName + " &8[" + clanTag + "&8] &7em combate e tornou-se um &cTraidor Global&7!"));
//
//            for (Player p : Bukkit.getOnlinePlayers()) {
//                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.8f, 1.0f);
//            }
//
//            FeedbackAPI.display(player, "&cVocê desertou em guerra e virou um Traidor Global!", FeedbackPriority.HIGH);
//        }
    }
}