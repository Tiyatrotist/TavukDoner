package dev.tavukdoner.skewer;

import dev.tavukdoner.TavukDonerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.inventory.ItemStack;
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
        private final UUID rodUuid;
        private final List<UUID> meatUuids;
        private final UUID interactionUuid;

        public RenderedEntities(UUID rodUuid, List<UUID> meatUuids, UUID interactionUuid) {
            this.rodUuid = rodUuid;
            this.meatUuids = meatUuids;
            this.interactionUuid = interactionUuid;
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

    public void render(@NotNull Skewer skewer) {
        World world = Bukkit.getWorld(skewer.getWorld());
        if (world == null || !world.isChunkLoaded(skewer.getX() >> 4, skewer.getZ() >> 4)) {
            return;
        }

        remove(skewer);

        Location baseLoc = new Location(world, skewer.getX() + 0.5, skewer.getY(), skewer.getZ() + 0.5);
        float spinAngle = skewer.getSpinAngle();

        // 1) Rod ItemDisplay (~1.6 blocks high, vertical)
        Material rodMaterial = manager.getRodMaterial();
        ItemDisplay rodDisplay = world.spawn(baseLoc, ItemDisplay.class, display -> {
            display.setPersistent(false);
            display.setItemStack(new ItemStack(rodMaterial));
            display.setItemDisplayTransform(ItemDisplayTransform.FIXED);

            Vector3f translation = new Vector3f(0f, 0.8f, 0f);
            Quaternionf leftRot = new Quaternionf().rotateY(spinAngle);
            Vector3f scale = new Vector3f(0.35f, 1.6f, 0.35f);
            Quaternionf rightRot = new Quaternionf().rotateZ((float) Math.toRadians(-45));
            display.setTransformation(new Transformation(translation, leftRot, scale, rightRot));
        });

        // 2) Meat ItemDisplays (conical: 0.35 + 0.07 * i, yOffset: 0.20 + 0.17 * i)
        List<UUID> meatUuids = new ArrayList<>();
        int cookedCount = skewer.cookedLayers(manager.getCookTicks());
        Quaternionf baseMeatRot = new Quaternionf().rotateX((float) Math.toRadians(90));
        Quaternionf meatLeftRot = new Quaternionf().rotateY(spinAngle).mul(baseMeatRot);
        Quaternionf meatRightRot = new Quaternionf();

        for (int i = 0; i < skewer.getLayers(); i++) {
            final int layerIndex = i;
            Material meatMaterial = (layerIndex < cookedCount) ? Material.COOKED_CHICKEN : Material.CHICKEN;
            ItemDisplay meatDisplay = world.spawn(baseLoc, ItemDisplay.class, display -> {
                display.setPersistent(false);
                display.setItemStack(new ItemStack(meatMaterial));
                display.setItemDisplayTransform(ItemDisplayTransform.FIXED);

                float s = (float) (0.35 + 0.07 * layerIndex);
                float yOffset = (float) (0.20 + 0.17 * layerIndex);

                Vector3f translation = new Vector3f(0f, yOffset, 0f);
                Vector3f scale = new Vector3f(s, s, s);
                display.setTransformation(new Transformation(translation, meatLeftRot, scale, meatRightRot));
            });
            meatUuids.add(meatDisplay.getUniqueId());
        }

        // 3) Interaction entity (width ~0.7, height ~1.7)
        Interaction interaction = world.spawn(baseLoc, Interaction.class, entity -> {
            entity.setPersistent(false);
            entity.setInteractionWidth(0.7f);
            entity.setInteractionHeight(1.7f);
            entity.setResponsive(true);
        });

        manager.registerInteraction(interaction.getUniqueId(), skewer);

        RenderedEntities rendered = new RenderedEntities(rodDisplay.getUniqueId(), meatUuids, interaction.getUniqueId());
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

        // 1) Rod transformasyonunu güncelle
        Entity rodEntity = world.getEntity(rendered.getRodUuid());
        if (rodEntity instanceof ItemDisplay rodDisplay) {
            Vector3f translation = new Vector3f(0f, 0.8f, 0f);
            Quaternionf leftRot = new Quaternionf().rotateY(newSpinAngle);
            Vector3f scale = new Vector3f(0.35f, 1.6f, 0.35f);
            Quaternionf rightRot = new Quaternionf().rotateZ((float) Math.toRadians(-45));

            rodDisplay.setInterpolationDelay(0);
            rodDisplay.setInterpolationDuration(intervalTicks);
            rodDisplay.setTransformation(new Transformation(translation, leftRot, scale, rightRot));
        }

        // 2) Et katmanlarının transformasyonunu güncelle
        Quaternionf baseMeatRot = new Quaternionf().rotateX((float) Math.toRadians(90));
        Quaternionf meatLeftRot = new Quaternionf().rotateY(newSpinAngle).mul(baseMeatRot);
        Quaternionf meatRightRot = new Quaternionf();

        List<UUID> meatUuids = rendered.getMeatUuids();
        for (int i = 0; i < meatUuids.size(); i++) {
            Entity meatEntity = world.getEntity(meatUuids.get(i));
            if (meatEntity instanceof ItemDisplay meatDisplay) {
                float s = (float) (0.35 + 0.07 * i);
                float yOffset = (float) (0.20 + 0.17 * i);

                Vector3f translation = new Vector3f(0f, yOffset, 0f);
                Vector3f scale = new Vector3f(s, s, s);

                meatDisplay.setInterpolationDelay(0);
                meatDisplay.setInterpolationDuration(intervalTicks);
                meatDisplay.setTransformation(new Transformation(translation, meatLeftRot, scale, meatRightRot));
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
