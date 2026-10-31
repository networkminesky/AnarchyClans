package anarchy.minesky.data;

import anarchy.minesky.enums.ClanRoles;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MemberData {

    private final Clan clan;
    private ClanRoles role;
    private int kills;
    private final long joinedAt;

    public MemberData(Clan clan, ClanRoles role, int kills, long joinedAt) {
        this.clan = Objects.requireNonNull(clan, "o clã não pode ser nula");
        this.role = Objects.requireNonNull(role, "O cargo não pode ser nulo");
        this.kills = Math.max(0, kills);
        this.joinedAt = joinedAt;
    }

    public MemberData(Clan clan, ClanRoles role) {
        this(clan, role, 0, System.currentTimeMillis());
    }

    public Clan getClan() {
        return clan;
    }

    public ClanRoles getRole() {
        return role;
    }

    public void setRole(ClanRoles role) {
        this.role = Objects.requireNonNull(role, "O cargo não pode ser nulo");
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public void addKill() {
        this.kills++;
    }

    public void addKills(int amount) {
        if (amount > 0) {
            this.kills += amount;
        }
    }

    public long getDaysInClan() {
        return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - joinedAt);
    }

    @Override
    public String toString() {
        return "MemberData{" +
                "guild=" + clan.getId() +
                ", role=" + role +
                ", kills=" + kills +
                ", joinedAt=" + joinedAt +
                '}';
    }
}