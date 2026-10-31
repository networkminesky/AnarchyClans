package anarchy.minesky.hooks;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.economy.api.EconomyAPI;
import anarchy.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AnarchyEconomyHook {

    public static EconomyAPI getAPI() {
        return EconomyAPI.get();
    }

    public static boolean isAvailable() {
        return getAPI() != null;
    }

    public static CompletableFuture<Double> getBalance(UUID uuid) {
        EconomyAPI api = getAPI();
        if (api == null) return CompletableFuture.completedFuture(0.0);
        return api.getBalance(uuid);
    }

    public static CompletableFuture<Double> getBalance(Player player) {
        return getBalance(player.getUniqueId());
    }

    public static CompletableFuture<Double> getBalance(OfflinePlayer player) {
        return getBalance(player.getUniqueId());
    }

    public static CompletableFuture<Boolean> hasBalance(Player player, double amount) {
        return getBalance(player).thenApply(bal -> bal >= amount);
    }

    public static CompletableFuture<Boolean> deposit(OfflinePlayer player, double amount) {
        EconomyAPI api = getAPI();
        if (api == null || amount <= 0) return CompletableFuture.completedFuture(false);

        return api.deposit(player, amount)
                .thenApply(newBalance -> true)
                .exceptionally(err -> false);
    }

    public static CompletableFuture<Boolean> withdraw(Player player, double amount) {
        EconomyAPI api = getAPI();
        if (api == null || amount <= 0) return CompletableFuture.completedFuture(false);

        return api.getBalance(player.getUniqueId()).thenCompose(balance -> {
            if (balance < amount) {
                return CompletableFuture.completedFuture(false);
            }

            return api.withdraw(player, amount)
                    .thenApply(newBalance -> true)
                    .exceptionally(err -> false);
        });
    }

    public static void withdraw(Player player, double amount, Consumer<Boolean> callback) {
        if (amount <= 0) {
            callback.accept(true);
            return;
        }

        withdraw(player, amount).thenAccept(success -> {
            Utils.runOnPlayer(player, () -> callback.accept(success));
        });
    }

    public static void deposit(OfflinePlayer player, double amount, Consumer<Boolean> callback) {
        deposit(player, amount).thenAccept(success -> {
            Bukkit.getScheduler().runTask(AnarchyClans.getInstance(), () -> callback.accept(success));
        });
    }
}