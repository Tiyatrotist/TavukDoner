package dev.tavukdoner;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TavukDonerCommand implements CommandExecutor, TabCompleter {

    private final TavukDonerPlugin plugin;

    public TavukDonerCommand(@NotNull TavukDonerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("tavukdoner.admin")) {
                sender.sendMessage(Messages.get("no-permission"));
                return true;
            }

            plugin.reloadConfig();
            Messages.reload();
            if (plugin.getSkewerManager() != null) {
                plugin.getSkewerManager().reload();
            }
            if (plugin.getRecipeManager() != null) {
                plugin.getRecipeManager().reload();
            }
            sender.sendMessage(Messages.get("config-reloaded"));
            return true;
        }

        String version = plugin.getPluginMeta().getVersion();
        sender.sendMessage(Messages.get("version-info", Placeholder.parsed("version", version)));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("tavukdoner.admin") && "reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
