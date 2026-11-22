package dev.tavukdoner.skewer;

import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record ChunkKey(@NotNull UUID world, int x, int z) {
    public static @NotNull ChunkKey from(@NotNull Chunk chunk) {
        return new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }
}
