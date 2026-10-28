package anarchy.minesky.builders;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.Normalizer;
import java.util.UUID;

public class ClanBuilder {
    private String displayName;
    private String tag;
    private final UUID lider;

    public ClanBuilder(UUID uuid) {
        this.lider = uuid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setName(String name) {
        this.displayName = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Player getLider() {
        return Bukkit.getPlayer(lider);
    }

    public String generateId() {
        if (displayName == null || displayName.isEmpty()) return "";

        String baseId = Normalizer.normalize(displayName, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toLowerCase();

        String uniqueId = UUID.randomUUID().toString().split("-")[0];
        return baseId + "-" + uniqueId;
    }
}
