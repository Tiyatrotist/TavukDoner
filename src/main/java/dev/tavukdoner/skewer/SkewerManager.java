package dev.tavukdoner.skewer;

import dev.tavukdoner.TavukDonerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkewerManager {

    private final TavukDonerPlugin plugin;
    private final SkewerRenderer renderer;
    private final NamespacedKey skewersKey;

    private final Map<ChunkKey, Map<BlockKey, Skewer>> skewersByChunk = new ConcurrentHashMap<>();
    private final Map<UUID, Skewer> interactionToSkewer = new ConcurrentHashMap<>();

    private Material rodMaterial = Material.BLAZE_ROD;
    private int maxLayers = 8;
    private long cookTicks = 120L * 20L;
    private int maxSkewersPerChunk = 8;
    private int maxSkewersPerPlayer = 4;
    private long tickIntervalTicks = 20L;
    private int animationRadius = 32;
    private float spinDegreesPerSecond = 90.0f;
    private int slicesPerCut = 2;

    private CookingTask cookingTask;
    private org.bukkit.scheduler.BukkitTask cookingBukkitTask;

    public SkewerManager(@NotNull TavukDonerPlugin plugin, @NotNull SkewerRenderer renderer) {
        this.plugin = plugin;
        this.renderer = renderer;
        this.renderer.setManager(this);
        this.skewersKey = new NamespacedKey("tavukdoner", "skewers");
        reload();
    }

    public void reload() {
        String rodName = plugin.getConfig().getString("rod-material", "BLAZE_ROD");
        Material matched = Material.matchMaterial(rodName);
        this.rodMaterial = matched != null ? matched : Material.BLAZE_ROD;

        this.maxLayers = Math.max(1, plugin.getConfig().getInt("max-layers", 8));
        long cookSeconds = Math.max(1L, plugin.getConfig().getLong("cook-time-seconds", 120L));
        this.cookTicks = cookSeconds * 20L;
        this.maxSkewersPerChunk = Math.max(1, plugin.getConfig().getInt("max-skewers-per-chunk", 8));
        this.maxSkewersPerPlayer = Math.max(0, plugin.getConfig().getInt("max-skewers-per-player", 4));
        this.tickIntervalTicks = Math.max(1L, plugin.getConfig().getLong("tick-interval-ticks", 20L));
        this.animationRadius = Math.max(0, plugin.getConfig().getInt("animation-radius", 32));
        this.spinDegreesPerSecond = (float) Math.max(0.0, plugin.getConfig().getDouble("spin-degrees-per-second", 90.0));
        this.slicesPerCut = Math.max(1, plugin.getConfig().getInt("slices-per-cut", 2));

        if (cookingBukkitTask != null) {
            startCookingTask();
        }
    }

    public void onEnable() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                loadChunk(chunk);
            }
        }
        startCookingTask();
    }

    public void onDisable() {
        stopCookingTask();
        for (Map.Entry<ChunkKey, Map<BlockKey, Skewer>> entry : skewersByChunk.entrySet()) {
            ChunkKey chunkKey = entry.getKey();
            World world = Bukkit.getWorld(chunkKey.world());
            if (world != null && world.isChunkLoaded(chunkKey.x(), chunkKey.z())) {
                saveChunk(world.getChunkAt(chunkKey.x(), chunkKey.z()));
            }
        }
        renderer.removeAll();
        skewersByChunk.clear();
        interactionToSkewer.clear();
    }

    public void loadChunk(@NotNull Chunk chunk) {
        ChunkKey chunkKey = ChunkKey.from(chunk);
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (!pdc.has(skewersKey, PersistentDataType.STRING)) {
            return;
        }

        String raw = pdc.get(skewersKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            pdc.remove(skewersKey);
            return;
        }

        Map<BlockKey, Skewer> chunkMap = skewersByChunk.computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>());
        String[] lines = raw.split("\r?\n");
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length < 6) {
                continue;
            }
            try {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());
                int layers = Integer.parseInt(parts[3].trim());
                long progressTicks = Long.parseLong(parts[4].trim());
                String ownerStr = parts[5].trim();
                UUID owner = "-".equals(ownerStr) ? null : UUID.fromString(ownerStr);

                BlockKey blockKey = new BlockKey(x, y, z);
                Skewer skewer = new Skewer(chunk.getWorld().getUID(), x, y, z, layers, progressTicks, owner);
                chunkMap.put(blockKey, skewer);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    World world = Bukkit.getWorld(skewer.getWorld());
                    if (world == null || !world.isChunkLoaded(chunkKey.x(), chunkKey.z())) {
                        return;
                    }
                    Map<BlockKey, Skewer> currentChunkMap = skewersByChunk.get(chunkKey);
                    if (currentChunkMap == null || currentChunkMap.get(blockKey) != skewer) {
                        return;
                    }
                    renderer.render(skewer);
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Şiş verisi okunamadı (" + chunk.getX() + "," + chunk.getZ() + "): " + line + " - " + e.getMessage());
            }
        }
    }

    public void saveChunk(@NotNull Chunk chunk) {
        ChunkKey chunkKey = ChunkKey.from(chunk);
        Map<BlockKey, Skewer> chunkMap = skewersByChunk.get(chunkKey);
        if (chunkMap == null || chunkMap.isEmpty()) {
            chunk.getPersistentDataContainer().remove(skewersKey);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Skewer skewer : chunkMap.values()) {
            sb.append(skewer.getX()).append(',')
                    .append(skewer.getY()).append(',')
                    .append(skewer.getZ()).append(',')
                    .append(skewer.getLayers()).append(',')
                    .append(skewer.getProgressTicks()).append(',')
                    .append(skewer.getOwner() != null ? skewer.getOwner().toString() : "-")
                    .append('\n');
        }
        chunk.getPersistentDataContainer().set(skewersKey, PersistentDataType.STRING, sb.toString());
    }

    public void unloadChunk(@NotNull Chunk chunk) {
        saveChunk(chunk);
        ChunkKey chunkKey = ChunkKey.from(chunk);
        Map<BlockKey, Skewer> chunkMap = skewersByChunk.remove(chunkKey);
        if (chunkMap != null) {
            for (Skewer skewer : chunkMap.values()) {
                renderer.remove(skewer);
            }
        }
    }

    public boolean canAddSkewer(@NotNull Location location) {
        ChunkKey chunkKey = new ChunkKey(location.getWorld().getUID(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
        Map<BlockKey, Skewer> chunkMap = skewersByChunk.get(chunkKey);
        int currentCount = chunkMap != null ? chunkMap.size() : 0;
        return currentCount < maxSkewersPerChunk;
    }

    public boolean canPlayerAddSkewer(@NotNull UUID playerUuid) {
        if (maxSkewersPerPlayer <= 0) {
            return true;
        }
        int count = 0;
        for (Map<BlockKey, Skewer> chunkMap : skewersByChunk.values()) {
            for (Skewer skewer : chunkMap.values()) {
                if (playerUuid.equals(skewer.getOwner())) {
                    count++;
                    if (count >= maxSkewersPerPlayer) {
                        return false;
                    }
                }
            }
        }
        return count < maxSkewersPerPlayer;
    }

    public boolean addSkewer(@NotNull Skewer skewer) {
        World world = Bukkit.getWorld(skewer.getWorld());
        if (world == null) {
            return false;
        }

        Location loc = skewer.getLocation(world);
        if (!canAddSkewer(loc)) {
            return false;
        }

        if (skewer.getOwner() != null && !canPlayerAddSkewer(skewer.getOwner())) {
            return false;
        }

        ChunkKey chunkKey = skewer.getChunkKey();
        Map<BlockKey, Skewer> chunkMap = skewersByChunk.computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>());
        chunkMap.put(skewer.getBlockKey(), skewer);

        renderer.render(skewer);

        Chunk chunk = world.getChunkAt(chunkKey.x(), chunkKey.z());
        saveChunk(chunk);
        return true;
    }

    public void removeSkewer(@NotNull Skewer skewer) {
        renderer.remove(skewer);

        ChunkKey chunkKey = skewer.getChunkKey();
        Map<BlockKey, Skewer> chunkMap = skewersByChunk.get(chunkKey);
        if (chunkMap != null) {
            chunkMap.remove(skewer.getBlockKey());
            if (chunkMap.isEmpty()) {
                skewersByChunk.remove(chunkKey);
            }
        }

        World world = Bukkit.getWorld(skewer.getWorld());
        if (world != null && world.isChunkLoaded(chunkKey.x(), chunkKey.z())) {
            Chunk chunk = world.getChunkAt(chunkKey.x(), chunkKey.z());
            saveChunk(chunk);
        }
    }

    public void refreshSkewer(@NotNull Skewer skewer) {
        ChunkKey chunkKey = skewer.getChunkKey();
        World world = Bukkit.getWorld(skewer.getWorld());
        if (world != null && world.isChunkLoaded(chunkKey.x(), chunkKey.z())) {
            Chunk chunk = world.getChunkAt(chunkKey.x(), chunkKey.z());
            saveChunk(chunk);
        }
        renderer.refresh(skewer);
    }

    public void dropAndRemoveSkewer(@NotNull Skewer skewer) {
        World world = Bukkit.getWorld(skewer.getWorld());
        if (world != null) {
            Location dropLoc = new Location(world, skewer.getX() + 0.5, skewer.getY() + 0.5, skewer.getZ() + 0.5);
            world.dropItemNaturally(dropLoc, new ItemStack(rodMaterial, 1));
            int totalLayers = skewer.getLayers();
            if (totalLayers > 0) {
                int cooked = skewer.cookedLayers(cookTicks);
                int raw = totalLayers - cooked;
                if (cooked > 0) {
                    world.dropItemNaturally(dropLoc, new ItemStack(Material.COOKED_CHICKEN, cooked));
                }
                if (raw > 0) {
                    world.dropItemNaturally(dropLoc, new ItemStack(Material.CHICKEN, raw));
                }
            }
        }
        removeSkewer(skewer);
    }

    public void removeEmptySkewer(@NotNull Skewer skewer) {
        World world = Bukkit.getWorld(skewer.getWorld());
        if (world != null) {
            Location dropLoc = new Location(world, skewer.getX() + 0.5, skewer.getY() + 0.5, skewer.getZ() + 0.5);
            world.dropItemNaturally(dropLoc, new ItemStack(rodMaterial, 1));
        }
        removeSkewer(skewer);
    }

    public @Nullable Skewer getSkewer(@NotNull Location location) {
        ChunkKey chunkKey = new ChunkKey(location.getWorld().getUID(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
        Map<BlockKey, Skewer> chunkMap = skewersByChunk.get(chunkKey);
        if (chunkMap == null) {
            return null;
        }
        return chunkMap.get(BlockKey.from(location));
    }

    public @Nullable Skewer getSkewerByInteraction(@NotNull UUID interactionUuid) {
        return interactionToSkewer.get(interactionUuid);
    }

    public void registerInteraction(@NotNull UUID interactionUuid, @NotNull Skewer skewer) {
        interactionToSkewer.put(interactionUuid, skewer);
    }

    public void unregisterInteraction(@NotNull UUID interactionUuid) {
        interactionToSkewer.remove(interactionUuid);
    }

    public @NotNull java.util.List<Skewer> snapshot() {
        java.util.List<Skewer> list = new java.util.ArrayList<>();
        for (Map<BlockKey, Skewer> chunkMap : skewersByChunk.values()) {
            list.addAll(chunkMap.values());
        }
        return list;
    }

    public void saveWorld(@NotNull World world) {
        UUID worldUid = world.getUID();
        for (Map.Entry<ChunkKey, Map<BlockKey, Skewer>> entry : skewersByChunk.entrySet()) {
            ChunkKey chunkKey = entry.getKey();
            if (chunkKey.world().equals(worldUid)) {
                if (world.isChunkLoaded(chunkKey.x(), chunkKey.z())) {
                    Chunk chunk = world.getChunkAt(chunkKey.x(), chunkKey.z());
                    saveChunk(chunk);
                }
            }
        }
    }

    public void startCookingTask() {
        stopCookingTask();
        this.cookingTask = new CookingTask(plugin, this, renderer);
        this.cookingBukkitTask = this.cookingTask.runTaskTimer(plugin, tickIntervalTicks, tickIntervalTicks);
    }

    public void stopCookingTask() {
        if (cookingBukkitTask != null) {
            cookingBukkitTask.cancel();
            cookingBukkitTask = null;
        }
        if (cookingTask != null) {
            try {
                cookingTask.cancel();
            } catch (IllegalStateException ignored) {
            }
            cookingTask = null;
        }
    }

    public int getSlicesPerCut() {
        return slicesPerCut;
    }

    public long getTickIntervalTicks() {
        return tickIntervalTicks;
    }

    public int getAnimationRadius() {
        return animationRadius;
    }

    public float getSpinDegreesPerSecond() {
        return spinDegreesPerSecond;
    }

    public Material getRodMaterial() {
        return rodMaterial;
    }

    public int getMaxLayers() {
        return maxLayers;
    }

    public long getCookTicks() {
        return cookTicks;
    }

    public int getMaxSkewersPerChunk() {
        return maxSkewersPerChunk;
    }

    public int getMaxSkewersPerPlayer() {
        return maxSkewersPerPlayer;
    }
}
