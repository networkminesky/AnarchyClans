package anarchy.minesky.dialogs;

import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.data.Clan;
import anarchy.minesky.handlers.ClanHandler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ClanListDialog {

    private static final int CLANS_PER_PAGE = 5;
    private static final DecimalFormat DF = new DecimalFormat("0.#");

    public static void open(Player player) {
        open(player, 1);
    }

    public static void open(Player player, int page) {
        List<Clan> clans = new ArrayList<>(ClanHandler.getClans().values());

        clans.sort((a, b) -> Integer.compare(b.getLevel(), a.getLevel()));

        if (clans.isEmpty()) {
            MineSkyDialog.builder()
                    .title("&fLista de Clãs")
                    .body("&7Nenhum clã foi fundado no servidor ainda.\n&7Seja o primeiro a criar um!")
                    .button("&aFundar um Clã", p -> ClanCreateDialog.open(p))
                    .cancelButton("&7Voltar", ClanDialog::open)
                    .show(player);
            return;
        }

        int totalClans = clans.size();
        int totalPages = (int) Math.ceil((double) totalClans / CLANS_PER_PAGE);
        int currentPage = Math.max(1, Math.min(page, totalPages));

        int start = (currentPage - 1) * CLANS_PER_PAGE;
        int end = Math.min(start + CLANS_PER_PAGE, totalClans);

        StringBuilder body = new StringBuilder();
        body.append("&7Clãs registrados no servidor &8(")
                .append(totalClans).append(" no total)&7:\n\n");

        for (int i = start; i < end; i++) {
            Clan c = clans.get(i);
            OfflinePlayer leader = Bukkit.getOfflinePlayer(c.getLeader());
            String leaderName = leader.getName() != null ? leader.getName() : "Desconhecido";

            body.append("&e").append(i + 1).append(". &f").append(c.getName())
                    .append(" &8[").append(c.getTag()).append("&8]\n")
                    .append("   &7Nível: &a").append(c.getLevel())
                    .append(" &7| Líder: &f").append(leaderName)
                    .append(" &7| Membros: &f").append(ClanHandler.getOnlineMembers(c)).append("\n\n");
        }

        body.append("&7Clique em um clã abaixo para inspecionar:");

        MineSkyDialog.Builder dialog = MineSkyDialog.builder()
                .title("&fClãs &8(Pág. " + currentPage + "/" + totalPages + ")")
                .body(body.toString());

        for (int i = start; i < end; i++) {
            Clan c = clans.get(i);
            dialog.button("&f" + c.getName() + " &8[" + c.getTag() + "&8]", p -> openClanView(p, c, currentPage));
        }

        if (currentPage > 1) {
            dialog.button("&7← Anterior", p -> open(p, currentPage - 1));
        }

        if (currentPage < totalPages) {
            dialog.button("&7Próxima →", p -> open(p, currentPage + 1));
        }

        dialog.cancelButton("&cVoltar", ClanDialog::open);

        dialog.show(player);
    }

    public static void openClanView(Player player, Clan clan, int returnPage) {
        OfflinePlayer leader = Bukkit.getOfflinePlayer(clan.getLeader());
        String leaderName = leader.getName() != null ? leader.getName() : "Desconhecido";
        String description = clan.getDescription() != null && !clan.getDescription().isBlank()
                ? clan.getDescription()
                : "Sem descrição.";

        StringBuilder body = new StringBuilder();
        body.append("&7• Nome: &f").append(clan.getName()).append(" &8[").append(clan.getTag()).append("&8]\n");
        body.append("&7• Descrição: &f").append(description).append("\n");
        body.append("&7• Líder: &f").append(leaderName).append("\n");
        body.append("&7• Nível: &a").append(clan.getLevel()).append(" &7(&b").append(DF.format(clan.getXp()))
                .append("&7/&b").append(DF.format(clan.xpRequiredForNextLevel())).append(" XP&7)\n");
        body.append("&7• Membros: &f").append(ClanHandler.getOnlineMembers(clan)).append("\n");
        body.append("&7• Aliados: &b").append(clan.getAllies().size()).append(" &7| Rivais: &c").append(clan.getRivals().size()).append("\n");

        MineSkyDialog.Builder dialog = MineSkyDialog.builder()
                .title("&fClã &8[" + clan.getTag() + "&8]")
                .body(body.toString());

        if (clan.getBanner() != null && !clan.getBanner().getType().isAir()) {
            dialog.bodyItem(clan.getBanner());
        }

        dialog.cancelButton("&7Voltar à Lista", p -> open(p, returnPage));

        dialog.show(player);
    }

    public static void openMainMenu(Player player) {
        open(player, 1);
    }

    public static void openMainMenu(Player player, int page) {
        open(player, page);
    }
}
