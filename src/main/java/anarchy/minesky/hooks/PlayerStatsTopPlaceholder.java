package anarchy.minesky.hooks;

import anarchy.minesky.AnarchyClans;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Leaderboards: {@code %anarchyclans_top_weekly_kills_1_name%}, {@code %anarchyclans_top_kills_1_value%}, etc.
 */
public class PlayerStatsTopPlaceholder extends PlaceholderExpansion {

    private final AnarchyClans plugin;

    public PlayerStatsTopPlaceholder(AnarchyClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "anarchyclans_top";
    }

    @Override
    public @NotNull String getAuthor() {
        return "zBrunoC (Bruno C.)";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        var playerData = plugin.getPlayerData();
        if (playerData == null) {
            return "";
        }
        String result = playerData.getLeaderboardCache().resolvePlaceholder(params.toLowerCase(Locale.ROOT));
        return result != null ? result : "";
    }
}
