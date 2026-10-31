package anarchy.minesky.commands.subcommands.clans;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.handlers.requests.ReagroupHandler;
import anarchy.minesky.utils.Utils;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class ReagroupSubCommand extends SubCommand implements Listener {

    private static final Map<UUID, Location> teleportingPlayers = new ConcurrentHashMap<>();
    private static final Map<UUID, ScheduledTask> teleportTasks = new ConcurrentHashMap<>();

    public ReagroupSubCommand() {
        Bukkit.getPluginManager().registerEvents(this, AnarchyClans.getInstance());
    }

    @Override
    public String getName() {
        return "reagrupar";
    }

    @Override
    public String getDescription() {
        return "Solicita que os membros do clã se reagrupem até você ou na base";
    }

    @Override
    public String getUsage() {
        return "/clan reagrupar [base]";
    }

    @Override
    public List<String> getAliases() {
        return List.of("reagroup");
    }

    @Override
    public boolean getAdminCommand() {
        return false;
    }

    @Override
    public void perform(Player player, String[] args) {
        Clan clan = ClanHandler.getClanByPlayer(player.getUniqueId());

        if (clan == null) {
            sendError(player, "&cVocê não pertence a um clã.");
            return;
        }

        if (!ClanRoles.isLeadership(clan.getRole(player.getUniqueId()))) {
            sendError(player, "&cApenas capitães ou líderes podem solicitar o reagrupamento.");
            return;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("base")) {
            if (clan.getBase() == null) {
                sendError(player, "&cSeu clã ainda não possui uma base definida.");
                return;
            }

            ClanHandler.broadcastClanMessage(clan, "&f" + player.getName() + " &7iniciou um reagrupamento na base do clã.");
            AnarchyClans.l.info("[CLÃS] " + player.getName() + " iniciou um reagrupamento na base do clã " + clan.getName());

            for (UUID memberId : clan.getMembers().keySet()) {
                if (clan.getRole(memberId) == ClanRoles.RECRUIT) continue;

                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    teleportWithDelay(member, clan.getBase(), "&aVocê foi teleportado para a base do clã!");
                }
            }

            FeedbackAPI.display(player, "&aReagrupamento na base iniciado.");
            return;
        }

        ReagroupHandler handler = AnarchyClans.getInstance().getReagroupHandler();
        if (handler != null) {
            handler.sendReagroupRequest(player, clan);
            AnarchyClans.l.info("[CLÃS] " + player.getName() + " enviou um pedido de reagrupamento no clã " + clan.getName());
            FeedbackAPI.display(player, "&aPedido de reagrupamento enviado para os membros!");
        }
    }

    private void teleportWithDelay(Player player, Location target, String successMessage) {
        UUID uuid = player.getUniqueId();
        if (teleportingPlayers.containsKey(uuid)) {
            sendError(player, "&cVocê já está aguardando um teleporte.");
            return;
        }

        teleportingPlayers.put(uuid, player.getLocation().clone());
        FeedbackAPI.display(player, "&7Fique parado por &a5 segundos&7...");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

        AtomicInteger seconds = new AtomicInteger(5);

        ScheduledTask task = player.getScheduler().runAtFixedRate(AnarchyClans.getInstance(), scheduledTask -> {
            if (!teleportingPlayers.containsKey(uuid)) {
                scheduledTask.cancel();
                teleportTasks.remove(uuid);
                return;
            }

            if (seconds.get() <= 0) {
                teleportingPlayers.remove(uuid);
                teleportTasks.remove(uuid);
                scheduledTask.cancel();

                player.teleportAsync(target, PlayerTeleportEvent.TeleportCause.COMMAND).thenAccept(success -> {
                    if (success) {
                        FeedbackAPI.display(player, successMessage);
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    } else {
                        FeedbackAPI.display(player, "&cFalha ao realizar o teleporte.", FeedbackPriority.HIGH);
                    }
                });
                return;
            }

            player.sendActionBar(Utils.c("&7Teleportando em &a" + seconds.get() + "s&7..."));
            seconds.decrementAndGet();
        }, null, 1L, 20L);

        teleportTasks.put(uuid, task);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!teleportingPlayers.containsKey(uuid)) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            cancelTeleport(player, "&cTeleporte cancelado: você se moveu.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelTeleport(event.getPlayer(), null);
    }

    private void cancelTeleport(Player player, String reason) {
        UUID uuid = player.getUniqueId();
        teleportingPlayers.remove(uuid);
        ScheduledTask task = teleportTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        if (reason != null && player.isOnline()) {
            FeedbackAPI.display(player, reason, FeedbackPriority.HIGH);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }
}