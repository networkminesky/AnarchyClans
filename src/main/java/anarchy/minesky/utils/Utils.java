package anarchy.minesky.utils;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.config.Config;
import anarchy.minesky.data.Clan;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import anarchy.minesky.hooks.AnarchyVanishHook;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final ZoneId SAO_PAULO_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DecimalFormat DF_XP = new DecimalFormat("0.#");
    private static final DecimalFormat DF_KDR = new DecimalFormat("0.00");

    public static final String BLUE_COLOR = ChatColor.of("#3d85c6").toString();

    public static String c(String message) {
        return hex(message);
    }

    public static String hex(String message) {
        if (message == null || message.isEmpty()) return "";

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(result, ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(result);

        return ChatColor.translateAlternateColorCodes('&', result.toString());
    }

    public static String stripColor(String text) {
        if (text == null || text.isEmpty()) return "";
        String withoutHex = HEX_PATTERN.matcher(text).replaceAll("");
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', withoutHex));
    }

    public static boolean isValidTag(String tag) {
        if (tag == null || tag.isEmpty()) return false;
        if (tag.contains("&k") || tag.contains("§k")) return false;

        String clean = stripColor(tag);
        return clean.length() <= Config.TagLimit;
    }

    public static String getTag(String tag) {
        return stripColor(tag);
    }

    public static String normalizeTag(String tag) {
        String clean = getTag(tag);
        if (clean.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (char c : clean.toCharArray()) {
            sb.append(smallCapToNormal(c));
        }
        return sb.toString().toLowerCase();
    }

    public static String toSmallCaps(String text) {
        if (text == null || text.isEmpty()) return text;

        StringBuilder result = new StringBuilder();
        int length = text.length();
        int i = 0;

        while (i < length) {
            if (i + 7 < length && text.charAt(i) == '&' && text.charAt(i + 1) == '#') {
                String hex = text.substring(i, i + 8);
                if (hex.matches("(?i)&#[0-9a-f]{6}")) {
                    result.append(hex);
                    i += 8;
                    continue;
                }
            }

            if ((text.charAt(i) == '&' || text.charAt(i) == '§') && i + 1 < length) {
                char next = text.charAt(i + 1);
                if ("0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(next) != -1) {
                    result.append(text.charAt(i)).append(next);
                    i += 2;
                    continue;
                }
            }

            result.append(charToSmallCap(text.charAt(i)));
            i++;
        }

        return result.toString();
    }

    private static char charToSmallCap(char c) {
        return switch (Character.toLowerCase(c)) {
            case 'a' -> 'ᴀ';
            case 'b' -> 'ʙ';
            case 'c' -> 'ᴄ';
            case 'd' -> 'ᴅ';
            case 'e' -> 'ᴇ';
            case 'f' -> 'ғ';
            case 'g' -> 'ɢ';
            case 'h' -> 'ʜ';
            case 'i' -> 'ɪ';
            case 'j' -> 'ᴊ';
            case 'k' -> 'ᴋ';
            case 'l' -> 'ʟ';
            case 'm' -> 'ᴍ';
            case 'n' -> 'ɴ';
            case 'o' -> 'ᴏ';
            case 'p' -> 'ᴘ';
            case 'q' -> 'ǫ';
            case 'r' -> 'ʀ';
            case 's' -> 's';
            case 't' -> 'ᴛ';
            case 'u' -> 'ᴜ';
            case 'v' -> 'ᴠ';
            case 'w' -> 'ᴡ';
            case 'x' -> 'x';
            case 'y' -> 'ʏ';
            case 'z' -> 'ᴢ';
            default  -> c;
        };
    }

    private static char smallCapToNormal(char c) {
        return switch (c) {
            case 'ᴀ' -> 'a';
            case 'ʙ' -> 'b';
            case 'ᴄ' -> 'c';
            case 'ᴅ' -> 'd';
            case 'ᴇ' -> 'e';
            case 'ғ', 'ꜰ' -> 'f';
            case 'ɢ' -> 'g';
            case 'ʜ' -> 'h';
            case 'ɪ' -> 'i';
            case 'ᴊ' -> 'j';
            case 'ᴋ' -> 'k';
            case 'ʟ' -> 'l';
            case 'ᴍ' -> 'm';
            case 'ɴ' -> 'n';
            case 'ᴏ' -> 'o';
            case 'ᴘ' -> 'p';
            case 'ǫ', 'ϙ' -> 'q';
            case 'ʀ' -> 'r';
            case 'ꜱ' -> 's';
            case 'ᴛ' -> 't';
            case 'ᴜ' -> 'u';
            case 'ᴠ' -> 'v';
            case 'ᴡ' -> 'w';
            case 'ʏ' -> 'y';
            case 'ᴢ' -> 'z';
            default  -> Character.toLowerCase(c);
        };
    }

    public static void runOnPlayer(Player player, Runnable runnable) {
        try {
            player.getScheduler().run(AnarchyClans.getInstance(), task -> runnable.run(), null);
        } catch (Throwable t) {
            Bukkit.getScheduler().runTask(AnarchyClans.getInstance(), runnable);
        }
    }

    public static void getClanInfo(Player p, Consumer<ItemStack> callback) {
        Clan clan = ClanHandler.getClanByPlayer(p.getUniqueId());
        getClanInfoAsync(clan, callback);
    }

    public static void getClanInfoAsync(Clan clan, Consumer<ItemStack> callback) {
        ItemStack item = (clan != null && clan.getBanner() != null && !clan.getBanner().getType().isAir())
                ? clan.getBanner().clone()
                : new ItemStack(Material.GREEN_BANNER);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            item = new ItemStack(Material.GREEN_BANNER);
            meta = item.getItemMeta();
        }

        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        if (clan == null) {
            meta.setDisplayName(c("§b§lAgente Livre"));
            meta.setLore(List.of(
                    c("&7• Você não está em um clã."),
                    " ",
                    c("&bPreço: &31,500"),
                    " ",
                    c("&e➳ Clique esquerdo - Para criar um clã.")
            ));
            item.setItemMeta(meta);
            callback.accept(item);
            return;
        }

        meta.setDisplayName(c("§b§l" + clan.getName() + " &b[&f" + clan.getTag() + "&b]"));

        final ItemMeta finalMeta = meta;
        final ItemStack finalItem = item;

        ClanStatsManager.calculateClanStats(clan, (totalKills, totalDeaths, kdr) -> {
            List<String> lore = new ArrayList<>();
            lore.add(c("&7• Informações do clã."));
            lore.add(" ");
            lore.add(c("&bDescrição: &3" + (clan.getDescription() == null ? "Sem descrição" : clan.getDescription())));
            lore.add(c("&bLevel: &3" + clan.getLevel()));
            lore.add(c("&bXP: &3" + DF_XP.format(clan.getXp()) + "/" + DF_XP.format(clan.xpRequiredForNextLevel())));

            OfflinePlayer leader = Bukkit.getOfflinePlayer(clan.getLeader());
            lore.add(c("&bLíder: &3" + (leader.getName() != null ? leader.getName() : "Desconhecido")));
            lore.add(c("&bMembros: &3" + ClanHandler.getOnlineMembers(clan)));
            lore.add(c("&bKills totais: &3" + totalKills));
            lore.add(c("&bMortes totais: &3" + totalDeaths));
            lore.add(c("&bKDR médio: &3" + DF_KDR.format(kdr)));

            lore.add(c("&bRivais: &3" + (clan.getRivals().isEmpty() ? "Nenhum" : String.join("&b, &3", ClanHandler.getRivalsTags(clan)))));
            lore.add(c("&bAliados: &3" + (clan.getAllies().isEmpty() ? "Nenhum" : String.join("&b, &3", ClanHandler.getAlliesTags(clan)))));
            lore.add(" ");

            finalMeta.setLore(lore);
            finalItem.setItemMeta(finalMeta);
            callback.accept(finalItem);
        });
    }

    public static List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(b -> !AnarchyVanishHook.isPlayerVanished(b))
                .map(Player::getName)
                .toList();
    }

    public static List<String> getOnlineClanMembersNames(Player p) {
        Clan clan = ClanHandler.getClanByPlayer(p.getUniqueId());
        if (clan == null) return Collections.emptyList();

        return clan.getMembers().keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .map(Player::getName)
                .toList();
    }

    public static List<String> getClanMembersNames(Player p) {
        Clan clan = ClanHandler.getClanByPlayer(p.getUniqueId());
        if (clan == null) return Collections.emptyList();

        return clan.getMembers().keySet().stream()
                .map(Bukkit::getOfflinePlayer)
                .map(OfflinePlayer::getName)
                .toList();
    }

    public static List<String> getClanMembersNamesByTag(String tag) {
        Clan clan = ClanHandler.getClanByTag(tag);
        if (clan == null) return Collections.emptyList();

        return clan.getMembers().keySet().stream()
                .map(Bukkit::getOfflinePlayer)
                .map(OfflinePlayer::getName)
                .toList();
    }

    public static List<String> getClansTags(Player p) {
        Clan playerClan = ClanHandler.getClanByPlayer(p.getUniqueId());

        return ClanHandler.getClans().values().stream()
                .filter(c -> playerClan == null || !c.getId().equals(playerClan.getId()))
                .map(c -> getTag(c.getTag()))
                .toList();
    }

    public static List<String> getClansTags() {
        return ClanHandler.getClans().values().stream()
                .map(c -> getTag(c.getTag()))
                .toList();
    }

    public static List<String> getClansNames() {
        return ClanHandler.getClans().values().stream()
                .map(Clan::getName)
                .toList();
    }

    public static List<String> getClansIds() {
        return ClanHandler.getClans().values().stream()
                .map(Clan::getId)
                .toList();
    }

    public static void removeClansAlliesAndRivalsOnDelete(Clan target) {
        ClanHandler.getClans().values().forEach(other -> {
            if (other.isAlly(target) || other.isRival(target)) {
                other.removeAlly(target);
                other.removeRival(target);
                target.removeAlly(other);
                target.removeRival(other);
            }
        });
        ClanHandler.saveClans();
    }

    public static String getRoleTagColor(ClanRoles role) {
        return switch (role) {
            case LEADER     -> "&4♦";
            case SUB_LEADER -> "&c♦";
            case CAPTAIN    -> "&6♦";
            case RECRUITER  -> "&e♦";
            case LOYAL      -> "&a♦";
            case MEMBER     -> "&9♦";
            case RECRUIT    -> "&8♦";
        };
    }

    public static String getClanTagWithRole(UUID playerId) {
        Clan clan = ClanHandler.getClanByPlayer(playerId);
        if (clan == null) return "";

        ClanRoles role = clan.getRole(playerId);
        return " " + clan.getTag() + getRoleTagColor(role);
    }

    public static String serializeLocation(Location l) {
        if (l == null || l.getWorld() == null) return null;
        return l.getWorld().getName() + ',' + l.getX() + ',' + l.getY() + ',' + l.getZ() + ',' + l.getYaw() + ',' + l.getPitch();
    }

    public static Location deserializeLocation(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] part = s.split(",");
        return new Location(
                Bukkit.getWorld(part[0]),
                Double.parseDouble(part[1]),
                Double.parseDouble(part[2]),
                Double.parseDouble(part[3]),
                Float.parseFloat(part[4]),
                Float.parseFloat(part[5])
        );
    }

    public static String encodeItem(ItemStack item) {
        if (item == null) return null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos)) {
            oos.writeObject(item);
            oos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ItemStack decodeItem(String encoded) {
        if (encoded == null || encoded.isEmpty()) return null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(encoded));
             BukkitObjectInputStream ois = new BukkitObjectInputStream(bais)) {
            return (ItemStack) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getTimeNow() {
        return ZonedDateTime.now(SAO_PAULO_ZONE).toLocalTime().withNano(0).toString();
    }

    public static String formatTime(long millis) {
        if (millis <= 0) return "0s";

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}