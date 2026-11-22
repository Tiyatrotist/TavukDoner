package dev.tavukdoner;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class Messages {

    private static TavukDonerPlugin plugin;
    private static YamlConfiguration messagesConfig;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Messages() {
    }

    public static void init(@NotNull TavukDonerPlugin pl) {
        plugin = pl;
        reload();
    }

    public static void reload() {
        if (plugin == null) {
            return;
        }

        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(file);

        InputStream defStream = plugin.getResource("messages.yml");
        if (defStream != null) {
            try (Reader reader = new InputStreamReader(defStream, StandardCharsets.UTF_8)) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
                messagesConfig.setDefaults(defConfig);
            } catch (Exception e) {
                plugin.getLogger().warning("messages.yml varsayilanlari yuklenirken hata: " + e.getMessage());
            }
        }
    }

    public static @NotNull Component get(@NotNull String key, @NotNull TagResolver... resolvers) {
        String lang = "tr";
        if (plugin != null) {
            lang = plugin.getConfig().getString("language", "tr");
        }
        if (lang == null || lang.isBlank()) {
            lang = "tr";
        }
        lang = lang.toLowerCase(Locale.ROOT);

        String raw = null;
        if (messagesConfig != null) {
            raw = messagesConfig.getString(lang + "." + key);
            if (raw == null && !"en".equals(lang)) {
                raw = messagesConfig.getString("en." + key);
            }
        }

        if (raw == null) {
            raw = key;
        }

        return MINI_MESSAGE.deserialize(raw, resolvers);
    }

    public static @NotNull List<Component> getList(@NotNull String key, @NotNull TagResolver... resolvers) {
        String lang = "tr";
        if (plugin != null) {
            lang = plugin.getConfig().getString("language", "tr");
        }
        if (lang == null || lang.isBlank()) {
            lang = "tr";
        }
        lang = lang.toLowerCase(Locale.ROOT);

        List<String> rawList = null;
        if (messagesConfig != null) {
            rawList = messagesConfig.getStringList(lang + "." + key);
            if (rawList.isEmpty() && !"en".equals(lang)) {
                rawList = messagesConfig.getStringList("en." + key);
            }
        }

        if (rawList == null || rawList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Component> components = new ArrayList<>(rawList.size());
        for (String line : rawList) {
            components.add(MINI_MESSAGE.deserialize(line, resolvers));
        }
        return components;
    }
}
