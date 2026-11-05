package anarchy.minesky.listeners;

import anarchy.minesky.AnarchyClans;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerStatsListener implements Listener {

    private final AnarchyClans plugin;

    public PlayerStatsListener(AnarchyClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerData().syncPlayer(player.getUniqueId(), player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID victimId = victim.getUniqueId();
        String victimName = victim.getName();

        plugin.getPlayerData().addDeath(victimId, victimName);

        Player killer = victim.getKiller();
        if (killer != null && !killer.getUniqueId().equals(victimId)) {
            plugin.getPlayerData().addKill(killer.getUniqueId(), killer.getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        plugin.getPlayerData().addBlock(player.getUniqueId(), player.getName());
    }

    public static void syncOnlinePlayers(AnarchyClans plugin) {
        var playerData = plugin.getPlayerData();
        if (playerData == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerData.syncPlayer(player.getUniqueId(), player.getName());
        }
    }
}
