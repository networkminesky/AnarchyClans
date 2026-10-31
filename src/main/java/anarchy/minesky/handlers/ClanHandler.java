package anarchy.minesky.handlers;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.utils.Utils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.*;

public class ClanHandler {
    private final AnarchyClans plugin = AnarchyClans.getInstance();
    private static MongoCollection<Document> coll;
    private static final Map<String, Clan> clans = new ConcurrentHashMap<>();
    private static final Set<String> dirtyClans = ConcurrentHashMap.newKeySet();

    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "AnarchyClans-DB-Thread");
        thread.setDaemon(true);
        return thread;
    });

    public ClanHandler() {
        coll = plugin.coll;
        loadClans();
    }

    private void loadClans() {
        for (Document doc : coll.find()) {
            if (doc.getString("_id").startsWith("cooldown_")) continue;

            Clan clan = docToClan(doc);
            if (clan != null) {
                clans.put(clan.getId(), clan);
            }
        }
    }

    private static Clan docToClan(Document doc) {
        String id = doc.getString("_id");
        Document meta = doc.get("meta", Document.class);
        if (meta == null) meta = new Document();

        String tag = meta.getString("tag");
        if (tag != null) tag = Utils.toSmallCaps(tag);

        Clan clan = new Clan(
                id,
                meta.getString("name"),
                tag,
                UUID.fromString(doc.getString("leader")),
                meta.getInteger("level", 0),
                meta.getDouble("xp") != null ? meta.getDouble("xp") : 0.0,
                meta.getInteger("balance", 0)
        );

        clan.setDescription(meta.getString("description"));
        clan.setBase(Utils.deserializeLocation(meta.getString("base")));
        if (meta.containsKey("banner-item")) {
            clan.setBanner(Utils.decodeItem(meta.getString("banner-item")));
        }

        List<Document> memberDocs = doc.getList("members", Document.class);
        if (memberDocs != null) {
            for (Document m : memberDocs) {
                clan.addMember(
                        UUID.fromString(m.getString("uuid")),
                        ClanRoles.valueOf(m.getString("role")),
                        m.getInteger("kills", 0),
                        m.getLong("joinedAt") != null ? m.getLong("joinedAt") : System.currentTimeMillis()
                );
            }
        }

        List<String> rivals = doc.getList("rivals", String.class);
        if (rivals != null) rivals.forEach(clan::addRival);

        List<String> allies = doc.getList("allies", String.class);
        if (allies != null) allies.forEach(clan::addAlly);

        return clan;
    }

    private static Document clanToDoc(Clan c) {
        List<Document> members = c.getMembers().entrySet().stream()
                .map(e -> new Document("uuid", e.getKey().toString())
                        .append("role", e.getValue().getRole().name())
                        .append("kills", e.getValue().getKills())
                        .append("joinedAt", e.getValue().getJoinedAt()))
                .toList();

        Document meta = new Document("name", c.getName())
                .append("tag", c.getTag())
                .append("description", c.getDescription())
                .append("level", c.getLevel())
                .append("xp", c.getXp())
                .append("balance", c.getBalance())
                .append("base", Utils.serializeLocation(c.getBase()))
                .append("banner-item", c.getBanner() != null ? Utils.encodeItem(c.getBanner()) : null);

        return new Document("_id", c.getId())
                .append("leader", c.getLeader().toString())
                .append("members", members)
                .append("rivals", new ArrayList<>(c.getRivals()))
                .append("allies", new ArrayList<>(c.getAllies()))
                .append("meta", meta);
    }

    public static void saveClan(Clan c) {
        if (c == null) return;
        dirtyClans.add(c.getId());
        flushDirty();
    }

    public static void saveClans() {
        dirtyClans.addAll(clans.keySet());
        flushDirty();
    }

    private static void flushDirty() {
        if (coll == null || dirtyClans.isEmpty()) return;
        DB_EXECUTOR.execute(() -> {
            Set<String> toSave = new HashSet<>(dirtyClans);
            dirtyClans.removeAll(toSave);
            performBulkWrite(toSave);
        });
    }

    public static void saveAllSync() {
        if (coll == null) return;
        dirtyClans.addAll(clans.keySet());
        Set<String> toSave = new HashSet<>(dirtyClans);
        dirtyClans.clear();
        performBulkWrite(toSave);
    }

    private static void performBulkWrite(Set<String> clanIds) {
        if (clanIds.isEmpty()) return;

        List<WriteModel<Document>> operations = clanIds.stream()
                .map(clans::get)
                .filter(Objects::nonNull)
                .map(c -> (WriteModel<Document>) new ReplaceOneModel<>(
                        new Document("_id", c.getId()),
                        clanToDoc(c),
                        new ReplaceOptions().upsert(true)
                ))
                .toList();

        if (!operations.isEmpty()) {
            try {
                coll.bulkWrite(operations, new BulkWriteOptions().ordered(false));
            } catch (Exception e) {
                AnarchyClans.l.severe("Erro ao salvar clãs no MongoDB: " + e.getMessage());
            }
        }
    }

    public static boolean doesClanExist(String id) {
        return clans.containsKey(id);
    }

    public static boolean doesClanNameExist(String name) {
        return clans.values().stream().anyMatch(c -> name.equalsIgnoreCase(c.getName()));
    }

    public static boolean doesClanTagExist(String tag) {
        return doesClanTagExist(tag, null);
    }

    public static boolean doesClanTagExist(String tag, String ignoreClanId) {
        if (coll == null || !Utils.isValidTag(tag)) return false;

        String normalized = Utils.normalizeTag(tag);
        if (normalized.isEmpty()) return false;

        return clans.values().stream()
                .filter(c -> c.getTag() != null && !c.getId().equals(ignoreClanId))
                .anyMatch(c -> Utils.normalizeTag(c.getTag()).equalsIgnoreCase(normalized));
    }

    public static String getClanName(String id) {
        Clan c = clans.get(id);
        return c != null ? c.getName() : "";
    }

    public static String getClanTag(String id) {
        Clan c = clans.get(id);
        return c != null ? c.getTag() : "";
    }

    public static int getClanLevel(String id) {
        Clan c = clans.get(id);
        return c != null ? c.getLevel() : 0;
    }

    public static double getClanXp(String id) {
        Clan c = clans.get(id);
        return c != null ? c.getXp() : 0.0;
    }

    public static Map<String, Clan> getClans() {
        return clans;
    }

    public static Clan getClanByID(String id) {
        return clans.get(id);
    }

    public static Clan getClanByPlayer(UUID player) {
        if (player == null) return null;
        for (Clan clan : clans.values()) {
            if (clan.getMembers().containsKey(player)) return clan;
        }
        return null;
    }

    public static Clan getClanByPlayer(Player player) {
        return player != null ? getClanByPlayer(player.getUniqueId()) : null;
    }

    public static Clan getClanByTag(String tag) {
        if (tag == null || tag.isEmpty()) return null;
        String normalizedTarget = Utils.normalizeTag(tag);

        return clans.values().stream()
                .filter(c -> c.getTag() != null && Utils.normalizeTag(c.getTag()).equalsIgnoreCase(normalizedTarget))
                .findFirst()
                .orElse(null);
    }

    public static boolean hasClan(Player player) {
        return getClanByPlayer(player) != null;
    }

    public static String getOnlineMembers(Clan clan) {
        long online = clan.getMembers().keySet().stream()
                .filter(uuid -> Bukkit.getPlayer(uuid) != null)
                .count();
        return online + "/" + clan.getMembers().size();
    }

    public static List<String> getAlliesTags(Clan clan) {
        return clan.getAllies().stream()
                .map(ClanHandler::getClanByID)
                .filter(Objects::nonNull)
                .map(Clan::getTag)
                .toList();
    }

    public static List<String> getRivalsTags(Clan clan) {
        return clan.getRivals().stream()
                .map(ClanHandler::getClanByID)
                .filter(Objects::nonNull)
                .map(Clan::getTag)
                .toList();
    }

    public static void createClan(String id, String name, String tag, Player lider) {
        Clan clan = new Clan(id, name, tag, lider.getUniqueId(), 0, 0, 0);
        clans.put(id, clan);
        saveClan(clan);

        var locator = AnarchyClans.getInstance().getLocator();
        if (locator != null) locator.updatePlayer(lider);

        AnarchyClans.l.info("Criando clã " + name + " (" + id + ")...");
    }

    public static void deleteClan(String id) {
        Clan clan = getClanByID(id);
        if (clan == null) {
            AnarchyClans.l.warning("Clã com ID '" + id + "' não encontrado.");
            return;
        }

        var locator = AnarchyClans.getInstance().getLocator();
        if (locator != null) locator.onClanDeleted(clan);

        Utils.removeClansAlliesAndRivalsOnDelete(clan);

        clans.remove(id);
        dirtyClans.remove(id);

        DB_EXECUTOR.execute(() -> {
            try {
                coll.deleteOne(new Document("_id", id));
            } catch (Exception e) {
                AnarchyClans.l.severe("Erro ao deletar clã no MongoDB: " + e.getMessage());
            }
        });

        AnarchyClans.l.info("Clã com ID '" + id + "' foi deletado com sucesso.");
    }

    public static void addAlly(Clan clan, Clan ally, Player player) {
        clan.addAlly(ally.getId());
        ally.addAlly(clan.getId());

        broadcastClanMessage(clan, "&aSeu clã selou uma aliança com &f" + ally.getName() + "&a.");
        broadcastClanMessage(ally, "&aO clã &f" + clan.getName() + " &aselou uma aliança com seu clã.");

        saveClan(clan);
        saveClan(ally);
        updateLocatorRelations(clan, ally);
    }

    public static void removeAlly(Clan clan, Clan ally, Player player) {
        clan.removeAlly(ally);
        ally.removeAlly(clan);

        broadcastClanMessage(ally, "&cA aliança com &f" + clan.getName() + " &cfoi rompida.");
        broadcastClanMessage(clan, "&f" + player.getName() + " &crompeu a aliança com &f" + ally.getName() + "&c.");

        saveClan(clan);
        saveClan(ally);
        updateLocatorRelations(clan, ally);
    }

    public static void addRival(Clan clan, Clan rival, Player player) {
        clan.addRival(rival.getId());
        rival.addRival(clan.getId());

        broadcastClanMessage(rival, "&cO clã &f" + clan.getName() + " &cdeclarou rivalidade contra seu clã.");
        broadcastClanMessage(clan, "&f" + player.getName() + " &cdeclarou rivalidade com o clã &f" + rival.getName() + "&c.");

        saveClan(clan);
        saveClan(rival);
        updateLocatorRelations(clan, rival);
    }

    public static void removeRival(Clan clan, Clan rival, Player player) {
        clan.removeRival(rival);
        rival.removeRival(clan);

        broadcastClanMessage(clan, "&aO clã &f" + rival.getName() + " &aremoveu a rivalidade com seu clã.");
        broadcastClanMessage(rival, "&f" + player.getName() + " &aremoveu a rivalidade com o clã &f" + clan.getName() + "&a.");

        saveClan(clan);
        saveClan(rival);
        updateLocatorRelations(clan, rival);
    }

    private static void updateLocatorRelations(Clan clanA, Clan clanB) {
        var locator = AnarchyClans.getInstance().getLocator();
        if (locator != null) locator.updateBetweenClans(clanA, clanB);
    }

    public static void addMember(Player player, Clan clan) {
        clan.addMember(player.getUniqueId(), ClanRoles.RECRUIT, 0, System.currentTimeMillis());
        saveClan(clan);

        var locator = AnarchyClans.getInstance().getLocator();
        if (locator != null) {
            locator.updatePlayer(player);
            locator.updateClan(clan);
        }
    }

    public static void removeMember(UUID uuid, Clan clan) {
        clan.removeMember(uuid);
        saveClan(clan);

        var locator = AnarchyClans.getInstance().getLocator();
        if (locator != null) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null) locator.onPlayerLeaveClan(target);
            locator.updateClan(clan);
        }
    }

    public static void removeMember(Player player, Clan clan) {
        removeMember(player.getUniqueId(), clan);
    }

    public static void addKill(Player player, Clan clan) {
        clan.getMemberData(player.getUniqueId()).addKill();
        saveClan(clan);
    }

    public static void getMemberPromoteKills(Player player, Clan clan) {
        int kills = clan.getKills(player.getUniqueId());
        ClanRoles currentRole = clan.getRole(player.getUniqueId());

        if (ClanRoles.isLeadershipAndRecruiter(currentRole)) return;

        if (kills >= 40 && currentRole != ClanRoles.MEMBER) {
            setRole(player, clan, ClanRoles.MEMBER);
            broadcastClanMessage(clan, "&f" + player.getName() + " &7atingiu 40 abates e foi promovido a &aMembro&7.");
        }
    }

    public static void setRole(Player player, Clan clan, ClanRoles role) {
        clan.getMemberData(player.getUniqueId()).setRole(role);
        saveClan(clan);
    }

    public static void promotePlayer(Player player, Clan clan) {
        ClanRoles current = clan.getRole(player.getUniqueId());
        ClanRoles promoted = promoteRole(current);
        if (promoted != current) setRole(player, clan, promoted);
    }

    public static void promotePlayer(Player player, Clan clan, ClanRoles role) {
        setRole(player, clan, role);
    }

    public static void demotePlayer(Player player, Clan clan) {
        ClanRoles current = clan.getRole(player.getUniqueId());
        ClanRoles demoted = demoteRole(current);
        if (demoted != current) setRole(player, clan, demoted);
    }

    public static ClanRoles promoteRole(ClanRoles current) {
        return switch (current) {
            case RECRUIT -> ClanRoles.RECRUIT;
            case MEMBER -> ClanRoles.MEMBER;
            case LOYAL -> ClanRoles.LOYAL;
            case RECRUITER -> ClanRoles.CAPTAIN;
            case CAPTAIN, SUB_LEADER -> ClanRoles.SUB_LEADER;
            case LEADER -> ClanRoles.LEADER;
        };
    }

    public static ClanRoles demoteRole(ClanRoles current) {
        return switch (current) {
            case LEADER -> ClanRoles.LEADER;
            case SUB_LEADER -> ClanRoles.CAPTAIN;
            case CAPTAIN -> ClanRoles.RECRUITER;
            case RECRUITER, LOYAL -> ClanRoles.LOYAL;
            case MEMBER -> ClanRoles.MEMBER;
            case RECRUIT -> ClanRoles.RECRUIT;
        };
    }

    public static void setBanner(ItemStack item, Clan clan) {
        clan.setBanner(item);
        saveClan(clan);
    }

    public static void addXpToClan(UUID player, double xp) {
        Clan clan = getClanByPlayer(player);
        if (clan != null) {
            clan.addXP(xp);
            saveClan(clan);
        }
    }

    public static void broadcastClanMessage(Clan clan, String message) {
        String formatted = Utils.c("&8* " + message);

        for (UUID uuid : clan.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(formatted);
            }
        }
    }

    public static void broadcastClanChat(Player player, Clan clan, String message, boolean allies) {
        Set<UUID> recipients = new HashSet<>();

        if (allies) {

            clan.getMembers().keySet().stream()
                    .filter(uuid -> clan.getRole(uuid) != ClanRoles.RECRUIT)
                    .forEach(recipients::add);

            clan.getAllies().stream()
                    .map(clans::get)
                    .filter(Objects::nonNull)
                    .forEach(ally -> ally.getMembers().keySet().stream()
                            .filter(uuid -> ally.getRole(uuid) != ClanRoles.RECRUIT)
                            .forEach(recipients::add));
        } else {
            recipients.addAll(clan.getMembers().keySet());
        }

        String name = player.getName();
        String role = clan.getRole(player.getUniqueId()).getLabel();
        String tag = clan.getTag();

        String formattedMessage;
        if (allies) {
            formattedMessage = Utils.c("&b[Aliança] &8[&b" + tag + "&8] &8[&7" + role + "&8] &f" + name + "&7: &f" + message);
        } else {
            formattedMessage = Utils.c("&a[Clã] &8[&7" + role + "&8] &f" + name + "&7: &f" + message);
        }

        String spyMessage = Utils.c("&7[SPY] " + (allies ? "&b[Aliança] &8[" + tag + "]" : "&a[Clã] &8[" + tag + "]") + " &7" + name + "&8: &f" + message);
        String consoleLog = "[SPY] " + (allies ? "[ALIANÇA]" : "[CLÃ]") + " [" + Utils.getTag(tag) + "] " + name + ": " + message;

        for (UUID uuid : recipients) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(formattedMessage);
            }
        }

        dispatchSpyAndConsole(spyMessage, consoleLog, recipients);
    }

    private static void dispatchSpyAndConsole(String spyMessage, String consoleMessage, Set<UUID> excluded) {
        AnarchyClans inst = AnarchyClans.getInstance();
        var playerData = inst.getPlayerData();

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> !excluded.contains(p.getUniqueId()) && p.hasPermission("anarchyclans.spy"))
                .forEach(p -> {
                    if (playerData != null) {
                        playerData.getSpy(p.getUniqueId(), isSpy -> {
                            if (isSpy) {
                                p.sendMessage(spyMessage);
                            }
                        });
                    }
                });

        Bukkit.getConsoleSender().sendMessage(consoleMessage);
    }

    public void close() {
        saveAllSync();

        DB_EXECUTOR.shutdown();
        try {
            if (!DB_EXECUTOR.awaitTermination(2, TimeUnit.SECONDS)) {
                DB_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            DB_EXECUTOR.shutdownNow();
        }

        if (plugin.mm != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    plugin.mm.close();
                } catch (Exception ignored) {}
            });
        }
    }
}