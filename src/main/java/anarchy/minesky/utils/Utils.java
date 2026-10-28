package anarchy.minesky.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String c(String s) {
        return hex(s);
    }

    private static String createHex(String hexString) {
        hexString = hexString.replace("&", "");
        return net.md_5.bungee.api.ChatColor.of(hexString).toString();
    }

    public static String hex(String message) {
        Pattern hexPattern = Pattern.compile("&#[A-Fa-f0-9]{6}");
        Matcher matcher = hexPattern.matcher(message);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(result, createHex(matcher.group()));
        }

        matcher.appendTail(result);
        message = result.toString();

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
