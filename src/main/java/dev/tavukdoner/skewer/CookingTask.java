package dev.tavukdoner.skewer;

import dev.tavukdoner.TavukDonerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CookingTask extends BukkitRunnable {

    private final TavukDonerPlugin plugin;
    private final SkewerManager manager;
    private final SkewerRenderer renderer;

    public CookingTask(
            @NotNull TavukDonerPlugin plugin,
            @NotNull SkewerManager manager,
            @NotNull SkewerRenderer renderer
    ) {
        this.plugin = plugin;
        this.manager = manager;
        this.renderer = renderer;
    }

    @Override
    public void run() {
        List<Skewer> skewers = manager.snapshot();
        if (skewers.isEmpty()) {
            return;
        }

        long cookTicks = manager.getCookTicks();
        long intervalTicks = manager.getTickIntervalTicks();
        float spinDegPerSec = manager.getSpinDegreesPerSecond();
        float deltaRad = (float) Math.toRadians(spinDegPerSec) * ((float) intervalTicks / 20.0f);
        int animRadius = manager.getAnimationRadius();

        for (Skewer skewer : skewers) {
            if (skewer.getLayers() <= 0) {
                continue;
            }

            World world = Bukkit.getWorld(skewer.getWorld());
            if (world == null || !world.isChunkLoaded(skewer.getX() >> 4, skewer.getZ() >> 4)) {
                continue;
            }

            boolean hasHeat = checkHeat(world, skewer.getX(), skewer.getY(), skewer.getZ());
            if (!hasHeat) {
                continue;
            }

            // Isı var: Pişme ilerlemesini artır
            long currentProgress = skewer.getProgressTicks();
            int oldCookedLayers = skewer.cookedLayers(cookTicks);

            if (currentProgress < cookTicks) {
                long newProgress = Math.min(cookTicks, currentProgress + intervalTicks);
                skewer.setProgressTicks(newProgress);
                int newCookedLayers = skewer.cookedLayers(cookTicks);
                if (newCookedLayers != oldCookedLayers) {
                    manager.refreshSkewer(skewer);
                }
            }

            // Dönme güncellemesi: yakında oyuncu varsa veya animasyon mesafesi 0 ise
            boolean shouldAnimate;
            if (animRadius <= 0) {
                shouldAnimate = true;
            } else {
                Location centerLoc = new Location(world, skewer.getX() + 0.5, skewer.getY() + 0.5, skewer.getZ() + 0.5);
                shouldAnimate = !world.getNearbyPlayers(centerLoc, animRadius).isEmpty();
            }

            if (shouldAnimate) {
                renderer.spin(skewer, deltaRad, (int) intervalTicks);
            }
        }
    }

    private boolean checkHeat(@NotNull World world, int x, int y, int z) {
        for (int dy = -1; dy <= 0; dy++) {
            int checkY = y + dy;
            if (checkY < world.getMinHeight() || checkY >= world.getMaxHeight()) {
                continue;
            }
            for (int dx = -1; dx <= 1; dx++) {
                int checkX = x + dx;
                for (int dz = -1; dz <= 1; dz++) {
                    int checkZ = z + dz;
                    int chunkX = checkX >> 4;
                    int chunkZ = checkZ >> 4;
                    if (!world.isChunkLoaded(chunkX, chunkZ)) {
                        continue;
                    }
                    Block block = world.getBlockAt(checkX, checkY, checkZ);
                    Material mat = block.getType();
                    if (mat == Material.CAMPFIRE || mat == Material.SOUL_CAMPFIRE) {
                        if (block.getBlockData() instanceof Campfire campfire && campfire.isLit()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
