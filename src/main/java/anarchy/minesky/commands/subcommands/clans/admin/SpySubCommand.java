package anarchy.minesky.commands.subcommands.clans.admin;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import org.bukkit.entity.Player;

import java.util.List;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class SpySubCommand extends SubCommand {

    @Override
    public String getName() {
        return "spy";
    }

    @Override
    public String getDescription() {
        return "Ativa ou desativa o modo espião de chats";
    }

    @Override
    public String getUsage() {
        return "/clan admin spy [ativar/desativar]";
    }

    @Override
    public List<String> getAliases() {
        return List.of("socialspy");
    }

    @Override
    public boolean getAdminCommand() {
        return true;
    }

    @Override
    public void perform(Player player, String[] args) {
        if (!player.hasPermission("anarchyclans.spy")) {
            sendError(player, "&cVocê não tem permissão.");
            return;
        }

        var playerData = AnarchyClans.getInstance().getPlayerData();
        if (playerData == null) return;

        if (args.length == 1) {
            playerData.getSpy(player.getUniqueId(), currentStatus -> setSpyStatus(player, !currentStatus));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "ativar", "on", "enable", "habilitar" -> setSpyStatus(player, true);
            case "desativar", "off", "disable", "desabilitar" -> setSpyStatus(player, false);
            default -> sendError(player, "&cOpção inválida. Use 'ativar' ou 'desativar'.");
        }
    }

    private void setSpyStatus(Player player, boolean enabled) {
        var playerData = AnarchyClans.getInstance().getPlayerData();
        if (playerData != null) {
           // playerData.setSpy(player.getUniqueId(), enabled);
        }

        String status = enabled ? "ativou" : "desativou";
        AnarchyClans.l.info("[ADMIN] " + player.getName() + " " + status + " o spy dos clãs.");

        if (enabled) {
            FeedbackAPI.display(player, "&aModo espião ativado.");
        } else {
            FeedbackAPI.display(player, "&cModo espião desativado.");
        }
    }
}