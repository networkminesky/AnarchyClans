package anarchy.minesky.handlers;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.config.Config;
import anarchy.minesky.core.api.dialogapi.DialogAPI;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.data.Clan;
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

public class InviteHandler {

    private final AnarchyClans plugin;
    private final Map<UUID, Clan> activeInvites = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> inviteTasks = new ConcurrentHashMap<>();

    public InviteHandler(AnarchyClans plugin) {
        this.plugin = plugin;
    }

    public boolean hasInvite(UUID playerId) {
        return playerId != null && activeInvites.containsKey(playerId);
    }

    public Clan getInviteClan(UUID playerId) {
        return playerId != null ? activeInvites.get(playerId) : null;
    }

    public Clan getInviteGuild(UUID playerId) {
        return getInviteClan(playerId);
    }

    public void removeInvite(UUID playerId) {
        if (playerId == null) return;
        activeInvites.remove(playerId);
        ScheduledTask task = inviteTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public void sendInvite(Player invited, Clan clan) {
        if (invited == null || clan == null) return;

        removeInvite(invited.getUniqueId());
        activeInvites.put(invited.getUniqueId(), clan);

        AtomicInteger minutes = new AtomicInteger(0);

        ScheduledTask task = invited.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (!invited.isOnline() || minutes.get() >= Config.InviteDuration) {
                ClanHandler.broadcastClanMessage(clan,
                        "&cO convite enviado para &f" + invited.getName() + " &cexpirou.");
                removeInvite(invited.getUniqueId());
                scheduledTask.cancel();
                return;
            }

            sendInviteNotification(invited, clan);
            minutes.incrementAndGet();
        }, null, 1L, 20L * 60);

        inviteTasks.put(invited.getUniqueId(), task);
    }

    private void sendInviteNotification(Player invited, Clan clan) {
        String cleanTag = Utils.getTag(clan.getTag());
        String text = "&7Você recebeu um convite para ingressar no clã &f" + clan.getName() + " &8[" + cleanTag + "&8]&7.";

        invited.playSound(invited.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        if (isBedrock(invited)) {
            MineSkyDialog.builder()
                    .title("&fConvite de Clã")
                    .body(text + "\n\n&7Deseja se juntar a este clã?")
                    .button("&aAceitar", p -> p.performCommand("clan aceitar"))
                    .cancelButton("&cRecusar", p -> p.performCommand("clan rejeitar"))
                    .show(invited);
            return;
        }

        invited.sendMessage(Utils.c("\n&e✉ " + text));

        Component accept = createOption("&a[Aceitar]", "&aClique para aceitar o convite", "/clan aceitar");
        Component reject = createOption("&c[Recusar]", "&cClique para recusar o convite", "/clan rejeitar");

        Component options = LegacyComponentSerializer.legacySection().deserialize(Utils.c("&7Resposta: "))
                .append(accept)
                .append(Component.text(" "))
                .append(reject)
                .append(Component.text("\n"));

        invited.sendMessage(options);
    }

    private Component createOption(String text, String hoverText, String command) {
        return LegacyComponentSerializer.legacySection().deserialize(Utils.c(text))
                .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(Utils.c(hoverText))))
                .clickEvent(ClickEvent.runCommand(command));
    }


    private boolean isBedrock(Player player) {
        DialogAPI api = DialogAPI.get();
        if (api != null) {
            return api.isBedrock(player);
        }

        return player.getName().startsWith(".") || player.getName().startsWith("*");
    }
}