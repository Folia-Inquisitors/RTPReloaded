package dev.codex.rtpreloaded.command;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class CommandAliasNormalizer implements Listener {
    private static final Pattern KNOWN_COMMAND = Pattern.compile("^/(rtp|wild|randomtp|rtpreloaded|truertp)(?=$|\\s)", Pattern.CASE_INSENSITIVE);

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Matcher matcher = KNOWN_COMMAND.matcher(event.getMessage());
        if (!matcher.find()) {
            return;
        }

        String command = matcher.group(1).toLowerCase(Locale.ROOT);
        String normalized = command.equals("truertp") ? "rtpreloaded" : command;
        String arguments = event.getMessage().substring(matcher.end());
        event.setMessage("/" + normalized + arguments);
    }
}