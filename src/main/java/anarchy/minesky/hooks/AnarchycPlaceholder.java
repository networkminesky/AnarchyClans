package anarchy.minesky.hooks;

import anarchy.minesky.AnarchyClans;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Stats: {@code %anarchyc_kills%}, {@code %anarchyc_deaths%}, {@code %anarchyc_blocks%}.
 * Tops: {@code %anarchyc_top_kills_1_name%} (mesma expansion — o PAPI prefixa tudo em {@code anarchyc_}).
 */
public class AnarchycPlaceholder extends PlaceholderExpansion {

    private final AnarchyClans plugin;

    public AnarchycPlaceholder(AnarchyClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "anarchyc";
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
        return resolve(params, player);
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        return resolve(params, player);
    }

    private String resolve(@NotNull String params, OfflinePlayer player) {
        String param = params.toLowerCase(Locale.ROOT);
        var playerData = plugin.getPlayerData();
        if (playerData == null) {
            return param.startsWith("top_") ? "" : "0";
        }

        if (param.startsWith("top_")) {
            String result = playerData.getLeaderboardCache().resolvePlaceholder(param.substring(4));
            return result != null ? result : "";
        }

        if (player == null) {
            return "0";
        }

        return switch (param) {
            case "kills" -> String.valueOf(playerData.getKillsSync(player.getUniqueId()));
            case "deaths" -> String.valueOf(playerData.getDeathsSync(player.getUniqueId()));
            case "blocks" -> String.valueOf(playerData.getBlocksSync(player.getUniqueId()));
            default -> "";
        };
    }
}
