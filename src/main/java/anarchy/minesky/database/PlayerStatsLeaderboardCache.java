package anarchy.minesky.database;

import anarchy.minesky.AnarchyClans;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bson.Document;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerStatsLeaderboardCache {

    public enum Board {
        WEEKLY_KILLS("weekly.kills"),
        WEEKLY_DEATHS("weekly.deaths"),
        WEEKLY_BLOCKS("weekly.blocks"),
        ALLTIME_KILLS("kills"),
        ALLTIME_DEATHS("deaths"),
        ALLTIME_BLOCKS("blocks");

        private final String sortField;

        Board(String sortField) {
            this.sortField = sortField;
        }
    }

    public record Entry(String nickname, int value) {
    }

    private static final int TOP_SIZE = 10;
    private static final long REFRESH_TICKS = 20L * 30;

    private static final Pattern PLACEHOLDER = Pattern.compile(
            "^(?:(weekly)_(kills|deaths|blocks)|(kills|deaths|blocks))_(\\d{1,2})(?:_(name|value))?$"
    );

    private final MongoCollection<Document> players;
    private final AtomicReference<Map<Board, List<Entry>>> cache =
            new AtomicReference<>(Map.of());
    private ScheduledTask refreshTask;

    public PlayerStatsLeaderboardCache(MongoCollection<Document> players) {
        this.players = players;
    }

    public void start(AnarchyClans plugin) {
        refreshAsync();
        refreshTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                task -> refreshAsync(),
                REFRESH_TICKS,
                REFRESH_TICKS
        );
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    public void invalidate() {
        refreshAsync();
    }

    public String resolvePlaceholder(String param) {
        if (param == null || param.isEmpty()) {
            return null;
        }
        Matcher m = PLACEHOLDER.matcher(param.toLowerCase(Locale.ROOT));
        if (!m.matches()) {
            return null;
        }

        Board board = resolveBoard(m.group(1), m.group(2), m.group(3));
        if (board == null) {
            return null;
        }

        int rank;
        try {
            rank = Integer.parseInt(m.group(4));
        } catch (NumberFormatException e) {
            return null;
        }
        if (rank < 1 || rank > TOP_SIZE) {
            return "";
        }

        List<Entry> entries = cache.get().getOrDefault(board, List.of());
        if (rank > entries.size()) {
            return "";
        }

        Entry entry = entries.get(rank - 1);
        String part = m.group(5);
        if ("value".equals(part)) {
            return String.valueOf(entry.value());
        }
        if ("name".equals(part) || part == null) {
            return entry.nickname();
        }
        return entry.nickname();
    }

    private static Board resolveBoard(String weeklyFlag, String weeklyStat, String alltimeStat) {
        if (weeklyFlag != null) {
            return switch (weeklyStat) {
                case "kills" -> Board.WEEKLY_KILLS;
                case "deaths" -> Board.WEEKLY_DEATHS;
                case "blocks" -> Board.WEEKLY_BLOCKS;
                default -> null;
            };
        }
        return switch (alltimeStat) {
            case "kills" -> Board.ALLTIME_KILLS;
            case "deaths" -> Board.ALLTIME_DEATHS;
            case "blocks" -> Board.ALLTIME_BLOCKS;
            default -> null;
        };
    }

    private void refreshAsync() {
        if (players == null) {
            return;
        }
        AnarchyClans.getInstance().getPlayerData().runOnDbThread(() -> {
            Map<Board, List<Entry>> next = new EnumMap<>(Board.class);
            String period = WeeklyPeriod.currentKey();

            next.put(Board.ALLTIME_KILLS, fetchTop("kills", null));
            next.put(Board.ALLTIME_DEATHS, fetchTop("deaths", null));
            next.put(Board.ALLTIME_BLOCKS, fetchTop("blocks", null));
            next.put(Board.WEEKLY_KILLS, fetchTop("weekly.kills", period));
            next.put(Board.WEEKLY_DEATHS, fetchTop("weekly.deaths", period));
            next.put(Board.WEEKLY_BLOCKS, fetchTop("weekly.blocks", period));

            cache.set(Map.copyOf(next));
        });
    }

    private List<Entry> fetchTop(String sortField, String weeklyPeriod) {
        var find = weeklyPeriod != null
                ? players.find(Filters.eq("weekly.period", weeklyPeriod))
                : players.find();

        String valueKey = sortField.contains(".") ? sortField.split("\\.", 2)[1] : sortField;

        List<Entry> result = new ArrayList<>(TOP_SIZE);
        for (Document doc : find
                .projection(Projections.include(sortField, "latest-nickname", "first-nickname"))
                .sort(Sorts.descending(sortField))
                .limit(TOP_SIZE)) {

            int value = readNestedInt(doc, sortField, valueKey);
            if (value <= 0) {
                continue;
            }
            result.add(new Entry(resolveNickname(doc), value));
        }
        return List.copyOf(result);
    }

    private static int readNestedInt(Document doc, String path, String leaf) {
        if (path.contains(".")) {
            Document nested = doc.get("weekly", Document.class);
            return nested != null ? nested.getInteger(leaf, 0) : 0;
        }
        return doc.getInteger(leaf, 0);
    }

    private static String resolveNickname(Document doc) {
        String latest = doc.getString("latest-nickname");
        if (latest != null && !latest.isEmpty()) {
            return latest;
        }
        String first = doc.getString("first-nickname");
        return first != null ? first : "Unknown";
    }
}
