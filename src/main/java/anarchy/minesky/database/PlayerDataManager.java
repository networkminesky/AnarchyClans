package anarchy.minesky.database;

import anarchy.minesky.AnarchyClans;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bson.Document;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PlayerDataManager {

    private static final String FIELD_UUID = "uuid";
    private static final String FIELD_KILLS = "kills";
    private static final String FIELD_DEATHS = "deaths";
    private static final String FIELD_BLOCKS = "blocks";
    private static final String FIELD_FIRST_NICK = "first-nickname";
    private static final String FIELD_LATEST_NICK = "latest-nickname";
    private static final String WEEKLY = "weekly";
    private static final String WEEKLY_PERIOD = "weekly.period";

    private final MongoCollection<Document> players;
    private final PlayerStatsLeaderboardCache leaderboardCache;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AnarchyClans-Players-DB");
        t.setDaemon(true);
        return t;
    });

    private ScheduledTask weeklyResetTask;
    private final ConcurrentHashMap<UUID, CachedStats> statsCache = new ConcurrentHashMap<>();

    private static final class CachedStats {
        volatile int kills;
        volatile int deaths;
        volatile int blocks;
    }

    public PlayerDataManager() {
        this.players = AnarchyClans.getInstance().mm.getPlayers();
        this.leaderboardCache = new PlayerStatsLeaderboardCache(players);
    }

    public void start(AnarchyClans plugin) {
        executor.execute(this::resetStaleWeeklyPeriods);
        scheduleWeeklyReset(plugin);
        leaderboardCache.start(plugin);
    }

    public PlayerStatsLeaderboardCache getLeaderboardCache() {
        return leaderboardCache;
    }

    void runOnDbThread(Runnable task) {
        executor.execute(task);
    }

    public void syncPlayer(UUID uuid, String nickname) {
        if (uuid == null || nickname == null || nickname.isEmpty() || players == null) {
            return;
        }
        executor.execute(() -> {
            upsertNickname(uuid, nickname);
            refreshStatsCache(uuid);
        });
    }

    public void addKill(UUID uuid, String nickname) {
        increment(uuid, nickname, FIELD_KILLS);
    }

    public void addDeath(UUID uuid, String nickname) {
        increment(uuid, nickname, FIELD_DEATHS);
    }

    public void addBlock(UUID uuid, String nickname) {
        increment(uuid, nickname, FIELD_BLOCKS);
    }

    public int getKillsSync(UUID uuid) {
        return readCached(uuid).kills;
    }

    public int getDeathsSync(UUID uuid) {
        return readCached(uuid).deaths;
    }

    public int getBlocksSync(UUID uuid) {
        return readCached(uuid).blocks;
    }

    private CachedStats readCached(UUID uuid) {
        if (uuid == null) {
            return emptyStats();
        }
        CachedStats stats = statsCache.get(uuid);
        return stats != null ? stats : emptyStats();
    }

    private static CachedStats emptyStats() {
        return new CachedStats();
    }

    public void getKills(UUID uuid, Consumer<Integer> callback) {
        if (tryCallbackFromCache(uuid, callback, FIELD_KILLS)) {
            return;
        }
        fetchStat(uuid, FIELD_KILLS, callback);
    }

    public void getDeaths(UUID uuid, Consumer<Integer> callback) {
        if (tryCallbackFromCache(uuid, callback, FIELD_DEATHS)) {
            return;
        }
        fetchStat(uuid, FIELD_DEATHS, callback);
    }

    public void getBlocks(UUID uuid, Consumer<Integer> callback) {
        if (tryCallbackFromCache(uuid, callback, FIELD_BLOCKS)) {
            return;
        }
        fetchStat(uuid, FIELD_BLOCKS, callback);
    }

    private boolean tryCallbackFromCache(UUID uuid, Consumer<Integer> callback, String field) {
        if (callback == null || uuid == null) {
            return false;
        }
        CachedStats stats = statsCache.get(uuid);
        if (stats == null) {
            return false;
        }
        callback.accept(switch (field) {
            case FIELD_KILLS -> stats.kills;
            case FIELD_DEATHS -> stats.deaths;
            case FIELD_BLOCKS -> stats.blocks;
            default -> 0;
        });
        return true;
    }

    public void getSpy(UUID uuid, Consumer<Boolean> callback) {
        if (callback != null) {
            callback.accept(true);
        }
    }

    public boolean getSpy(UUID uuid) {
        return true;
    }

    public void shutdown() {
        if (weeklyResetTask != null) {
            weeklyResetTask.cancel();
            weeklyResetTask = null;
        }
        leaderboardCache.stop();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void increment(UUID uuid, String nickname, String field) {
        if (uuid == null || players == null) {
            return;
        }
        executor.execute(() -> incrementStat(uuid, nickname, field));
    }

    private void upsertNickname(UUID uuid, String nickname) {
        String id = uuid.toString();
        Document existing = players.find(Filters.eq("_id", id)).first();

        if (existing == null) {
            players.insertOne(newDocument(id, nickname, nickname, 0, 0, 0));
            return;
        }

        String firstNick = existing.getString(FIELD_FIRST_NICK);
        if (firstNick != null && !nickname.equals(firstNick)) {
            applyLatestNickname(existing, id, nickname);
        }
    }

    private void incrementStat(UUID uuid, String nickname, String field) {
        String id = uuid.toString();
        String nick = nickname != null && !nickname.isEmpty() ? nickname : "Unknown";
        Document existing = players.find(Filters.eq("_id", id)).first();

        if (existing == null) {
            int kills = FIELD_KILLS.equals(field) ? 1 : 0;
            int deaths = FIELD_DEATHS.equals(field) ? 1 : 0;
            int blocks = FIELD_BLOCKS.equals(field) ? 1 : 0;
            players.insertOne(newDocument(id, nick, nick, kills, deaths, blocks));
            putCache(id, kills, deaths, blocks);
            maybeInvalidateLeaderboard(field);
            return;
        }

        ensureWeeklyPeriod(id, existing);

        players.updateOne(
                Filters.eq("_id", id),
                Updates.combine(
                        Updates.inc(field, 1),
                        Updates.inc(WEEKLY + "." + field, 1)
                )
        );
        bumpCache(uuid, field);
        applyLatestNickname(existing, id, nick);
        maybeInvalidateLeaderboard(field);
    }

    /** Blocks atualizam o top no refresh periódico; kills/deaths disparam rebuild async. */
    private void maybeInvalidateLeaderboard(String field) {
        if (!FIELD_BLOCKS.equals(field)) {
            leaderboardCache.invalidate();
        }
    }

    private void putCache(String id, int kills, int deaths, int blocks) {
        UUID uuid = UUID.fromString(id);
        CachedStats stats = statsCache.computeIfAbsent(uuid, u -> new CachedStats());
        stats.kills = kills;
        stats.deaths = deaths;
        stats.blocks = blocks;
    }

    private void bumpCache(UUID uuid, String field) {
        CachedStats stats = statsCache.computeIfAbsent(uuid, u -> new CachedStats());
        switch (field) {
            case FIELD_KILLS -> stats.kills++;
            case FIELD_DEATHS -> stats.deaths++;
            case FIELD_BLOCKS -> stats.blocks++;
            default -> {
            }
        }
    }

    private void refreshStatsCache(UUID uuid) {
        if (players == null || uuid == null) {
            return;
        }
        Document doc = players.find(Filters.eq("_id", uuid.toString())).first();
        if (doc == null) {
            statsCache.remove(uuid);
            return;
        }
        putCache(
                uuid.toString(),
                readNumber(doc.get(FIELD_KILLS)),
                readNumber(doc.get(FIELD_DEATHS)),
                readNumber(doc.get(FIELD_BLOCKS))
        );
    }

    private static int readNumber(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private Document newDocument(String id, String firstNick, String latestNick, int kills, int deaths, int blocks) {
        String period = WeeklyPeriod.currentKey();
        return new Document("_id", id)
                .append(FIELD_UUID, id)
                .append(FIELD_KILLS, kills)
                .append(FIELD_DEATHS, deaths)
                .append(FIELD_BLOCKS, blocks)
                .append(FIELD_FIRST_NICK, firstNick)
                .append(FIELD_LATEST_NICK, latestNick)
                .append(WEEKLY, new Document("period", period)
                        .append(FIELD_KILLS, kills)
                        .append(FIELD_DEATHS, deaths)
                        .append(FIELD_BLOCKS, blocks));
    }

    private void ensureWeeklyPeriod(String id, Document existing) {
        Document weekly = existing.get(WEEKLY, Document.class);
        String current = WeeklyPeriod.currentKey();
        if (weekly != null && current.equals(weekly.getString("period"))) {
            return;
        }
        players.updateOne(
                Filters.eq("_id", id),
                Updates.combine(
                        Updates.set(WEEKLY_PERIOD, current),
                        Updates.set(WEEKLY + "." + FIELD_KILLS, 0),
                        Updates.set(WEEKLY + "." + FIELD_DEATHS, 0),
                        Updates.set(WEEKLY + "." + FIELD_BLOCKS, 0)
                )
        );
    }

    private void resetStaleWeeklyPeriods() {
        if (players == null) {
            return;
        }
        String current = WeeklyPeriod.currentKey();
        players.updateMany(
                Filters.ne(WEEKLY_PERIOD, current),
                Updates.combine(
                        Updates.set(WEEKLY_PERIOD, current),
                        Updates.set(WEEKLY + "." + FIELD_KILLS, 0),
                        Updates.set(WEEKLY + "." + FIELD_DEATHS, 0),
                        Updates.set(WEEKLY + "." + FIELD_BLOCKS, 0)
                )
        );
        leaderboardCache.invalidate();
        AnarchyClans.l.info("| Stats semanais resetadas (periodo " + current + ").");
    }

    private void scheduleWeeklyReset(AnarchyClans plugin) {
        ZoneId zone = WeeklyPeriod.zone();
        LocalDate today = LocalDate.now(zone);
        LocalDate thisSunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDateTime nextReset = thisSunday.plusWeeks(1).atStartOfDay();

        long delayMs = Math.max(
                1000L,
                java.time.Duration.between(LocalDateTime.now(zone), nextReset).toMillis()
        );
        long delayTicks = Math.max(1L, delayMs / 50L);
        long weekTicks = 20L * 60 * 60 * 24 * 7;

        weeklyResetTask = plugin.getServer().getGlobalRegionScheduler().runDelayed(
                plugin,
                task -> {
                    executor.execute(this::resetStaleWeeklyPeriods);
                    weeklyResetTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
                            plugin,
                            t -> executor.execute(this::resetStaleWeeklyPeriods),
                            weekTicks,
                            weekTicks
                    );
                },
                delayTicks
        );
    }

    private void applyLatestNickname(Document existing, String id, String nickname) {
        String firstNick = existing.getString(FIELD_FIRST_NICK);
        if (firstNick == null || nickname.equals(firstNick)) {
            return;
        }
        if (nickname.equals(existing.getString(FIELD_LATEST_NICK))) {
            return;
        }
        players.updateOne(Filters.eq("_id", id), Updates.set(FIELD_LATEST_NICK, nickname));
    }

    private void fetchStat(UUID uuid, String field, Consumer<Integer> callback) {
        if (callback == null) {
            return;
        }
        if (uuid == null || players == null) {
            callback.accept(0);
            return;
        }
        executor.execute(() -> {
            Document doc = players.find(Filters.eq("_id", uuid.toString())).first();
            int value = doc != null ? readNumber(doc.get(field)) : 0;
            if (doc != null) {
                putCache(
                        uuid.toString(),
                        readNumber(doc.get(FIELD_KILLS)),
                        readNumber(doc.get(FIELD_DEATHS)),
                        readNumber(doc.get(FIELD_BLOCKS))
                );
            }
            callback.accept(value);
        });
    }
}
