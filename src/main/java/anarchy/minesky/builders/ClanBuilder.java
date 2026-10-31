package anarchy.minesky.builders;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

public class ClanBuilder {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9]+");
    private static final Pattern TRIM_UNDERSCORES = Pattern.compile("^_+|_+$");

    private String name;
    private String tag;
    private UUID leader;

    public ClanBuilder() {}

    public ClanBuilder(UUID leader) {
        this.leader = leader;
    }

    public ClanBuilder(Player leader) {
        this(leader != null ? leader.getUniqueId() : null);
    }

    public ClanBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ClanBuilder tag(String tag) {
        this.tag = tag;
        return this;
    }

    public ClanBuilder leader(UUID leader) {
        this.leader = leader;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public UUID getLeaderId() {
        return leader;
    }

    public Player getLeader() {
        return leader != null ? Bukkit.getPlayer(leader) : null;
    }

    public Player getLider() {
        return getLeader();
    }

    public String generateId() {
        return generateId(this.name);
    }

    public static String generateId(String name) {
        if (name == null || name.isBlank()) return "";

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String withoutAccents = DIACRITICS.matcher(normalized).replaceAll("");

        String clean = NON_ALPHANUMERIC.matcher(withoutAccents).replaceAll("_");
        clean = TRIM_UNDERSCORES.matcher(clean).replaceAll("").toLowerCase();

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        return clean.isEmpty() ? uniqueSuffix : clean + "-" + uniqueSuffix;
    }
}