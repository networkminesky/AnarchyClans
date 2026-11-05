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
        executor.execute(() -> upsertNickname(uuid, nickname));
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

    public void getKills(UUID uuid, Consumer<Integer> callback) {
        fetchStat(uuid, FIELD_KILLS, callback);
    }

    public void getDeaths(UUID uuid, Consumer<Integer> callback) {
        fetchStat(uuid, FIELD_DEATHS, callback);
    }

    public void getBlocks(UUID uuid, Consumer<Integer> callback) {
        fetchStat(uuid, FIELD_BLOCKS, callback);
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
        applyLatestNickname(existing, id, nick);
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
            int value = doc != null ? doc.getInteger(field, 0) : 0;
            callback.accept(value);
        });
    }
}
