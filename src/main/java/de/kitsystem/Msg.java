package de.kitsystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class Msg {
    public static final String PREFIX = "§8[§bKits§8] §7";

    private Msg() {}

    public static Component c(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static void send(CommandSender s, String msg) {
        s.sendMessage(c(PREFIX + msg));
    }
}
