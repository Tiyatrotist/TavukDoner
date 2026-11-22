package dev.tavukdoner.skewer;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class Skewer {

    private final UUID world;
    private final int x;
    private final int y;
    private final int z;
    private int layers;
    private long progressTicks;
    private @Nullable UUID owner;
    private float spinAngle = 0.0f;

    public Skewer(
            @NotNull UUID world,
            int x,
            int y,
            int z,
            int layers,
            long progressTicks,
            @Nullable UUID owner
    ) {
        this.world = Objects.requireNonNull(world, "world cannot be null");
        this.x = x;
        this.y = y;
        this.z = z;
        this.layers = layers;
        this.progressTicks = progressTicks;
        this.owner = owner;
    }

    public boolean isCooked(long cookTicks) {
        return cookTicks > 0 && this.layers > 0 && this.progressTicks >= cookTicks;
    }

    public int cookedLayers(long cookTicks) {
        if (cookTicks <= 0 || layers <= 0 || progressTicks <= 0) {
            return 0;
        }
        long effectiveProgress = Math.min(progressTicks, cookTicks);
        int cooked = (int) Math.floor(layers * effectiveProgress / (double) cookTicks);
        return Math.min(layers, Math.max(0, cooked));
    }

    public void sliceLayer(long cookTicks) {
        if (cookTicks <= 0 || layers <= 0 || cookedLayers(cookTicks) < 1) {
            return;
        }

        int l = this.layers;
        int c = cookedLayers(cookTicks);
        double frac = ((double) this.progressTicks * l) / (double) cookTicks - c;
        if (frac < 0.0) {
            frac = 0.0;
        } else if (frac > 1.0) {
            frac = 1.0;
        }

        this.layers = l - 1;
        if (this.layers > 0) {
            long newProgress = Math.round(((c - 1) + frac) * (double) cookTicks / (double) this.layers);
            this.progressTicks = Math.min(cookTicks, Math.max(0L, newProgress));
        } else {
            this.progressTicks = 0L;
        }
    }

    public float getSpinAngle() {
        return spinAngle;
    }

    public void setSpinAngle(float spinAngle) {
        this.spinAngle = spinAngle;
    }

    public @NotNull UUID getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getLayers() {
        return layers;
    }

    public void setLayers(int layers) {
        this.layers = layers;
    }

    public long getProgressTicks() {
        return progressTicks;
    }

    public void setProgressTicks(long progressTicks) {
        this.progressTicks = progressTicks;
    }

    public @Nullable UUID getOwner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    public @NotNull Location getLocation(@NotNull World worldObj) {
        return new Location(worldObj, x, y, z);
    }

    public @NotNull BlockKey getBlockKey() {
        return new BlockKey(x, y, z);
    }

    public @NotNull ChunkKey getChunkKey() {
        return new ChunkKey(world, x >> 4, z >> 4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Skewer skewer = (Skewer) o;
        return x == skewer.x && y == skewer.y && z == skewer.z && Objects.equals(world, skewer.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }

    @Override
    public String toString() {
        return "Skewer{" +
                "world=" + world +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", layers=" + layers +
                ", progressTicks=" + progressTicks +
                ", owner=" + owner +
                '}';
    }
}
