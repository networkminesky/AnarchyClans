package anarchy.minesky.dialogs;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.config.Config;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.data.Clan;
import anarchy.minesky.data.MemberData;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ClanEditDialog {
    public static void open(Player player) {
        Clan clan = ClanHandler.getClanByPlayer(player.getUniqueId());

        if (clan == null) {
            FeedbackAPI.display(player, "&cVocê não pertence a um clã.", FeedbackPriority.HIGH);
            return;
        }

        OfflinePlayer leader = Bukkit.getOfflinePlayer(clan.getLeader());
        String leaderName = leader.getName() != null ? leader.getName() : "Desconhecido";
        String description = clan.getDescription() != null && !clan.getDescription().isBlank()
                ? clan.getDescription()
                : "&7Nenhuma";

        StringBuilder body = new StringBuilder();
        body.append("&7Configurações atuais do clã:\n");
        body.append("&7• Tag: &b").append(clan.getTag()).append("\n");
        body.append("&7• Descrição: &f").append(description).append("\n");
        body.append("&7• Líder atual: &f").append(leaderName).append("\n\n");
        body.append("&7Escolha o que deseja editar:");

        MineSkyDialog.Builder dialog = MineSkyDialog.builder()
                .title("&fEditar Clã &8[" + clan.getTag() + "&8]")
                .body(body.toString());

        dialog.button("&fAlterar Tag", p -> openTagDialog(p, clan));
        dialog.button("&fAlterar Descrição", p -> openDescriptionDialog(p, clan));

        boolean isLeader = clan.getLeader().equals(player.getUniqueId());
        if (isLeader) {
            dialog.button("&cTransferir Liderança", p -> openTransferLeaderDialog(p, clan));
        }

        dialog.cancelButton("&7Voltar", ClanInfoDialog::open);

        dialog.show(player);
    }

    private static void openTagDialog(Player player, Clan clan) {
        MineSkyDialog.builder()
                .title("&fAlterar Tag do Clã")
                .body("&7Digite a nova tag para o clã (máx: " + Config.TagLimit + " letras):")
                .textInput("tag", "Nova Tag:", "Ex: ANA", clan.getTag())
                .button("&aSalvar Tag", (p, response) -> {
                    String rawTag = response.getText("tag") != null ? response.getText("tag").trim() : "";

                    if (rawTag.isEmpty()) {
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        FeedbackAPI.display(p, "&cDigite uma tag válida.", FeedbackPriority.HIGH);
                        openTagDialog(p, clan);
                        return;
                    }

                    if (!Utils.isValidTag(rawTag)) {
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        FeedbackAPI.display(p, "&cA tag pode ter no máximo &f" + Config.TagLimit + " &cletras.", FeedbackPriority.HIGH);
                        openTagDialog(p, clan);
                        return;
                    }

                    String formattedTag = Utils.toSmallCaps(rawTag);

                    if (ClanHandler.doesClanTagExist(formattedTag, clan.getId())) {
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        FeedbackAPI.display(p, "&cA tag &f" + formattedTag + " &cjá está em uso.", FeedbackPriority.HIGH);
                        openTagDialog(p, clan);
                        return;
                    }

                    clan.setTag(formattedTag);
                    ClanHandler.saveClan(clan);

                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1, 1);
                    FeedbackAPI.display(p, "&aTag alterada para &f" + formattedTag + "&a!");
                    open(p);
                })
                .cancelButton("&7Voltar", ClanEditDialog::open)
                .show(player);
    }

    private static void openDescriptionDialog(Player player, Clan clan) {
        String currentDesc = clan.getDescription() != null ? clan.getDescription() : "";

        MineSkyDialog.builder()
                .title("&fEditar Descrição")
                .body("&7Digite a nova descrição do clã:")
                .textInput("description", "Descrição:", "Ex: Clã competitivo de Anarquia", currentDesc)
                .button("&aSalvar Descrição", (p, response) -> {
                    String desc = response.getText("description") != null ? response.getText("description").trim() : "";

                    clan.setDescription(desc.isEmpty() ? null : desc);
                    ClanHandler.saveClan(clan);

                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1, 1);
                    FeedbackAPI.display(p, "&aDescrição do clã atualizada!");
                    open(p);
                })
                .button("&cRemover Descrição", (p, response) -> {
                    clan.setDescription(null);
                    ClanHandler.saveClan(clan);

                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1, 1);
                    FeedbackAPI.display(p, "&aDescrição removida.");
                    open(p);
                })
                .cancelButton("&7Voltar", ClanEditDialog::open)
                .show(player);
    }

    private static void openTransferLeaderDialog(Player player, Clan clan) {
        if (!clan.getLeader().equals(player.getUniqueId())) {
            FeedbackAPI.display(player, "&cVocê não pode transferir a liderança.", FeedbackPriority.HIGH);
            return;
        }

//        var warHandler = AnarchyClans.getInstance().getWarHandler();
//        if (warHandler != null && (warHandler.isClanInActiveWar(clan.getId()) || warHandler.isClanInWarOrPending(clan.getId()))) {
//            FeedbackAPI.display(player, "&cSeu clã está em guerra e não pode trocar de líder.", FeedbackPriority.HIGH);
//            return;
//        }

        MineSkyDialog.builder()
                .title("&cTransferir Liderança")
                .body("&7Digite o nick do membro que receberá a liderança suprema do clã.\n&cEsta ação não pode ser desfeita!")
                .textInput("target", "Membro:", "Nick do jogador", "")
                .button("&cConfirmar Transferência", (p, response) -> {
                    String targetName = response.getText("target") != null ? response.getText("target").trim() : "";

                    if (targetName.isEmpty()) {
                        FeedbackAPI.display(p, "&cDigite o nome de um jogador.", FeedbackPriority.HIGH);
                        openTransferLeaderDialog(p, clan);
                        return;
                    }

                    Player targetOnline = Bukkit.getPlayerExact(targetName);
                    OfflinePlayer target = targetOnline != null ? targetOnline : Bukkit.getOfflinePlayer(targetName);

                    if (!target.hasPlayedBefore() && !target.isOnline()) {
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        FeedbackAPI.display(p, "&cJogador não encontrado no servidor.", FeedbackPriority.HIGH);
                        openTransferLeaderDialog(p, clan);
                        return;
                    }

                    UUID newLeaderUuid = target.getUniqueId();
                    UUID oldLeaderUuid = clan.getLeader();

                    if (newLeaderUuid.equals(oldLeaderUuid)) {
                        FeedbackAPI.display(p, "&cVocê já é o líder do clã.", FeedbackPriority.HIGH);
                        return;
                    }

                    MemberData newLeaderData = clan.getMemberData(newLeaderUuid);
                    if (newLeaderData == null) {
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        FeedbackAPI.display(p, "&cEste jogador não pertence ao seu clã.", FeedbackPriority.HIGH);
                        openTransferLeaderDialog(p, clan);
                        return;
                    }

                    newLeaderData.setRole(ClanRoles.LEADER);

                    MemberData oldData = clan.getMemberData(oldLeaderUuid);
                    if (oldData != null) {
                        oldData.setRole(ClanRoles.SUB_LEADER);
                    }

                    clan.setLeader(newLeaderUuid);
                    ClanHandler.saveClan(clan);

                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1, 1);
                    FeedbackAPI.display(p, "&aLiderança transferida para &f" + target.getName() + "&a!");

                    ClanHandler.broadcastClanMessage(clan, "&f" + target.getName() + " assumiu a liderança do clã.");
                    if (targetOnline != null && targetOnline.isOnline()) {
                        FeedbackAPI.display(targetOnline, "&aVocê foi promovido a líder do clã!");
                    }
                })
                .cancelButton("&7Voltar", ClanEditDialog::open)
                .show(player);
    }

    public static void openMainMenu(Player player) {
        open(player);
    }
}