package me.zombieman.dev.suffixesplus.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ChatUtil {
    public static Component parseLegacyColors(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
}
