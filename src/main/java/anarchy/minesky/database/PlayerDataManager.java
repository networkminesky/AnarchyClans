package anarchy.minesky.database;

import anarchy.minesky.AnarchyClans;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PlayerDataManager {

    private static final String FIELD_UUID = "uuid";
    private static final String FIELD_KILLS = "kills";
    private static final String FIELD_DEATHS = "deaths";
    private static final String FIELD_FIRST_NICK = "first-nickname";
    private static final String FIELD_LATEST_NICK = "latest-nickname";

    private final MongoCollection<Document> players;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AnarchyClans-Players-DB");
        t.setDaemon(true);
        return t;
    });

    public PlayerDataManager() {
        this.players = AnarchyClans.getInstance().mm.getPlayers();
    }

    public void syncPlayer(UUID uuid, String nickname) {
        if (uuid == null || nickname == null || nickname.isEmpty() || players == null) {
            return;
        }
        executor.execute(() -> upsertNickname(uuid, nickname));
    }

    public void addKill(UUID uuid, String nickname) {
        if (uuid == null || players == null) {
            return;
        }
        executor.execute(() -> incrementStat(uuid, nickname, FIELD_KILLS));
    }

    public void addDeath(UUID uuid, String nickname) {
        if (uuid == null || players == null) {
            return;
        }
        executor.execute(() -> incrementStat(uuid, nickname, FIELD_DEATHS));
    }

    public void getKills(UUID uuid, Consumer<Integer> callback) {
        fetchStat(uuid, FIELD_KILLS, callback);
    }

    public void getDeaths(UUID uuid, Consumer<Integer> callback) {
        fetchStat(uuid, FIELD_DEATHS, callback);
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

    private void upsertNickname(UUID uuid, String nickname) {
        String id = uuid.toString();
        Document existing = players.find(Filters.eq("_id", id)).first();

        if (existing == null) {
            players.insertOne(new Document("_id", id)
                    .append(FIELD_UUID, id)
                    .append(FIELD_KILLS, 0)
                    .append(FIELD_DEATHS, 0)
                    .append(FIELD_FIRST_NICK, nickname)
                    .append(FIELD_LATEST_NICK, nickname));
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
            players.insertOne(new Document("_id", id)
                    .append(FIELD_UUID, id)
                    .append(FIELD_KILLS, FIELD_KILLS.equals(field) ? 1 : 0)
                    .append(FIELD_DEATHS, FIELD_DEATHS.equals(field) ? 1 : 0)
                    .append(FIELD_FIRST_NICK, nick)
                    .append(FIELD_LATEST_NICK, nick));
            return;
        }

        players.updateOne(Filters.eq("_id", id), Updates.inc(field, 1));
        applyLatestNickname(existing, id, nick);
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
