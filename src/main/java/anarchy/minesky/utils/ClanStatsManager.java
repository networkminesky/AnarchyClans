package anarchy.minesky.utils;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.data.Clan;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ClanStatsManager {

    @FunctionalInterface
    public interface ClanStatsCallback {
        void onStatsCalculated(int totalKills, int totalDeaths, double kdr);
    }

    public static void calculateClanStats(Clan clan, ClanStatsCallback callback) {
        if (clan == null || clan.getMembers().isEmpty()) {
            callback.onStatsCalculated(0, 0, 0.0);
            return;
        }

        Set<UUID> members = Set.copyOf(clan.getMembers().keySet());
        int totalMembers = members.size();

        AtomicInteger totalKills = new AtomicInteger(0);
        AtomicInteger totalDeaths = new AtomicInteger(0);
        AtomicInteger processed = new AtomicInteger(0);

        var playerData = AnarchyClans.getInstance().getPlayerData();

        for (UUID memberId : members) {
            playerData.getKills(memberId, killsObj -> {
                int kills = parseIntSafe(killsObj);

                playerData.getDeaths(memberId, deathsObj -> {
                    int deaths = parseIntSafe(deathsObj);

                    totalKills.addAndGet(kills);
                    totalDeaths.addAndGet(deaths);

                    if (processed.incrementAndGet() >= totalMembers) {
                        int finalKills = totalKills.get();
                        int finalDeaths = totalDeaths.get();
                        callback.onStatsCalculated(finalKills, finalDeaths, calculateKdr(finalKills, finalDeaths));
                    }
                });
            });
        }
    }

    private static double calculateKdr(int kills, int deaths) {
        return (deaths <= 0) ? kills : ((double) kills / deaths);
    }

    private static int parseIntSafe(Object value) {
        if (value instanceof Number num) return num.intValue();
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }
}