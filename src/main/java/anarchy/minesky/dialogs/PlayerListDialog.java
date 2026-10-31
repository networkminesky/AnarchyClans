package anarchy.minesky.dialogs;

import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.data.Clan;
import anarchy.minesky.data.MemberData;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class PlayerListDialog {
    private static final int PLAYERS_PER_PAGE = 5;

    public static void open(Player player) {
        open(player, 1);
    }

    public static void open(Player player, int page) {
        List<PlayerEntry> entries = buildPlayerRanking();

        if (entries.isEmpty()) {
            MineSkyDialog.builder()
                    .title("&fRanking de Jogadores")
                    .body("&7Nenhum jogador registrado no momento.")
                    .cancelButton("&7Voltar", ClanDialog::open)
                    .show(player);
            return;
        }

        int totalPlayers = entries.size();
        int totalPages = (int) Math.ceil((double) totalPlayers / PLAYERS_PER_PAGE);
        int currentPage = Math.max(1, Math.min(page, totalPages));

        int start = (currentPage - 1) * PLAYERS_PER_PAGE;
        int end = Math.min(start + PLAYERS_PER_PAGE, totalPlayers);

        StringBuilder body = new StringBuilder();
        body.append("&7Ranking de jogadores do servidor &8(")
                .append(totalPlayers).append(" encontrados)&7:\n\n");

        for (int i = start; i < end; i++) {
            PlayerEntry pe = entries.get(i);
            String clanTag = pe.clan != null ? " &8[" + pe.clan.getTag() + "&8]" : " &7[Sem Clã]";
            String status = pe.isOnline ? "&a●" : "&7○";

            body.append("&e#").append(i + 1).append(" ")
                    .append(status).append(" &f").append(pe.name)
                    .append(clanTag).append("\n")
                    .append("   &7Kills: &a").append(pe.kills)
                    .append(" &7| Cargo: &f").append(pe.role != null ? ClanRoles.getLabelRole(pe.role) : "Nenhum")
                    .append("\n\n");
        }

        body.append("&7Clique em um jogador para ver o perfil completo:");

        MineSkyDialog.Builder dialog = MineSkyDialog.builder()
                .title("&fJogadores &8(Pág. " + currentPage + "/" + totalPages + ")")
                .body(body.toString());

        for (int i = start; i < end; i++) {
            PlayerEntry pe = entries.get(i);
            String label = (pe.isOnline ? "&a" : "&7") + pe.name + (pe.clan != null ? " &8[" + pe.clan.getTag() + "&8]" : "");
            dialog.button(label, p -> openPlayerProfile(p, pe, currentPage));
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

    public static void openPlayerProfile(Player viewer, PlayerEntry target, int returnPage) {
        StringBuilder body = new StringBuilder();
        body.append("&7• Jogador: &f").append(target.name).append("\n");
        body.append("&7• Status: ").append(target.isOnline ? "&aOnline" : "&cOffline").append("\n");
        body.append("&7• Clã: ").append(target.clan != null
                ? "&b" + target.clan.getName() + " &8[" + target.clan.getTag() + "&8]"
                : "&cSem Clã").append("\n");
        body.append("&7• Cargo: &f").append(target.role != null ? ClanRoles.getLabelRole(target.role) : "Nenhum").append("\n");
        body.append("&7• Abates no Clã: &a").append(target.kills).append("\n");

        MineSkyDialog.Builder dialog = MineSkyDialog.builder()
                .title("&fPerfil &8[" + target.name + "&8]")
                .body(body.toString())
                .bodyItem(getPlayerHead(target.uuid, target.name)) // Renderiza a cabeça do jogador
                .cancelButton("&7Voltar à Lista", p -> open(p, returnPage));

        dialog.show(viewer);
    }

    private static List<PlayerEntry> buildPlayerRanking() {
        Map<UUID, PlayerEntry> map = new HashMap<>();

        for (Clan clan : ClanHandler.getClans().values()) {
            for (Map.Entry<UUID, MemberData> entry : clan.getMembers().entrySet()) {
                UUID uuid = entry.getKey();
                MemberData data = entry.getValue();
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                String name = op.getName() != null ? op.getName() : "Desconhecido";

                map.put(uuid, new PlayerEntry(uuid, name, clan, data.getRole(), data.getKills(), op.isOnline()));
            }
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!map.containsKey(online.getUniqueId())) {
                map.put(online.getUniqueId(), new PlayerEntry(
                        online.getUniqueId(),
                        online.getName(),
                        null,
                        null,
                        0,
                        true
                ));
            }
        }

        List<PlayerEntry> list = new ArrayList<>(map.values());
        list.sort(Comparator
                .comparingInt(PlayerEntry::getKills).reversed()
                .thenComparing(PlayerEntry::isOnline).reversed()
        );

        return list;
    }

    private static ItemStack getPlayerHead(UUID uuid, String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            try {
                meta.setPlayerProfile(Bukkit.createProfile(uuid, name));
            } catch (Throwable t) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            }
            meta.setDisplayName(Utils.c("&e" + name));
            head.setItemMeta(meta);
        }
        return head;
    }

    public static class PlayerEntry {
        private final UUID uuid;
        private final String name;
        private final Clan clan;
        private final ClanRoles role;
        private final int kills;
        private final boolean isOnline;

        public PlayerEntry(UUID uuid, String name, Clan clan, ClanRoles role, int kills, boolean isOnline) {
            this.uuid = uuid;
            this.name = name;
            this.clan = clan;
            this.role = role;
            this.kills = kills;
            this.isOnline = isOnline;
        }

        public int getKills() { return kills; }
        public boolean isOnline() { return isOnline; }
    }

    public static void openMainMenu(Player player) {
        open(player, 1);
    }

    public static void openMainMenu(Player player, int page) {
        open(player, page);
    }
}