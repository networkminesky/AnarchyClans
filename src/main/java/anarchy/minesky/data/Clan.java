package anarchy.minesky.data;

import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.utils.Utils;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Clan {
    public static final int MAX_LEVEL = 6;
    private static final long THREE_DAYS_MILLIS = TimeUnit.DAYS.toMillis(3);

    private static final double[] XP_REQUIREMENTS = {
            2500.0, 7500.0, 17500.0, 37500.0, 75000.0, 150000.0
    };

    private final String id;
    private final String name;
    private String tag;
    private String description;
    private UUID leader;
    private int level;
    private double xp;
    private int balance;
    private Location base;
    private ItemStack banner;

    private final Set<String> rivals = new HashSet<>();
    private final Set<String> allies = new HashSet<>();
    private final Map<UUID, MemberData> members = new HashMap<>();

    public Clan(String id, String name, String tag, UUID leader, int level, double xp, int balance) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.level = level;
        this.xp = xp;
        this.balance = balance;

        this.members.put(leader, new MemberData(this, ClanRoles.LEADER));
    }

    public void addMember(UUID player, ClanRoles role, int kills, long joinedAt) {
        members.put(player, new MemberData(this, role, kills, joinedAt));
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public MemberData getMemberData(UUID uuid) {
        return members.get(uuid);
    }

    public ClanRoles getRole(UUID uuid) {
        MemberData member = members.get(uuid);
        return member != null ? member.getRole() : ClanRoles.RECRUIT;
    }

    public int getKills(UUID uuid) {
        MemberData member = members.get(uuid);
        return member != null ? member.getKills() : 0;
    }

    public int getMemberLimit() {
        return Math.max(1, level * 4);
    }

    public Map<UUID, MemberData> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public void addRival(String clanId) {
        rivals.add(clanId);
    }

    public void removeRival(String clanId) {
        rivals.remove(clanId);
    }

    public void removeRival(Clan clan) {
        removeRival(clan.getId());
    }

    public boolean isRival(Clan clan) {
        return rivals.contains(clan.getId());
    }

    public Set<String> getRivals() {
        return Collections.unmodifiableSet(rivals);
    }

    public void addAlly(String clanId) {
        allies.add(clanId);
    }

    public void removeAlly(String clanId) {
        allies.remove(clanId);
    }

    public void removeAlly(Clan clan) {
        removeAlly(clan.getId());
    }

    public boolean isAlly(Clan clan) {
        return allies.contains(clan.getId());
    }

    public Set<String> getAllies() {
        return Collections.unmodifiableSet(allies);
    }

    public void addXP(double amount) {
        if (amount <= 0 || level >= MAX_LEVEL) return;
        this.xp += amount;
        checkLevelUp();
    }

    public void removeXP(double amount) {
        if (amount <= 0) return;
        this.xp -= amount;
        checkLevelDown();
    }

    private void checkLevelUp() {
        while (level < MAX_LEVEL && this.xp >= xpRequiredForNextLevel()) {
            this.xp -= xpRequiredForNextLevel();
            this.level++;
            ClanHandler.broadcastClanMessage(this, "&b\uD83D\uDC09 &3Nível &b&l" + level + "&3 conquistado&b!");
        }

        if (level >= MAX_LEVEL) {
            this.xp = 0;
        }
    }

    private void checkLevelDown() {
        while (this.xp < 0 && this.level > 0) {
            this.level--;
            this.xp += xpRequiredForNextLevel();
            ClanHandler.broadcastClanMessage(this,
                    Utils.c("&c\uD83D\uDC09 &4O clã perdeu XP demais e caiu para o Nível &c&l" + level + "&4!"));
        }

        if (this.level == 0 && this.xp < 0) {
            this.xp = 0;
        }
    }

    public double xpRequiredForNextLevel() {
        if (level < 0 || level >= XP_REQUIREMENTS.length) return 0;
        return XP_REQUIREMENTS[level];
    }

    public void deposit(int amount) {
        if (amount > 0) this.balance += amount;
    }

    public boolean withdraw(int amount) {
        if (amount > 0 && this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public double getXp() { return xp; }
    public void setXP(double xp) { this.xp = xp; }
    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }
    public Location getBase() { return base; }
    public void setBase(Location base) { this.base = base; }
    public ItemStack getBanner() { return banner; }
    public void setBanner(ItemStack banner) { this.banner = banner; }
}