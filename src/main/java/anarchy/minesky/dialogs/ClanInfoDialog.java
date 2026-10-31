package anarchy.minesky.dialogs;

import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class ClanInfoDialog {
    private static final DecimalFormat DF = new DecimalFormat("0.#");

    public static void open(Player player) {
        Clan clan = ClanHandler.getClanByPlayer(player.getUniqueId());

        if (clan == null) {
            ClanCreateDialog.open(player);
            return;
        }

        OfflinePlayer leader = Bukkit.getOfflinePlayer(clan.getLeader());
        String leaderName = leader.getName() != null ? leader.getName() : "Desconhecido";
        String roleName = ClanRoles.getLabelRole(clan.getRole(player.getUniqueId()));
        boolean isLeader = clan.getLeader().equals(player.getUniqueId());

        StringBuilder body = new StringBuilder();
        body.append("&7• Nome: &f").append(clan.getName()).append(" &8[").append(clan.getTag()).append("&8]\n");
        body.append("&7• Líder: &f").append(leaderName).append("\n");
        body.append("&7• Seu Cargo: &f").append(roleName).append("\n");
        body.append("&7• Nível: &a").append(clan.getLevel()).append(" &7(&b").append(DF.format(clan.getXp()))
                .append("&7/&b").append(DF.format(clan.xpRequiredForNextLevel())).append(" XP&7)\n");
        body.append("&7• Saldo do Banco: &a$").append(clan.getBalance()).append("\n");
        body.append("&7• Membros: &f").append(ClanHandler.getOnlineMembers(clan)).append("\n");
        body.append("&7• Aliados: &b").append(clan.getAllies().size()).append(" &7| Rivais: &c").append(clan.getRivals().size()).append("\n\n");
        body.append("&7Selecione uma ação abaixo:");

        MineSkyDialog.Builder dialog = MineSkyDialog.builder()
                .title("&fGerenciar Clã &8[" + clan.getTag() + "&8]")
                .body(body.toString());

        if (clan.getBanner() != null && !clan.getBanner().getType().isAir()) {
            dialog.bodyItem(clan.getBanner());
        }

        dialog.button("&fMembros", p -> p.performCommand("clan membros"));
        dialog.button("&fEditar", p -> p.performCommand("clan editar"));
        dialog.button("&fBase", p -> p.performCommand("clan base teleportar"));
        dialog.button("&fSetar Base", p -> p.performCommand("clan base setar"));
        dialog.button("&fReagrupar", p -> p.performCommand("clan reagrupar"));
        dialog.button("&fFogo Amigo", p -> p.performCommand("clan fogo-amigo"));

        dialog.button("&aBanco ($)", p -> openBankDialog(p, clan));
        dialog.button("&bAliados & Rivais", p -> openRelationsDialog(p, clan));

        dialog.button("&cAbandonar Clã", p -> p.performCommand("clan sair"));

        if (isLeader) {
            dialog.button("&4Dissolver Clã", p -> p.performCommand("clan acabar"));
        }

        dialog.cancelButton("&7Voltar", ClanDialog::open);

        dialog.show(player);
    }

    public static void openBankDialog(Player player, Clan clan) {
        MineSkyDialog.builder()
                .title("&fBanco do Clã")
                .body("&7Saldo atual no cofre: &a$" + clan.getBalance() + "\n&7Digite o valor desejado no campo abaixo:")
                .textInput("amount", "Valor:", "Ex: 1000", "")
                .button("&aDepositar", (p, response) -> {
                    String amount = response.getText("amount");
                    if (amount != null && !amount.isBlank()) {
                        p.performCommand("clan banco depositar " + amount.trim());
                    }
                })
                .button("&cSacar", (p, response) -> {
                    String amount = response.getText("amount");
                    if (amount != null && !amount.isBlank()) {
                        p.performCommand("clan banco sacar " + amount.trim());
                    }
                })
                .cancelButton("&7Voltar", ClanInfoDialog::open)
                .show(player);
    }

    public static void openRelationsDialog(Player player, Clan clan) {
        MineSkyDialog.builder()
                .title("&fAliados e Rivais")
                .body("&7Digite a tag do clã alvo para adicionar ou remover alianças/rivalidades:")
                .textInput("tag", "Tag do Clã:", "Ex: TAG", "")
                .button("&b+ Adicionar Aliado", (p, res) -> executeRelationCommand(p, "aliado adicionar", res.getText("tag")))
                .button("&7- Remover Aliado", (p, res) -> executeRelationCommand(p, "aliado remover", res.getText("tag")))
                .button("&c+ Declarar Rival", (p, res) -> executeRelationCommand(p, "rival adicionar", res.getText("tag")))
                .button("&7- Remover Rival", (p, res) -> executeRelationCommand(p, "rival remover", res.getText("tag")))
                .cancelButton("&7Voltar", ClanInfoDialog::open)
                .show(player);
    }

    private static void executeRelationCommand(Player player, String subCommand, String tag) {
        if (tag != null && !tag.isBlank()) {
            player.performCommand("clan " + subCommand + " " + tag.trim());
        }
    }

    public static void openMainMenu(Player player) {
        open(player);
    }
}