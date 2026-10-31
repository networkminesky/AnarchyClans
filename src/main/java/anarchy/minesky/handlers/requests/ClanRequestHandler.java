package anarchy.minesky.handlers.requests;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.config.Config;
import anarchy.minesky.core.api.dialogapi.DialogAPI;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.utils.Utils;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ClanRequestHandler {

    private final AnarchyClans plugin;
    private final ClanRequestType type;
    private final Map<String, Player> requesters = new ConcurrentHashMap<>();
    private final Map<String, Clan> pendingRequests = new ConcurrentHashMap<>();
    private final Map<String, ScheduledTask> tasks = new ConcurrentHashMap<>();

    public ClanRequestHandler(AnarchyClans plugin, ClanRequestType type) {
        this.plugin = plugin;
        this.type = type;
    }

    public boolean hasRequest(Clan target) {
        return target != null && pendingRequests.containsKey(target.getId());
    }

    public Clan getRequestClan(Clan target) {
        return target != null ? pendingRequests.get(target.getId()) : null;
    }

    public Player getRequester(Clan target) {
        return target != null ? requesters.get(target.getId()) : null;
    }

    public void removeRequest(Clan target) {
        if (target == null) return;
        String id = target.getId();
        pendingRequests.remove(id);
        requesters.remove(id);
        ScheduledTask task = tasks.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    public void sendRequest(Clan requester, Clan target, Player sender) {
        if (target == null || requester == null) return;

        String id = target.getId();
        pendingRequests.put(id, requester);

        if (sender != null) {
            requesters.put(id, sender);
        } else {
            requesters.remove(id);
        }

        AtomicInteger count = new AtomicInteger(0);

        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (count.getAndIncrement() >= Config.InviteDuration || !notifyTargetClan(target, requester)) {
                removeRequest(target);
                ClanHandler.broadcastClanMessage(requester,
                        "&cO pedido de " + getTypeName() + " enviado para &f" + target.getName() + " &cexpirou.");
                scheduledTask.cancel();
            }
        }, 1L, 20L * 60);

        ScheduledTask old = tasks.put(id, task);
        if (old != null) old.cancel();
    }

    private boolean notifyTargetClan(Clan target, Clan requester) {
        boolean notified = false;

        for (UUID uuid : target.getMembers().keySet()) {
            Player targetPlayer = Bukkit.getPlayer(uuid);
            if (targetPlayer == null || !targetPlayer.isOnline()) continue;

            if (!EnumSet.of(ClanRoles.LEADER, ClanRoles.SUB_LEADER).contains(target.getRole(uuid))) continue;

            notifyTargetPlayer(targetPlayer, target, requester);
            notified = true;
        }

        return notified;
    }

    private void notifyTargetPlayer(Player player, Clan target, Clan requester) {
        String cleanTag = Utils.getTag(requester.getTag());
        String acceptCmd = "/clan aceitar";
        String rejectCmd = "/clan rejeitar";
        String actionName = type == ClanRequestType.ALLY ? "aliança" : "paz";
        String text = "&7O clã &f" + requester.getName() + " &8[" + cleanTag + "&8] &7enviou um pedido de &f" + actionName + "&7.";

        player.getScheduler().run(plugin, task -> {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            if (isBedrock(player)) {
                MineSkyDialog.builder()
                        .title("&fPedido de " + actionName)
                        .body(text + "\n\n&7Escolha uma opção:")
                        .button("&aAceitar", p -> p.performCommand("clan aceitar"))
                        .cancelButton("&cRecusar", p -> p.performCommand("clan rejeitar"))
                        .show(player);
                return;
            }

            player.sendMessage(Utils.c("&b📩 " + text));
            player.sendMessage(buildOptions(acceptCmd, rejectCmd));

        }, null);
    }

    private Component buildOptions(String acceptCmd, String rejectCmd) {
        Component accept = createOption("&a[Aceitar]", "&aClique para aceitar", acceptCmd);
        Component reject = createOption("&c[Recusar]", "&cClique para recusar", rejectCmd);

        return LegacyComponentSerializer.legacySection().deserialize(Utils.c("&7Resposta: "))
                .append(accept)
                .append(Component.text(" "))
                .append(reject);
    }

    private Component createOption(String text, String hover, String command) {
        return LegacyComponentSerializer.legacySection().deserialize(Utils.c(text))
                .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(Utils.c(hover))))
                .clickEvent(ClickEvent.runCommand(command));
    }

    private String getTypeName() {
        return type == ClanRequestType.ALLY ? "aliança" : "paz";
    }

    public void handleLogin(Player player, Clan clan) {
        if (clan == null || !EnumSet.of(ClanRoles.LEADER, ClanRoles.SUB_LEADER).contains(clan.getRole(player.getUniqueId()))) {
            return;
        }

        String id = clan.getId();
        if (pendingRequests.containsKey(id)) {
            Clan requester = pendingRequests.get(id);
            notifyTargetPlayer(player, clan, requester);
        }
    }

    private boolean isBedrock(Player player) {
        DialogAPI api = DialogAPI.get();
        if (api != null) {
            return api.isBedrock(player);
        }

        return player.getName().startsWith(".") || player.getName().startsWith("*");
    }
}