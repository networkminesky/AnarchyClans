package anarchy.minesky.hooks;

import anarchy.minesky.api.vanish.VanishAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AnarchyVanishHook {

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("AnarchyVanish");
    }

    public static boolean isPlayerVanished(Player player) {
        if (player == null) return false;
        try {
            return VanishAPI.isInvisible(player);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isPlayerVanished(UUID uuid) {
        if (uuid == null) return false;
        try {
            return VanishAPI.getAllInvisiblePlayers().contains(uuid);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
