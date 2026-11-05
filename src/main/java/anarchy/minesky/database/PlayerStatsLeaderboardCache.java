package anarchy.minesky.database;

import anarchy.minesky.AnarchyClans;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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

        String statKey() {
            return sortField.contains(".") ? sortField.split("\\.", 2)[1] : sortField;
        }
    }

    public record Entry(String nickname, int value) {
    }

    private record TopQuery(Board board, int rank, boolean value) {
    }

    private static final int TOP_SIZE = 10;
    private static final long REFRESH_TICKS = 20L * 30;

    private final MongoCollection<Document> players;
    private final AtomicReference<Map<Board, List<Entry>>> cache =
            new AtomicReference<>(Map.of());
    private ScheduledTask refreshTask;
    private volatile boolean refreshScheduled;

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

    /**
     * Só lê cache em memória (atualizado async pelo Mongo). Param: {@code kills_1_name}, etc.
     */
    public String resolvePlaceholder(String param) {
        TopQuery query = parse(param);
        if (query == null) {
            return null;
        }
        if (query.rank() < 1 || query.rank() > TOP_SIZE) {
            return "";
        }

        List<Entry> entries = cache.get().getOrDefault(query.board(), List.of());
        if (query.rank() > entries.size()) {
            return "";
        }

        Entry entry = entries.get(query.rank() - 1);
        return query.value() ? String.valueOf(entry.value()) : entry.nickname();
    }

    private static TopQuery parse(String param) {
        if (param == null || param.isEmpty()) {
            return null;
        }
        String lower = param.toLowerCase(Locale.ROOT);
        boolean weekly = lower.startsWith("weekly_");
        String rest = weekly ? lower.substring(7) : lower;

        String stat;
        String rankPart;
        String suffix;

        if (rest.startsWith("kills_")) {
            stat = "kills";
            rest = rest.substring(6);
        } else if (rest.startsWith("deaths_")) {
            stat = "deaths";
            rest = rest.substring(7);
        } else if (rest.startsWith("blocks_")) {
            stat = "blocks";
            rest = rest.substring(7);
        } else {
            return null;
        }

        int underscore = rest.indexOf('_');
        if (underscore < 0) {
            rankPart = rest;
            suffix = null;
        } else {
            rankPart = rest.substring(0, underscore);
            suffix = rest.substring(underscore + 1);
        }

        int rank;
        try {
            rank = Integer.parseInt(rankPart);
        } catch (NumberFormatException e) {
            return null;
        }

        boolean wantValue = "value".equals(suffix);
        Board board = resolveBoard(weekly, stat);
        if (board == null) {
            return null;
        }
        return new TopQuery(board, rank, wantValue);
    }

    private static Board resolveBoard(boolean weekly, String stat) {
        if (weekly) {
            return switch (stat) {
                case "kills" -> Board.WEEKLY_KILLS;
                case "deaths" -> Board.WEEKLY_DEATHS;
                case "blocks" -> Board.WEEKLY_BLOCKS;
                default -> null;
            };
        }
        return switch (stat) {
            case "kills" -> Board.ALLTIME_KILLS;
            case "deaths" -> Board.ALLTIME_DEATHS;
            case "blocks" -> Board.ALLTIME_BLOCKS;
            default -> null;
        };
    }

    private void refreshAsync() {
        if (players == null || refreshScheduled) {
            return;
        }
        var plugin = AnarchyClans.getInstance();
        if (plugin == null || plugin.getPlayerData() == null) {
            return;
        }
        refreshScheduled = true;
        plugin.getPlayerData().runOnDbThread(this::rebuildCache);
    }

    private void rebuildCache() {
        try {
            Map<Board, List<Entry>> next = new EnumMap<>(Board.class);
            for (Board board : Board.values()) {
                next.put(board, fetchTop(board));
            }
            cache.set(Map.copyOf(next));
        } catch (Exception e) {
            AnarchyClans.l.warning("| Falha ao atualizar cache de tops: " + e.getMessage());
        } finally {
            refreshScheduled = false;
        }
    }

    private List<Entry> fetchTop(Board board) {
        String period = WeeklyPeriod.currentKey();
        Bson filter = switch (board) {
            case WEEKLY_KILLS, WEEKLY_DEATHS, WEEKLY_BLOCKS ->
                    Filters.eq("weekly.period", period);
            default -> new Document();
        };

        List<Entry> result = new ArrayList<>(TOP_SIZE);
        for (Document doc : players.find(filter)
                .projection(projectionFor(board))
                .sort(Sorts.descending(board.sortField))
                .limit(100)) {

            int value = readStat(doc, board);
            if (value <= 0) {
                continue;
            }
            result.add(new Entry(resolveNickname(doc), value));
            if (result.size() >= TOP_SIZE) {
                break;
            }
        }
        return List.copyOf(result);
    }

    /** Evita path collision (ex.: {@code weekly.kills} + {@code weekly} no mesmo projection). */
    private static Bson projectionFor(Board board) {
        if (board.sortField.contains(".")) {
            return Projections.include("weekly", "latest-nickname", "first-nickname");
        }
        return Projections.include("kills", "deaths", "blocks", "latest-nickname", "first-nickname");
    }

    private static int readStat(Document doc, Board board) {
        if (board.sortField.contains(".")) {
            Document weekly = doc.get("weekly", Document.class);
            if (weekly == null) {
                return 0;
            }
            return readNumber(weekly.get(board.statKey()));
        }
        return readNumber(doc.get(board.statKey()));
    }

    private static int readNumber(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
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
