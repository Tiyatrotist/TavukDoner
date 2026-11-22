package dev.tavukdoner.protection;

import dev.tavukdoner.Messages;
import dev.tavukdoner.TavukDonerPlugin;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class Protection {

    private static TavukDonerPlugin plugin;
    private static boolean gpPresent = false;
    private static Object gpInstance;
    private static Object dataStore;
    private static Method getClaimAtMethod;
    private static boolean getClaimAtFourArgs = false;
    private static Method allowBuildMethod;
    private static Method checkPermMethod;
    private static Object buildPermission;
    private static final AtomicBoolean warningLogged = new AtomicBoolean(false);

    private Protection() {
    }

    public static void init(@NotNull TavukDonerPlugin pl) {
        plugin = pl;
        warningLogged.set(false);
        gpPresent = false;
        gpInstance = null;
        dataStore = null;
        getClaimAtMethod = null;
        getClaimAtFourArgs = false;
        allowBuildMethod = null;
        checkPermMethod = null;
        buildPermission = null;

        Plugin gpPlugin = pl.getServer().getPluginManager().getPlugin("GriefPrevention");
        if (gpPlugin == null || !gpPlugin.isEnabled()) {
            pl.getLogger().info("GriefPrevention bulunamadı, claim koruması kapalı");
            return;
        }

        try {
            Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");

            Field instanceField;
            try {
                instanceField = gpClass.getField("instance");
            } catch (NoSuchFieldException e) {
                instanceField = gpClass.getDeclaredField("instance");
            }
            instanceField.setAccessible(true);
            Object instance = instanceField.get(null);
            if (instance == null) {
                instance = gpPlugin;
            }
            gpInstance = instance;

            Field dataStoreField;
            try {
                dataStoreField = gpClass.getField("dataStore");
            } catch (NoSuchFieldException e) {
                dataStoreField = gpClass.getDeclaredField("dataStore");
            }
            dataStoreField.setAccessible(true);
            dataStore = dataStoreField.get(gpInstance);

            if (dataStore == null) {
                throw new IllegalStateException("dataStore field is null on GriefPrevention instance");
            }

            Class<?> claimClass = Class.forName("me.ryanhamshire.GriefPrevention.Claim");
            Class<?> dataStoreClass = dataStore.getClass();

            Method claimMethod = null;
            boolean fourArgs = false;
            try {
                claimMethod = dataStoreClass.getMethod("getClaimAt", Location.class, boolean.class, claimClass);
                fourArgs = false;
            } catch (NoSuchMethodException e1) {
                try {
                    claimMethod = dataStoreClass.getMethod("getClaimAt", Location.class, boolean.class, boolean.class, claimClass);
                    fourArgs = true;
                } catch (NoSuchMethodException e2) {
                    for (Method m : dataStoreClass.getMethods()) {
                        if ("getClaimAt".equals(m.getName())) {
                            Class<?>[] params = m.getParameterTypes();
                            if (params.length == 3 && Location.class.isAssignableFrom(params[0])) {
                                claimMethod = m;
                                fourArgs = false;
                                break;
                            } else if (params.length == 4 && Location.class.isAssignableFrom(params[0])) {
                                claimMethod = m;
                                fourArgs = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (claimMethod == null) {
                throw new NoSuchMethodException("getClaimAt method not found on " + dataStoreClass.getName());
            }
            claimMethod.setAccessible(true);
            getClaimAtMethod = claimMethod;
            getClaimAtFourArgs = fourArgs;

            // Modern GriefPrevention (16.x+): checkPermission(Player, ClaimPermission, Event) -> Supplier<String>
            try {
                Class<?> permClass = Class.forName("me.ryanhamshire.GriefPrevention.ClaimPermission");
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object build = Enum.valueOf((Class<Enum>) permClass.asSubclass(Enum.class), "Build");
                checkPermMethod = claimClass.getMethod("checkPermission", Player.class, permClass, org.bukkit.event.Event.class);
                buildPermission = build;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                checkPermMethod = null;
                buildPermission = null;
            }

            Method abMethod = null;
            try {
                abMethod = claimClass.getMethod("allowBuild", Player.class, Material.class);
            } catch (NoSuchMethodException e) {
                for (Method m : claimClass.getMethods()) {
                    if ("allowBuild".equals(m.getName()) && m.getParameterCount() == 2) {
                        Class<?>[] params = m.getParameterTypes();
                        if (Player.class.isAssignableFrom(params[0]) && Material.class.isAssignableFrom(params[1])) {
                            abMethod = m;
                            break;
                        }
                    }
                }
            }

            if (abMethod == null && checkPermMethod == null) {
                throw new NoSuchMethodException("checkPermission/allowBuild not found on " + claimClass.getName());
            }
            if (abMethod != null) {
                abMethod.setAccessible(true);
            }
            allowBuildMethod = abMethod;

            gpPresent = true;
            pl.getLogger().info("GriefPrevention entegrasyonu aktif (claim koruması açık).");
        } catch (Throwable t) {
            gpPresent = false;
            logWarningOnce(t.getMessage());
        }
    }

    public static boolean canModify(@NotNull Player player, @NotNull Location location) {
        if (player.hasPermission("tavukdoner.admin")) {
            return true;
        }

        if (!gpPresent || dataStore == null || getClaimAtMethod == null
                || (allowBuildMethod == null && checkPermMethod == null)) {
            return true;
        }

        try {
            Object claim;
            if (getClaimAtFourArgs) {
                claim = getClaimAtMethod.invoke(dataStore, location, false, false, null);
            } else {
                claim = getClaimAtMethod.invoke(dataStore, location, false, null);
            }

            if (claim == null) {
                return true;
            }

            Material rodMaterial = Material.BLAZE_ROD;
            if (plugin != null) {
                String rodName = plugin.getConfig().getString("rod-material", "BLAZE_ROD");
                Material matched = Material.matchMaterial(rodName);
                if (matched != null) {
                    rodMaterial = matched;
                }
            }

            Object result = checkPermMethod != null
                    ? checkPermMethod.invoke(claim, player, buildPermission, null)
                    : allowBuildMethod.invoke(claim, player, rodMaterial);
            if (result == null) {
                return true;
            }

            String reason;
            if (result instanceof Supplier<?> supplier) {
                Object supplied = supplier.get();
                if (supplied == null) {
                    return true;
                }
                reason = String.valueOf(supplied);
            } else {
                reason = String.valueOf(result);
            }

            if (reason.isBlank()) {
                reason = "GriefPrevention claim koruması.";
            }

            player.sendMessage(Messages.get("protection.denied", Placeholder.unparsed("reason", reason)));
            return false;
        } catch (Throwable t) {
            logWarningOnce(t.getMessage());
            return true;
        }
    }

    private static void logWarningOnce(@Nullable String detail) {
        if (warningLogged.compareAndSet(false, true)) {
            String msg = "GriefPrevention bulundu ama API uyumsuz, claim koruması ÇALIŞMIYOR";
            if (detail != null && !detail.isBlank()) {
                msg = msg + " (" + detail + ")";
            }
            if (plugin != null) {
                plugin.getLogger().warning(msg);
            } else {
                Bukkit.getLogger().warning("[TavukDoner] " + msg);
            }
        }
    }

    public static boolean isGriefPreventionPresent() {
        return gpPresent;
    }
}
