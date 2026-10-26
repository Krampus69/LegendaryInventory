package com.krampus.legendaryinventory.compat;

import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class LSOCompat {

    public static final String MOD_ID = "legendarysurvivaloverhaul";

    private static final String BAKED = "sfiomn.legendarysurvivaloverhaul.config.Config$Baked";
    private static final String CAPABILITY_UTIL = "sfiomn.legendarysurvivaloverhaul.util.CapabilityUtil";

    private static final int MANNEQUIN_X = 92;
    private static final int MANNEQUIN_Y = 33;
    private static final int MANNEQUIN_W = 16;
    private static final int MANNEQUIN_H = 32;
    private static final int OFFHAND_SHIFT = 31;

    private static boolean resolved = false;
    private static boolean available = false;
    private static Field enabled;
    private static Field offsetX;
    private static Field offsetY;
    private static Field healthLimit;
    private static Method capability;
    private static Method wounded;

    private static Player cachedPlayer = null;
    private static int cachedTick = -1;
    private static boolean cachedVisible = false;

    private LSOCompat() {}

    public static int[] mannequinBounds(Player player, int width, int height) {
        if (!resolve() || !visible(player)) {
            return null;
        }
        try {
            int dx = offsetX.getInt(null);
            int dy = offsetY.getInt(null);
            int x = width / 2 + MANNEQUIN_X + dx;
            int y = height - MANNEQUIN_Y + dy;
            if (!player.getOffhandItem().isEmpty() && player.getMainArm() == HumanoidArm.LEFT && dx == 0 && dy == 0) {
                x += OFFHAND_SHIFT;
            }
            return new int[] {x, y, MANNEQUIN_W, MANNEQUIN_H};
        } catch (ReflectiveOperationException | RuntimeException e) {
            disable(e);
            return null;
        }
    }

    private static boolean resolve() {
        if (resolved) {
            return available;
        }
        resolved = true;
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }
        try {
            Class<?> baked = Class.forName(BAKED);
            enabled = baked.getField("localizedBodyDamageEnabled");
            offsetX = baked.getField("bodyDamageIndicatorOffsetX");
            offsetY = baked.getField("bodyDamageIndicatorOffsetY");
            healthLimit = baked.getField("bodyDamageIndicatorRenderHealthLimit");
            capability = Class.forName(CAPABILITY_UTIL).getMethod("getBodyDamageCapability", Player.class);
            wounded = capability.getReturnType().getMethod("isWoundedBelow", float.class);
            available = true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            disable(e);
        }
        return available;
    }

    private static boolean visible(Player player) {
        if (player == cachedPlayer && player.tickCount == cachedTick) {
            return cachedVisible;
        }
        cachedPlayer = player;
        cachedTick = player.tickCount;
        cachedVisible = query(player);
        return cachedVisible;
    }

    private static boolean query(Player player) {
        try {
            if (!enabled.getBoolean(null)) {
                return false;
            }
            Object cap = capability.invoke(null, player);
            if (cap == null) {
                return false;
            }
            return (Boolean) wounded.invoke(cap, (float) healthLimit.getDouble(null));
        } catch (ReflectiveOperationException | RuntimeException e) {
            disable(e);
            return false;
        }
    }

    private static void disable(Exception e) {
        available = false;
        cachedPlayer = null;
        LegendaryInventory.LOGGER.warn("Could not read the LSO body damage indicator, the weight icon will ignore it", e);
    }
}
