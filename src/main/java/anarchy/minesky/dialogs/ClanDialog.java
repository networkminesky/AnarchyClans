package anarchy.minesky.dialogs;

import anarchy.minesky.commands.ClanCommand;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class ClanDialog {
    public static void open(Player player) {
        Clan clan = ClanHandler.getClanByPlayer(player.getUniqueId());

        String clanDisplay = (clan != null)
                ? "&b" + clan.getName() + " &8[" + clan.getTag() + "&8]"
                : "&cSem Clã";

        String roleDisplay = (clan != null)
                ? "&f" + ClanRoles.getLabelRole(clan.getRole(player.getUniqueId()))
                : "&7Nenhum";

        StringBuilder body = new StringBuilder();
        body.append("&7Suas informações:\n");
        body.append("&7• Jogador: &f").append(player.getName()).append("\n");
        body.append("&7• Clã: ").append(clanDisplay).append("\n");
        body.append("&7• Cargo: ").append(roleDisplay).append("\n\n");
        body.append("&7Escolha uma das ações abaixo:");

        MineSkyDialog.Builder dialog = MineSkyDialog.builder()
                .title("&fMenu do Clã")
                .body(body.toString())
                .bodyItem(getPlayerHead(player));

        if (clan != null && clan.getBanner() != null && !clan.getBanner().getType().isAir()) {
            dialog.bodyItem(clan.getBanner());
        }

        if (clan == null) {
            dialog.button("&aFundar Clã", p -> ClanCreateDialog.open(p));
        } else {
            dialog.button("&aMeu Clã", ClanInfoDialog::openMainMenu);
        }

        dialog.button("&fLista de Clãs", p -> {
            ClanListDialog.openMainMenu(p);
        });
        dialog.button("&fJogadores", p -> {
            PlayerListDialog.openMainMenu(p);
        });
        dialog.button("&eComandos", p -> {
           p.performCommand("clan ajuda");
        });

        dialog.cancelButton("&cFechar", p -> {});

        dialog.show(player);
    }

    private static ItemStack getPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(Utils.c("&e" + player.getName()));
            head.setItemMeta(meta);
        }
        return head;
    }
}