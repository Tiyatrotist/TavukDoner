package dev.tavukdoner.skewer;

import dev.tavukdoner.TavukDonerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkewerRenderer {

    private final TavukDonerPlugin plugin;
    private SkewerManager manager;
    private final Map<RenderKey, RenderedEntities> renderedSkewers = new ConcurrentHashMap<>();

    private record RenderKey(UUID world, int x, int y, int z) {
        public static RenderKey from(Skewer skewer) {
            return new RenderKey(skewer.getWorld(), skewer.getX(), skewer.getY(), skewer.getZ());
        }
    }

    public static final class RenderedEntities {
        private final UUID plateUuid;
        private final UUID rodUuid;
        private final List<UUID> meatUuids;
        private final UUID interactionUuid;

        public RenderedEntities(UUID plateUuid, UUID rodUuid, List<UUID> meatUuids, UUID interactionUuid) {
            this.plateUuid = plateUuid;
            this.rodUuid = rodUuid;
            this.meatUuids = meatUuids;
            this.interactionUuid = interactionUuid;
        }

        public UUID getPlateUuid() {
            return plateUuid;
        }

        public UUID getRodUuid() {
            return rodUuid;
        }

        public List<UUID> getMeatUuids() {
            return meatUuids;
        }

        public UUID getInteractionUuid() {
            return interactionUuid;
        }
    }

    public SkewerRenderer(@NotNull TavukDonerPlugin plugin) {
        this.plugin = plugin;
    }

    public void setManager(@NotNull SkewerManager manager) {
        this.manager = manager;
    }

    public static Transformation boxTransform(float w, float h, float y, float yaw) {
        Vector3f scale = new Vector3f(w, h, w);
        Quaternionf leftRot = new Quaternionf().rotateY(yaw);
        Vector3f offset = new Vector3f(w / 2f, 0f, w / 2f);
        leftRot.transform(offset);
        Vector3f translation = new Vector3f(0f, y, 0f).sub(offset);
        Quaternionf rightRot = new Quaternionf();
        return new Transformation(translation, leftRot, scale, rightRot);
    }

    public void render(@NotNull Skewer skewer) {
        World world = Bukkit.getWorld(skewer.getWorld());
        if (world == null || !world.isChunkLoaded(skewer.getX() >> 4, skewer.getZ() >> 4)) {
            return;
        }

        remove(skewer);

        Location baseLoc = new Location(world, skewer.getX() + 0.5, skewer.getY(), skewer.getZ() + 0.5);
        float spinAngle = skewer.getSpinAngle();

        // 1) Tepsi (taban): SMOOTH_STONE bloğu, w=0.8, h=0.06, y=0
        BlockDisplay plateDisplay = world.spawn(baseLoc, BlockDisplay.class, display -> {
            display.setPersistent(false);
            display.setBlock(Material.SMOOTH_STONE.createBlockData());
            display.setTransformation(boxTransform(0.8f, 0.06f, 0f, 0f));
        });

        // 2) Şiş çubuğu: IRON_BLOCK bloğu, w=0.06, h=2.05, y=0.06. Dönmez (sabit).
        BlockDisplay rodDisplay = world.spawn(baseLoc, BlockDisplay.class, display -> {
            display.setPersistent(false);
            display.setBlock(Material.IRON_BLOCK.createBlockData());
            display.setTransformation(boxTransform(0.06f, 2.05f, 0.06f, 0f));
        });

        // 3) Et katmanları: layers adet BlockDisplay
        List<UUID> meatUuids = new ArrayList<>();
        int layers = skewer.getLayers();
        int cookedLayers = skewer.cookedLayers(manager.getCookTicks());
        List<Material> rawBlocks = manager.getRawBlocks();
        List<Material> cookedBlocks = manager.getCookedBlocks();

        for (int i = 0; i < layers; i++) {
            final int layerIndex = i;
            float h = 0.105f;
            float y = 0.10f + 0.10f * layerIndex;
            float w = 0.34f + 0.02f * layerIndex;
            float baseYaw = (layerIndex % 2 != 0) ? (float) Math.toRadians(45) : 0f;
            float yaw = baseYaw + spinAngle;

            boolean isCooked = layerIndex < cookedLayers;
            List<Material> blockList = isCooked ? cookedBlocks : rawBlocks;
            Material material = blockList.get(layerIndex % blockList.size());

            BlockDisplay meatDisplay = world.spawn(baseLoc, BlockDisplay.class, display -> {
                display.setPersistent(false);
                display.setBlock(material.createBlockData());
                display.setTransformation(boxTransform(w, h, y, yaw));
            });
            meatUuids.add(meatDisplay.getUniqueId());
        }

        // 4) Interaction: genişlik 0.8, yükseklik 2.05 (çubuk boyu)
        Interaction interaction = world.spawn(baseLoc, Interaction.class, entity -> {
            entity.setPersistent(false);
            entity.setInteractionWidth(0.8f);
            entity.setInteractionHeight(2.05f);
            entity.setResponsive(true);
        });

        manager.registerInteraction(interaction.getUniqueId(), skewer);

        RenderedEntities rendered = new RenderedEntities(
                plateDisplay.getUniqueId(),
                rodDisplay.getUniqueId(),
                meatUuids,
                interaction.getUniqueId()
        );
        renderedSkewers.put(RenderKey.from(skewer), rendered);
    }

    public void remove(@NotNull Skewer skewer) {
        RenderKey key = RenderKey.from(skewer);
        RenderedEntities rendered = renderedSkewers.remove(key);
        if (rendered == null) {
            return;
        }

        manager.unregisterInteraction(rendered.getInteractionUuid());

        World world = Bukkit.getWorld(skewer.getWorld());
        if (world != null) {
            Entity plate = world.getEntity(rendered.getPlateUuid());
            if (plate != null) {
                plate.remove();
            }
            Entity rod = world.getEntity(rendered.getRodUuid());
            if (rod != null) {
                rod.remove();
            }
            for (UUID meatUuid : rendered.getMeatUuids()) {
                Entity meat = world.getEntity(meatUuid);
                if (meat != null) {
                    meat.remove();
                }
            }
            Entity interaction = world.getEntity(rendered.getInteractionUuid());
            if (interaction != null) {
                interaction.remove();
            }
        }
    }

    public void refresh(@NotNull Skewer skewer) {
        remove(skewer);
        render(skewer);
    }

    public void spin(@NotNull Skewer skewer, float deltaRad, int intervalTicks) {
        float maxDelta = (float) Math.toRadians(170);
        if (deltaRad > maxDelta) {
            deltaRad = maxDelta;
        } else if (deltaRad < -maxDelta) {
            deltaRad = -maxDelta;
        }

        float newSpinAngle = skewer.getSpinAngle() + deltaRad;
        if (newSpinAngle >= (float) (Math.PI * 4)) {
            newSpinAngle -= (float) (Math.PI * 4);
        } else if (newSpinAngle < 0f) {
            newSpinAngle += (float) (Math.PI * 4);
        }
        skewer.setSpinAngle(newSpinAngle);

        RenderKey key = RenderKey.from(skewer);
        RenderedEntities rendered = renderedSkewers.get(key);
        if (rendered == null) {
            return;
        }

        World world = Bukkit.getWorld(skewer.getWorld());
        if (world == null || !world.isChunkLoaded(skewer.getX() >> 4, skewer.getZ() >> 4)) {
            return;
        }

        // Yalnız et katmanlarının dönüşünü güncelle, çubuk ve tepsi sabit kalır
        List<UUID> meatUuids = rendered.getMeatUuids();
        for (int i = 0; i < meatUuids.size(); i++) {
            Entity meatEntity = world.getEntity(meatUuids.get(i));
            if (meatEntity instanceof BlockDisplay meatDisplay) {
                float h = 0.105f;
                float y = 0.10f + 0.10f * i;
                float w = 0.34f + 0.02f * i;
                float baseYaw = (i % 2 != 0) ? (float) Math.toRadians(45) : 0f;
                float yaw = baseYaw + newSpinAngle;

                meatDisplay.setInterpolationDelay(0);
                meatDisplay.setInterpolationDuration(intervalTicks);
                meatDisplay.setTransformation(boxTransform(w, h, y, yaw));
            }
        }
    }

    public void removeAll() {
        for (RenderKey key : new ArrayList<>(renderedSkewers.keySet())) {
            RenderedEntities rendered = renderedSkewers.remove(key);
            if (rendered != null) {
                manager.unregisterInteraction(rendered.getInteractionUuid());
                World world = Bukkit.getWorld(key.world());
                if (world != null) {
                    Entity plate = world.getEntity(rendered.getPlateUuid());
                    if (plate != null) {
                        plate.remove();
                    }
                    Entity rod = world.getEntity(rendered.getRodUuid());
                    if (rod != null) {
                        rod.remove();
                    }
                    for (UUID meatUuid : rendered.getMeatUuids()) {
                        Entity meat = world.getEntity(meatUuid);
                        if (meat != null) {
                            meat.remove();
                        }
                    }
                    Entity interaction = world.getEntity(rendered.getInteractionUuid());
                    if (interaction != null) {
                        interaction.remove();
                    }
                }
            }
        }
    }
}
