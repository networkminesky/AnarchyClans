package anarchy.minesky.dialogs;

import anarchy.minesky.builders.ClanBuilder;
import anarchy.minesky.config.Config;
import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.DialogResponse;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.hooks.AnarchyEconomyHook;
import anarchy.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class ClanCreateDialog {

    public static void open(Player player) {
        open(player, "", "");
    }

    public static void open(Player player, String currentName, String currentTag) {
        double price = Config.ClanPrice;

        MineSkyDialog.builder()
                .title("&fCriar Clã")
                .body("&7Digite o nome e a tag do seu clã.\n&7Custo: &a$" + price)
                .textInput("name", "Nome:", "Ex: Anarchy", currentName)
                .textInput("tag", "Tag:", "Ex: ANA", currentTag)
                .button(DialogButton.confirm("&aConfirmar", ClanCreateDialog::handleSubmit))
                .button(DialogButton.cancel("&cCancelar", p -> p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1)))
                .show(player);
    }

    private static void handleSubmit(Player player, DialogResponse response) {
        String name = response.getText("name") != null ? response.getText("name").trim() : "";
        String tag = response.getText("tag") != null ? response.getText("tag").trim() : "";

        if (name.isEmpty() || tag.isEmpty()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            FeedbackAPI.display(player, "&cDefina um nome e uma tag para o clã.", FeedbackPriority.HIGH);
            open(player, name, tag);
            return;
        }

        int tagLimit = Config.TagLimit > 0 ? Config.TagLimit : 5;
        if (!Utils.isValidTag(tag)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            FeedbackAPI.display(player, "&cA tag pode ter no máximo &f" + tagLimit + " &cletras.", FeedbackPriority.HIGH);
            open(player, name, tag);
            return;
        }

        String formattedTag = Utils.toSmallCaps(tag);

        if (ClanHandler.doesClanNameExist(name)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            FeedbackAPI.display(player, "&cJá existe um clã com o nome &f" + name + "&c.", FeedbackPriority.HIGH);
            open(player, "", tag);
            return;
        }

        if (ClanHandler.doesClanTagExist(formattedTag)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            FeedbackAPI.display(player, "&cA tag &f" + formattedTag + " &cjá está em uso.", FeedbackPriority.HIGH);
            open(player, name, "");
            return;
        }

        double price = Config.ClanPrice;

        if (price > 0) {
            AnarchyEconomyHook.withdraw(player, price, success -> {
                if (!success) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    FeedbackAPI.display(player, "&cVocê precisa de &a$" + price + " &cpara fundar um clã.", FeedbackPriority.HIGH);
                    return;
                }
                finishCreation(player, name, formattedTag);
            });
        } else {
            finishCreation(player, name, formattedTag);
        }
    }

    private static void finishCreation(Player player, String name, String formattedTag) {
        String id = ClanBuilder.generateId(name);

        ClanHandler.createClan(id, name, formattedTag, player);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        FeedbackAPI.display(player, "&aVocê fundou o clã &f" + name + "&a com sucesso!");
        Bukkit.broadcastMessage(Utils.c("&f" + player.getName() + " &7fundou o clã &f" + name + " &8[" + formattedTag + "&8]"));
    }
}