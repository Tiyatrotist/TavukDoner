package dev.tavukdoner.skewer;

import dev.tavukdoner.Messages;
import dev.tavukdoner.TavukDonerPlugin;
import dev.tavukdoner.item.DonerItems;
import dev.tavukdoner.protection.Protection;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SkewerListener implements Listener {

    private final TavukDonerPlugin plugin;
    private final SkewerManager manager;

    public SkewerListener(@NotNull TavukDonerPlugin plugin, @NotNull SkewerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChunkLoad(ChunkLoadEvent event) {
        manager.loadChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChunkUnload(ChunkUnloadEvent event) {
        manager.unloadChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldSave(org.bukkit.event.world.WorldSaveEvent event) {
        manager.saveWorld(event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        Block above = broken.getRelative(BlockFace.UP);
        Skewer skewerAbove = manager.getSkewer(above.getLocation());
        if (skewerAbove != null) {
            manager.dropAndRemoveSkewer(skewerAbove);
        }

        Skewer skewerAt = manager.getSkewer(broken.getLocation());
        if (skewerAt != null) {
            manager.dropAndRemoveSkewer(skewerAt);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplodedBlocks(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplodedBlocks(event.blockList());
    }

    private void handleExplodedBlocks(List<Block> blocks) {
        Set<Skewer> toDrop = new HashSet<>();
        for (Block block : blocks) {
            Skewer above = manager.getSkewer(block.getRelative(BlockFace.UP).getLocation());
            if (above != null) {
                toDrop.add(above);
            }
            Skewer at = manager.getSkewer(block.getLocation());
            if (at != null) {
                toDrop.add(at);
            }
        }
        for (Skewer skewer : toDrop) {
            manager.dropAndRemoveSkewer(skewer);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (manager.getSkewer(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        Material rodMaterial = manager.getRodMaterial();
        if (item == null || item.getType() != rodMaterial) {
            return;
        }

        if (event.getBlockFace() != BlockFace.UP) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null || !clicked.getType().isSolid()) {
            return;
        }

        Block target = clicked.getRelative(BlockFace.UP);
        if (!target.getType().isAir()) {
            return;
        }

        Location targetLoc = target.getLocation();
        if (manager.getSkewer(targetLoc) != null) {
            return;
        }

        Player player = event.getPlayer();
        if (!Protection.canModify(player, targetLoc)) {
            event.setCancelled(true);
            return;
        }

        if (!player.hasPermission("tavukdoner.use")) {
            player.sendMessage(Messages.get("skewer.no-permission"));
            event.setCancelled(true);
            return;
        }

        if (!manager.canAddSkewer(targetLoc)) {
            player.sendMessage(Messages.get("skewer.limit"));
            event.setCancelled(true);
            return;
        }

        if (!manager.canPlayerAddSkewer(player.getUniqueId())) {
            player.sendMessage(Messages.get("skewer.player-limit"));
            event.setCancelled(true);
            return;
        }

        Skewer skewer = new Skewer(
                target.getWorld().getUID(),
                target.getX(),
                target.getY(),
                target.getZ(),
                0,
                0L,
                player.getUniqueId()
        );

        if (!manager.addSkewer(skewer)) {
            if (!manager.canPlayerAddSkewer(player.getUniqueId())) {
                player.sendMessage(Messages.get("skewer.player-limit"));
            } else {
                player.sendMessage(Messages.get("skewer.limit"));
            }
            event.setCancelled(true);
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            item.subtract(1);
        }

        event.setCancelled(true);
        player.sendMessage(Messages.get("skewer.placed"));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!(event.getRightClicked() instanceof Interaction interaction)) {
            return;
        }

        Skewer skewer = manager.getSkewerByInteraction(interaction.getUniqueId());
        if (skewer == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();
        boolean isChicken = handItem.getType() == Material.CHICKEN;
        boolean isSword = Tag.ITEMS_SWORDS.isTagged(handItem.getType());

        if (!isChicken && !isSword) {
            return;
        }

        Location loc = skewer.getLocation(interaction.getWorld());
        if (!Protection.canModify(player, loc)) {
            event.setCancelled(true);
            return;
        }

        if (!player.hasPermission("tavukdoner.use")) {
            player.sendMessage(Messages.get("skewer.no-permission"));
            event.setCancelled(true);
            return;
        }

        if (isChicken) {
            int maxLayers = manager.getMaxLayers();
            int currentLayers = skewer.getLayers();
            if (currentLayers >= maxLayers) {
                player.sendMessage(Messages.get("skewer.full"));
                event.setCancelled(true);
                return;
            }

            if (currentLayers > 0) {
                long diluted = skewer.getProgressTicks() * currentLayers / (currentLayers + 1);
                skewer.setProgressTicks(diluted);
            }
            skewer.setLayers(currentLayers + 1);
            if (player.getGameMode() != GameMode.CREATIVE) {
                handItem.subtract(1);
            }

            manager.refreshSkewer(skewer);
            event.setCancelled(true);
            player.sendMessage(Messages.get(
                    "skewer.layer-added",
                    Placeholder.parsed("layers", String.valueOf(skewer.getLayers())),
                    Placeholder.parsed("max", String.valueOf(maxLayers))
            ));
            return;
        }

        // Sword (dilimleme) logic
        long cookTicks = manager.getCookTicks();
        if (skewer.cookedLayers(cookTicks) < 1) {
            player.sendMessage(Messages.get("skewer.not-cooked"));
            event.setCancelled(true);
            return;
        }

        skewer.sliceLayer(cookTicks);

        int slicesPerCut = manager.getSlicesPerCut();
        ItemStack sliceItem = DonerItems.createSlice(slicesPerCut);

        Location centerLoc = new Location(interaction.getWorld(), skewer.getX() + 0.5, skewer.getY() + 0.5, skewer.getZ() + 0.5);
        Item dropped = interaction.getWorld().dropItem(centerLoc, sliceItem);

        Vector toPlayer = player.getLocation().toVector().subtract(centerLoc.toVector());
        if (toPlayer.lengthSquared() > 1e-4) {
            toPlayer.normalize().multiply(0.2);
            toPlayer.setY(0.15);
            dropped.setVelocity(toPlayer);
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            damageSword(player, handItem);
        }

        interaction.getWorld().playSound(centerLoc, Sound.ENTITY_SHEEP_SHEAR, 1.0f, 1.0f);

        if (skewer.getLayers() == 0) {
            manager.removeEmptySkewer(skewer);
            player.sendMessage(Messages.get("skewer.empty-removed"));
        } else {
            manager.refreshSkewer(skewer);
            player.sendMessage(Messages.get(
                    "skewer.sliced",
                    Placeholder.parsed("layers", String.valueOf(skewer.getLayers()))
            ));
        }

        event.setCancelled(true);
    }

    private void damageSword(@NotNull Player player, @NotNull ItemStack handItem) {
        try {
            // Paper ItemStack#damage(int, LivingEntity)
            handItem.damage(1, player);
        } catch (NoSuchMethodError ignored) {
            if (handItem.getItemMeta() instanceof Damageable damageable) {
                int current = damageable.getDamage();
                int max = handItem.getType().getMaxDurability();
                if (current + 1 >= max) {
                    player.getInventory().setItemInMainHand(null);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                } else {
                    damageable.setDamage(current + 1);
                    handItem.setItemMeta(damageable);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Interaction interaction)) {
            return;
        }
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        Skewer skewer = manager.getSkewerByInteraction(interaction.getUniqueId());
        if (skewer == null) {
            return;
        }

        event.setCancelled(true);

        Location loc = skewer.getLocation(interaction.getWorld());
        if (!Protection.canModify(player, loc)) {
            event.setCancelled(true);
            return;
        }

        if (!player.hasPermission("tavukdoner.use")) {
            player.sendMessage(Messages.get("skewer.no-permission"));
            return;
        }

        manager.dropAndRemoveSkewer(skewer);
        player.sendMessage(Messages.get("skewer.removed"));
    }
}
