package dev.tavukdoner.skewer;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public record BlockKey(int x, int y, int z) {
    public static @NotNull BlockKey from(@NotNull Location location) {
        return new BlockKey(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
