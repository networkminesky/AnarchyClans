package anarchy.minesky.hooks;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.core.api.MineSkyAPI;
import anarchy.minesky.core.api.playerconfig.*;
import anarchy.minesky.data.Clan;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.utils.Utils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

public class ClansPlaceholder extends PlaceholderExpansion implements Relational {
    private static final String CATEGORY_KEY = "minesky_nick";
    private static final String OPTION_KEY = "anarchyclans_clan_tag";

    private final AnarchyClans plugin;
    private PlayerConfigBooleanOption clanTagOption;
    private Optional<PlayerConfigBooleanOption> showMoneyOption = Optional.empty();

    public ClansPlaceholder(AnarchyClans plugin) {
        this.plugin = plugin;

        PlayerConfigAPI config = MineSkyAPI.playerConfig();
        if (config != null) {
            PlayerConfigRegistry reg = config.registry();
            try {
                reg.registerCategory(plugin, PlayerConfigCategorySpec.builder(CATEGORY_KEY)
                        .title(Component.text("Exibição do Nick"))
                        .buttonLabel(Component.text("Visual do Nick"))
                        .iconMaterial(Material.NAME_TAG)
                        .sortOrder(40)
                        .build());
            } catch (IllegalStateException ignored) {
            }

            try {
                this.clanTagOption = reg.registerBooleanOption(plugin,
                        PlayerConfigBooleanOptionSpec.builder(OPTION_KEY, CATEGORY_KEY)
                                .label(Component.text("Exibir Tag do Clã"))
                                .defaultValue(false)
                                .build());
            } catch (IllegalStateException ignored) {
            }

            try {
                this.showMoneyOption = reg.booleanOption("anarchyeconomy_show_money");
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isTagEnabled(Player player) {
        if (player == null) return false;

        if (clanTagOption != null) {
            return clanTagOption.get(player);
        }

        try {
            PlayerConfigAPI config = MineSkyAPI.playerConfig();
            if (config != null) {
                return config.getBoolean(OPTION_KEY, player);
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private boolean isMoneyEnabled(Player player) {
        if (player == null) return false;

        if (showMoneyOption == null || showMoneyOption.isEmpty()) {
            PlayerConfigAPI config = MineSkyAPI.playerConfig();
            if (config != null) {
                this.showMoneyOption = config.registry().booleanOption("anarchyeconomy_show_money");
            }
        }

        if (showMoneyOption != null && showMoneyOption.isPresent()) {
            return showMoneyOption.get().get(player);
        }

        try {
            PlayerConfigAPI config = MineSkyAPI.playerConfig();
            if (config != null) {
                return config.getBoolean("anarchyeconomy_show_money", player);
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "anarchyclans";
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
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        Clan clan = ClanHandler.getClanByPlayer(player.getUniqueId());
        if (clan == null) return "";

        boolean showTag = isTagEnabled(player);
        return processPlaceholders(player, player, clan, identifier, showTag);
    }

    @Override
    public String onPlaceholderRequest(Player viewer, Player target, @NotNull String identifier) {
        if (viewer == null || target == null) return "";

        boolean showTag = isTagEnabled(viewer);
        if (!showTag) {
            return "";
        }

        Clan clan = ClanHandler.getClanByPlayer(target.getUniqueId());
        if (clan == null) return "";

        return processPlaceholders(viewer, target, clan, identifier, true);
    }

    private String processPlaceholders(Player viewer, Player target, Clan clan, String identifier, boolean showTag) {
        String param = identifier.toLowerCase(Locale.ROOT);

        return switch (param) {
            case "tag" -> (showTag && clan.getTag() != null) ? clan.getTag() : "";
            case "label" -> showTag ? Utils.getClanTagWithRole(target.getUniqueId()) : "";
            case "scoreboard" -> showTag ? Utils.c("&c🏹 &f") + Utils.getClanTagWithRole(target.getUniqueId()) : "";

            case "tab" -> (showTag && clan.getTag() != null)
                    ? (isMoneyEnabled(viewer) ? Utils.c(" &#35F600• &f") : "") + clan.getTag()
                    : "";

            case "name" -> clan.getName() != null ? clan.getName() : "";
            case "level" -> String.valueOf(clan.getLevel());
            case "xp" -> String.valueOf(clan.getXp());
            case "balance" -> String.valueOf(clan.getBalance());
            case "role" -> clan.getRole(target.getUniqueId()).name();
            case "kills" -> String.valueOf(clan.getKills(target.getUniqueId()));
            case "online" -> ClanHandler.getOnlineMembers(clan);

            default -> "";
        };
    }
}