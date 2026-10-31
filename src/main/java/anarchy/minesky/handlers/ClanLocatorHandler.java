package anarchy.minesky.handlers;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.core.api.locator.LocatorAPI;
import anarchy.minesky.data.Clan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ClanLocatorHandler {

    private final AnarchyClans plugin;

    public ClanLocatorHandler(AnarchyClans plugin) {
        this.plugin = plugin;
    }

    private LocatorAPI getAPI() {
        return LocatorAPI.get();
    }

    public void updateVisibility(Player p1, Player p2) {
        LocatorAPI api = getAPI();
        if (api == null || p1 == null || p2 == null || !p1.isOnline() || !p2.isOnline()) {
            return;
        }

        Clan c1 = ClanHandler.getClanByPlayer(p1.getUniqueId());
        Clan c2 = ClanHandler.getClanByPlayer(p2.getUniqueId());

        updateVisibility(p1, c1, p2, c2, api);
    }

    private void updateVisibility(Player p1, Clan c1, Player p2, Clan c2, LocatorAPI api) {
        boolean shouldShow = c1 != null && c2 != null && (
                c1.getId().equals(c2.getId()) ||
                        c1.isAlly(c2)
        );

        if (shouldShow) {
            showMutual(api, p1, p2);
        } else {
            hideMutual(api, p1, p2);
        }
    }

    private void showMutual(LocatorAPI api, Player p1, Player p2) {
        api.resetPlayerVisibility(p1, p2);
        api.resetPlayerVisibility(p2, p1);
        api.showPlayer(p1, p2);
        api.showPlayer(p2, p1);
    }

    private void hideMutual(LocatorAPI api, Player p1, Player p2) {
        api.hidePlayer(p1, p2);
        api.hidePlayer(p2, p1);
    }

//    public boolean areEnemiesInWar(Clan c1, Clan c2) {
//        if (c1 == null || c2 == null || plugin.getWarHandler() == null) {
//            return false;
//        }
//
//        Optional<WarSession> sessionOpt = plugin.getWarHandler().getActiveWarByClan(c1.getId());
//        if (sessionOpt.isEmpty()) {
//            return false;
//        }
//
//        WarSession session = sessionOpt.get();
//        int side1 = getWarSide(session, c1.getId());
//        int side2 = getWarSide(session, c2.getId());
//
//        // São inimigos se ambos participam da guerra, mas em lados opostos
//        return side1 != 0 && side2 != 0 && side1 != side2;
//    }

//    private int getWarSide(WarSession session, String clanId) {
//        // Lado 1 (Clã 1 ou apoiadores)
//        String id1 = session.getClan1() != null ? session.getClan1().getId() : null;
//        if (clanId.equals(id1) || session.getClan1Supporters().contains(clanId)) {
//            return 1;
//        }
//
//        // Lado 2 (Clã 2 ou apoiadores)
//        String id2 = session.getClan2() != null ? session.getClan2().getId() : null;
//        if (clanId.equals(id2) || session.getClan2Supporters().contains(clanId)) {
//            return 2;
//        }
//
//        return 0; // Não está na guerra
//    }

    public void updatePlayer(Player player) {
        LocatorAPI api = getAPI();
        if (api == null || player == null || !player.isOnline()) return;

        Clan clan = ClanHandler.getClanByPlayer(player.getUniqueId());
        if (clan != null) {
            api.addPlayerToGroup("clan_" + clan.getId(), player);
        } else {
            api.removePlayerFromGroup(player);
            api.clearPlayerOverrides(player);
        }

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId()) && other.isOnline()) {
                updateVisibility(player, other);
            }
        }
    }

//    public void updateWarSession(WarSession session) {
//        if (session == null) return;
//
//        Set<String> involvedClans = new HashSet<>();
//        if (session.getClan1() != null) involvedClans.add(session.getClan1().getId());
//        if (session.getClan2() != null) involvedClans.add(session.getClan2().getId());
//        involvedClans.addAll(session.getClan1Supporters());
//        involvedClans.addAll(session.getClan2Supporters());
//
//        for (String clanId : involvedClans) {
//            Clan c = ClanHandler.getClanByID(clanId);
//            if (c != null) {
//                updateClan(c);
//            }
//        }
//    }

    public void updateBetweenClans(Clan c1, Clan c2) {
        LocatorAPI api = getAPI();
        if (api == null || c1 == null || c2 == null) return;

        for (UUID u1 : c1.getMembers().keySet()) {
            Player p1 = Bukkit.getPlayer(u1);
            if (p1 == null || !p1.isOnline()) continue;

            for (UUID u2 : c2.getMembers().keySet()) {
                Player p2 = Bukkit.getPlayer(u2);
                if (p2 == null || !p2.isOnline()) continue;

                updateVisibility(p1, c1, p2, c2, api);
            }
        }
    }

    public void updateClan(Clan clan) {
        if (clan == null) return;
        for (UUID uuid : clan.getMembers().keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                updatePlayer(player);
            }
        }
    }

    public void onPlayerLeaveClan(Player player) {
        LocatorAPI api = getAPI();
        if (api == null || player == null) return;

        api.removePlayerFromGroup(player);
        api.clearPlayerOverrides(player);

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId())) {
                hideMutual(api, player, other);
            }
        }
    }

    public void onClanDeleted(Clan clan) {
        LocatorAPI api = getAPI();
        if (api == null || clan == null) return;

        api.deleteGroup("clan_" + clan.getId());

        for (UUID uuid : clan.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                onPlayerLeaveClan(p);
            }
        }
    }

    public void onPlayerQuit(Player player) {
        LocatorAPI api = getAPI();
        if (api == null || player == null) return;

        api.removePlayerFromGroup(player);
        api.clearPlayerOverrides(player);
    }

    public void syncAllOnlinePlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            updatePlayer(p);
        }
    }
}