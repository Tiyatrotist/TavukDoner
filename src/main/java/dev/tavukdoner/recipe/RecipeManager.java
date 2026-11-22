package dev.tavukdoner.recipe;

import dev.tavukdoner.TavukDonerPlugin;
import dev.tavukdoner.item.DonerItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tavuk Doner Ekmek tariflerinin olusturulmasi, kaydedilmesi, kaldirilmasi,
 * uretim sirasinda izin denetimi yapilmasi ve oyunculara tarif defterinde
 * kesfettirilmesi islemlerini yoneten sinif.
 */
public final class RecipeManager implements Listener {

    private final TavukDonerPlugin plugin;
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();

    public RecipeManager(@NotNull TavukDonerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Konfigurasyondan alinan ayarlarla Tavuk Doner Ekmek tariflerini olusturur ve Bukkit'e kaydeder.
     */
    public synchronized void register() {
        unregister();

        int amount = Math.max(1, plugin.getConfig().getInt("wraps-per-craft", 1));
        RecipeChoice.ExactChoice sliceChoice = RecipeChoice.exactChoice(DonerItems.createSlice(1));

        String toppingName = plugin.getConfig().getString("topping-material", "BEETROOT");
        Material topping = Material.matchMaterial(toppingName != null ? toppingName : "BEETROOT");
        if (topping == null || !topping.isItem()) {
            plugin.getLogger().warning("Gecersiz topping-material: '" + toppingName + "'. Varsayilan BEETROOT kullaniliyor.");
            topping = Material.BEETROOT;
        }

        // Tier 1: Döner Dilimi + BREAD
        NamespacedKey key1 = new NamespacedKey(plugin, "wrap_tier_1");
        ShapelessRecipe r1 = new ShapelessRecipe(key1, DonerItems.createWrap(1, amount));
        r1.addIngredient(sliceChoice);
        r1.addIngredient(Material.BREAD);
        registerRecipe(r1);

        // Tier 2: Döner Dilimi + BREAD + POTATO
        NamespacedKey keyPotato = new NamespacedKey(plugin, "wrap_potato");
        ShapelessRecipe rPotato = new ShapelessRecipe(keyPotato, DonerItems.createWrap(2, amount));
        rPotato.addIngredient(sliceChoice);
        rPotato.addIngredient(Material.BREAD);
        rPotato.addIngredient(Material.POTATO);
        registerRecipe(rPotato);

        // Tier 2: Döner Dilimi + BREAD + topping-material
        NamespacedKey keyTopping = new NamespacedKey(plugin, "wrap_topping");
        if (topping != Material.POTATO) {
            ShapelessRecipe rTopping = new ShapelessRecipe(keyTopping, DonerItems.createWrap(2, amount));
            rTopping.addIngredient(sliceChoice);
            rTopping.addIngredient(Material.BREAD);
            rTopping.addIngredient(topping);
            registerRecipe(rTopping);
        }

        // Tier 3: Döner Dilimi + BREAD + POTATO + topping-material
        NamespacedKey keyFull = new NamespacedKey(plugin, "wrap_full");
        ShapelessRecipe rFull = new ShapelessRecipe(keyFull, DonerItems.createWrap(3, amount));
        rFull.addIngredient(sliceChoice);
        rFull.addIngredient(Material.BREAD);
        rFull.addIngredient(Material.POTATO);
        rFull.addIngredient(topping);
        registerRecipe(rFull);

        plugin.getLogger().info(recipeKeys.size() + " adet Tavuk Doner Ekmek tarifi basariyla kaydedildi.");
    }

    /**
     * Tekil bir sekilsiz tarifi Bukkit'e kaydeder.
     *
     * @param recipe kaydedilecek ShapelessRecipe
     */
    private void registerRecipe(@NotNull ShapelessRecipe recipe) {
        try {
            Bukkit.addRecipe(recipe);
            recipeKeys.add(recipe.getKey());
        } catch (Exception e) {
            plugin.getLogger().warning("Tarif kaydedilemedi (" + recipe.getKey() + "): " + e.getMessage());
        }
    }

    /**
     * Kayitli tum Tavuk Doner Ekmek tariflerini Bukkit'ten kaldirir.
     */
    public synchronized void unregister() {
        if (recipeKeys.isEmpty()) {
            return;
        }

        for (NamespacedKey key : recipeKeys) {
            try {
                Bukkit.removeRecipe(key);
            } catch (Exception e) {
                plugin.getLogger().warning("Tarif kaldirilamadi (" + key + "): " + e.getMessage());
            }
        }
        recipeKeys.clear();
    }

    /**
     * Konfigurasyon guncellendiginde tarifleri yeniler ve cevrimici oyunculara tekrar kesfettirir.
     */
    public synchronized void reload() {
        unregister();
        register();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("tavukdoner.use") && !recipeKeys.isEmpty()) {
                player.discoverRecipes(recipeKeys);
            }
        }
    }

    /**
     * Kayitli aktif tarif anahtarlarini dondurur.
     *
     * @return degistirilemez tarif anahtarlari listesi
     */
    public @NotNull List<NamespacedKey> getRecipeKeys() {
        return Collections.unmodifiableList(recipeKeys);
    }

    /**
     * Uretim tablosunda sonuc Tavuk Doner Ekmek ise, 'tavukdoner.use' izni bulunmayan
     * oyuncularin esyayi uretmesini engeller.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPrepareItemCraft(@NotNull PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null || !DonerItems.isWrap(result)) {
            return;
        }

        for (HumanEntity viewer : event.getViewers()) {
            if (!viewer.hasPermission("tavukdoner.use")) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    /**
     * Oyuncu sunucuya katildiginda, eger yetkisi varsa Tavuk Doner Ekmek tariflerini
     * tarif defterinde gorunur kilmak uzere kesfettirir.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("tavukdoner.use") && !recipeKeys.isEmpty()) {
            player.discoverRecipes(recipeKeys);
        }
    }
}
