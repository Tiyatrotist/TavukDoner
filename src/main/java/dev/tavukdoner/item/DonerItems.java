package dev.tavukdoner.item;

import dev.tavukdoner.Messages;
import dev.tavukdoner.TavukDonerPlugin;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.FoodProperties;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * TavukDoner ozel esyalarinin (doner dilimi, doner ekmek vb.) olusturulmasi,
 * tespiti ve PersistentDataContainer (PDC) islemlerini yoneten yardimci sinif.
 */
public final class DonerItems {

    /**
     * Doner esyalarini tanimlamak icin kullanilan PDC anahtari (tavukdoner:item).
     */
    public static final NamespacedKey ITEM_KEY = new NamespacedKey("tavukdoner", "item");

    /**
     * Doner ekmek seviyesini (tier) saklamak icin kullanilan PDC anahtari (tavukdoner:tier).
     */
    public static final NamespacedKey TIER_KEY = new NamespacedKey("tavukdoner", "tier");

    /**
     * Doner dilimi icin PDC degeri ("slice").
     */
    public static final String KEY_SLICE = "slice";

    /**
     * Tavuk doner ekmek icin PDC degeri ("wrap").
     */
    public static final String KEY_WRAP = "wrap";

    private DonerItems() {
    }

    /**
     * Belirtilen miktarda ozel doner dilimi olusturur.
     *
     * @param amount olusturulacak dilim miktari (en az 1)
     * @return hazirlanmis ItemStack (COOKED_CHICKEN)
     */
    public static @NotNull ItemStack createSlice(int amount) {
        ItemStack item = new ItemStack(Material.COOKED_CHICKEN, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, KEY_SLICE);

            Component name = Messages.get("items.slice-name").decoration(TextDecoration.ITALIC, false);
            meta.displayName(name);

            List<Component> lore = Messages.getList("items.slice-lore");
            if (!lore.isEmpty()) {
                List<Component> nonItalicLore = new ArrayList<>(lore.size());
                for (Component line : lore) {
                    nonItalicLore.add(line.decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(nonItalicLore);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 1 adet ozel doner dilimi olusturur.
     *
     * @return hazirlanmis ItemStack
     */
    public static @NotNull ItemStack createSlice() {
        return createSlice(1);
    }

    /**
     * Verilen esyanin ozel TavukDoner dilimi olup olmadigini kontrol eder.
     *
     * @param item kontrol edilecek ItemStack
     * @return doner dilimi ise true, aksi halde false
     */
    public static boolean isSlice(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.COOKED_CHICKEN || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return KEY_SLICE.equals(pdc.get(ITEM_KEY, PersistentDataType.STRING));
    }

    /**
     * 1 adet belirtilen seviyede (tier 1..3) Tavuk Doner Ekmek olusturur.
     *
     * @param tier doner ekmek seviyesi (1, 2 veya 3)
     * @return hazirlanmis ItemStack (BREAD)
     */
    public static @NotNull ItemStack createWrap(int tier) {
        return createWrap(tier, 1);
    }

    /**
     * Belirtilen miktar ve seviyede (tier 1..3) Tavuk Doner Ekmek olusturur.
     *
     * @param tier doner ekmek seviyesi (1..3 araligina kirpilir)
     * @param amount olusturulacak adet (en az 1)
     * @return hazirlanmis ItemStack (BREAD)
     */
    public static @NotNull ItemStack createWrap(int tier, int amount) {
        int clampedTier = Math.max(1, Math.min(3, tier));
        ItemStack item = new ItemStack(Material.BREAD, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(ITEM_KEY, PersistentDataType.STRING, KEY_WRAP);
            pdc.set(TIER_KEY, PersistentDataType.INTEGER, clampedTier);

            Component name = Messages.get("items.wrap-name").decoration(TextDecoration.ITALIC, false);
            meta.displayName(name);

            List<Component> lore = Messages.getList("items.wrap-lore");
            List<Component> nonItalicLore = new ArrayList<>();
            if (!lore.isEmpty()) {
                for (Component line : lore) {
                    nonItalicLore.add(line.decoration(TextDecoration.ITALIC, false));
                }
            }
            Component tierLine = Messages.get("items.wrap-tier-" + clampedTier)
                    .decoration(TextDecoration.ITALIC, false);
            nonItalicLore.add(tierLine);
            meta.lore(nonItalicLore);

            item.setItemMeta(meta);
        }

        // Paper DataComponentTypes.FOOD ile besin degerlerini esya bileseni olarak ayarla
        try {
            int nutrition = 10;
            float saturation = 12.0f;
            try {
                TavukDonerPlugin plugin = JavaPlugin.getPlugin(TavukDonerPlugin.class);
                if (plugin != null && plugin.getConfig() != null) {
                    nutrition = plugin.getConfig().getInt("wrap.nutrition", 10);
                    saturation = (float) plugin.getConfig().getDouble("wrap.saturation", 12.0);
                }
            } catch (Throwable ignored) {
                // Plugin henuz hazir degilse varsayilan degerlerle devam et
            }
            item.setData(DataComponentTypes.FOOD, FoodProperties.food().nutrition(nutrition).saturation(saturation));
        } catch (Throwable ignored) {
            // Paper DataComponent destegi bulunamazsa vanilla ekmek degerleriyle devam edilir
        }

        return item;
    }

    /**
     * Verilen esyanin Tavuk Doner Ekmek olup olmadigini dogrular.
     *
     * @param item kontrol edilecek ItemStack
     * @return Tavuk Doner Ekmek ise true, aksi halde false
     */
    public static boolean isWrap(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.BREAD || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return KEY_WRAP.equals(pdc.get(ITEM_KEY, PersistentDataType.STRING));
    }

    /**
     * Verilen Tavuk Doner Ekmek esyasinin seviyesini (tier) dondurur.
     *
     * @param item incelenecek ItemStack
     * @return esya wrap ise 1..3 seviyesi, wrap degilse 0
     */
    public static int getTier(@Nullable ItemStack item) {
        if (!isWrap(item)) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 1;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer tier = pdc.get(TIER_KEY, PersistentDataType.INTEGER);
        if (tier == null) {
            return 1;
        }
        return Math.max(1, Math.min(3, tier));
    }
}
