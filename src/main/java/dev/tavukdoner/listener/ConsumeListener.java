package dev.tavukdoner.listener;

import dev.tavukdoner.TavukDonerPlugin;
import dev.tavukdoner.item.DonerItems;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Tavuk Doner Ekmek tuketildiginde besin degerlerini ve seviyeye gore
 * buff (iksir) etkilerini uygulayan dinleyici sinif.
 */
public final class ConsumeListener implements Listener {

    private final TavukDonerPlugin plugin;

    public ConsumeListener(@NotNull TavukDonerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Oyuncu bir esya yediginde tetiklenir; esya Tavuk Doner Ekmek ise
     * seviye bonuslari ve iksir etkilerini uygular.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerItemConsume(@NotNull PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (!DonerItems.isWrap(item)) {
            return;
        }

        Player player = event.getPlayer();
        int tier = DonerItems.getTier(item);

        // Paper DataComponentTypes.FOOD esyada bulunamadiysa, acikta kalan besin farkini
        // guvenli sekilde uygula (tavan 20). DataComponent varsa vanilla akisi beslemeyi
        // bilesen uzerinden dogrudan yapar; boylece cift besleme olusmaz.
        boolean hasFoodComponent = false;
        try {
            hasFoodComponent = item.hasData(DataComponentTypes.FOOD);
        } catch (Throwable ignored) {
            // Paper component destegi yoksa false kabul edilir
        }

        if (!hasFoodComponent) {
            int nutrition = plugin.getConfig().getInt("wrap.nutrition", 10);
            float saturation = (float) plugin.getConfig().getDouble("wrap.saturation", 12.0);
            int currentFood = player.getFoodLevel();
            int newFood = Math.min(20, currentFood + nutrition);
            player.setFoodLevel(newFood);
            player.setSaturation(Math.min((float) newFood, Math.min(20.0f, player.getSaturation() + saturation)));
        }

        // Seviyeye gore bonus sureli iksir etkilerini uygula
        applyWrapEffects(player, tier);
    }

    /**
     * Konfigurasyonda tanimlanan iksir etkilerini, seviye bonusu (tier-1)*bonus ekleyerek oyuncuya uygular.
     *
     * @param player etki uygulanacak oyuncu
     * @param tier   doner ekmegin seviyesi (1..3)
     */
    private void applyWrapEffects(@NotNull Player player, int tier) {
        int bonusPerTier = plugin.getConfig().getInt("wrap.tier-bonus-seconds", 15);
        int bonusSeconds = Math.max(0, (tier - 1) * bonusPerTier);

        List<String> effectEntries = plugin.getConfig().getStringList("wrap.effects");
        if (effectEntries.isEmpty()) {
            effectEntries = List.of("SPEED:30:0", "RESISTANCE:30:0");
        }

        for (String entry : effectEntries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            String[] parts = entry.split(":");
            if (parts.length < 3) {
                plugin.getLogger().warning("Gecersiz efekt formati atlandi: '" + entry + "'. Beklenen format: TYPE:saniye:seviye");
                continue;
            }

            String rawType = parts[0].trim();
            int baseSeconds;
            int amplifier;
            try {
                baseSeconds = Integer.parseInt(parts[1].trim());
                amplifier = Integer.parseInt(parts[2].trim());
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Efekt suresi veya seviyesi sayisal degil: '" + entry + "'");
                continue;
            }

            PotionEffectType type = resolvePotionEffectType(rawType);
            if (type == null) {
                plugin.getLogger().warning("Gecersiz efekt turu atlandi: '" + rawType + "'");
                continue;
            }

            int totalSeconds = Math.max(1, baseSeconds + bonusSeconds);
            int durationTicks = totalSeconds * 20;

            player.addPotionEffect(new PotionEffect(type, durationTicks, Math.max(0, amplifier)));
        }
    }

    /**
     * Verilen metin tipindeki etki turunu Registry.MOB_EFFECT uzerinden cozer.
     *
     * @param rawType cozulmek istenen etki adi (orn. SPEED, RESISTANCE, minecraft:speed)
     * @return bulunan PotionEffectType veya bulunamazsa null
     */
    private @Nullable PotionEffectType resolvePotionEffectType(@NotNull String rawType) {
        String trimmed = rawType.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        NamespacedKey key;
        if (lower.contains(":")) {
            key = NamespacedKey.fromString(lower);
        } else {
            key = NamespacedKey.minecraft(lower);
        }

        if (key != null) {
            PotionEffectType type = Registry.MOB_EFFECT.get(key);
            if (type != null) {
                return type;
            }
        }

        for (PotionEffectType type : Registry.MOB_EFFECT) {
            NamespacedKey typeKey = type.getKey();
            if (typeKey.getKey().equalsIgnoreCase(trimmed) || typeKey.asString().equalsIgnoreCase(trimmed)) {
                return type;
            }
        }

        return null;
    }
}
