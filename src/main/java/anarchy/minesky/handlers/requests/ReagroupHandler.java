package anarchy.minesky.handlers.requests;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.config.Config;
import anarchy.minesky.core.api.dialogapi.DialogAPI;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ReagroupHandler {

    private final AnarchyClans plugin;
    private final Map<UUID, UUID> active = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

    public ReagroupHandler(AnarchyClans plugin) {
        this.plugin = plugin;
    }

    public boolean hasRequest(UUID playerId) {
        return active.containsKey(playerId);
    }

    public UUID getRequester(UUID playerId) {
        return active.get(playerId);
    }

    public void removeRequest(UUID playerId) {
        active.remove(playerId);
        ScheduledTask task = tasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public void sendReagroupRequest(Player requester, Clan clan) {
        for (UUID memberId : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member == null || member.equals(requester)) continue;

            if (clan.getRole(memberId) == ClanRoles.RECRUIT) continue;

            sendReagroupMessage(member, requester, clan);
            active.put(memberId, requester.getUniqueId());

            AtomicInteger seconds = new AtomicInteger(0);

            ScheduledTask task = member.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
                if (!member.isOnline() || seconds.get() >= Config.InviteDuration * 60) {
                    removeRequest(memberId);
                    scheduledTask.cancel();
                    return;
                }
                seconds.incrementAndGet();
            }, null, 1L, 20L);

            tasks.put(memberId, task);
        }

        ClanHandler.broadcastClanMessage(clan, "&e" + requester.getName() + " solicitou o reagrupamento do clã.");
    }

    private void sendReagroupMessage(Player member, Player requester, Clan clan) {
        member.getScheduler().run(plugin, scheduledTask -> {
            String text = "&7O líder &f" + requester.getName() + " &7iniciou um chamado de reagrupamento para o clã.";

            member.playSound(member.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

            if (isBedrock(member)) {
                MineSkyDialog.builder()
                        .title("&fReagrupamento do Clã")
                        .body(text + "\n\n&7Deseja se teleportar até a localização dele agora?")
                        .button("&aTeleportar", p -> accept(p))
                        .cancelButton("&cRecusar", p -> reject(p))
                        .show(member);
                return;
            }

            member.sendMessage(Utils.c("&e📍 " + text));

            Component accept = createOption("&a[Teleportar]", "&aClique para se teleportar até " + requester.getName(), "/clan aceitar");
            Component reject = createOption("&c[Recusar]", "&cClique para recusar o teleporte", "/clan rejeitar");

            Component options = LegacyComponentSerializer.legacySection().deserialize(Utils.c("&7Ação: "))
                    .append(accept)
                    .append(Component.text(" "))
                    .append(reject);

            member.sendMessage(options);

        }, null);
    }

    private Component createOption(String text, String hoverText, String command) {
        return LegacyComponentSerializer.legacySection().deserialize(Utils.c(text))
                .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(Utils.c(hoverText))))
                .clickEvent(ClickEvent.runCommand(command));
    }

    public void accept(Player member) {
        if (!hasRequest(member.getUniqueId())) {
            FeedbackAPI.display(member, "&cNenhum pedido de reagrupamento ativo.", FeedbackPriority.HIGH);
            return;
        }

        UUID requesterId = getRequester(member.getUniqueId());
        Player requester = Bukkit.getPlayer(requesterId);

        if (requester == null || !requester.isOnline()) {
            FeedbackAPI.display(member, "&cO líder do chamado está offline.", FeedbackPriority.HIGH);
            removeRequest(member.getUniqueId());
            return;
        }

        member.teleportAsync(requester.getLocation()).thenAccept(success -> {
            if (success) {
                FeedbackAPI.display(member, "&aTeleportado até &f" + requester.getName() + "&a!");
                FeedbackAPI.display(requester, "&f" + member.getName() + " &aaceitou o reagrupamento.");
            } else {
                FeedbackAPI.display(member, "&cFalha ao teleportar.", FeedbackPriority.HIGH);
            }
            removeRequest(member.getUniqueId());
        });
    }

    public void reject(Player member) {
        if (!hasRequest(member.getUniqueId())) {
            FeedbackAPI.display(member, "&cNenhum pedido de reagrupamento ativo.", FeedbackPriority.HIGH);
            return;
        }

        UUID requesterId = getRequester(member.getUniqueId());
        Player requester = Bukkit.getPlayer(requesterId);

        FeedbackAPI.display(member, "&cVocê recusou o reagrupamento.");
        if (requester != null && requester.isOnline()) {
            FeedbackAPI.display(requester, "&f" + member.getName() + " &crecusou o reagrupamento.");
        }

        removeRequest(member.getUniqueId());
    }

    private boolean isBedrock(Player player) {
        DialogAPI api = DialogAPI.get();
        if (api != null) {
            return api.isBedrock(player);
        }

        return player.getName().startsWith(".") || player.getName().startsWith("*");
    }
}