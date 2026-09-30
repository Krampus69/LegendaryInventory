package com.krampus.legendaryinventory.compat;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.client.AugmentedScreens;
import com.krampus.legendaryinventory.client.InventoryScrollBar;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class ClientSortCompat {

    private static final String WIDGET_PACKAGE = "dev.terminalmc.clientsort.client.gui.widget.";
    private static final String BUTTON_CLASS = WIDGET_PACKAGE + "TriggerButton";
    private static final String VEC_CLASS = "dev.terminalmc.clientsort.client.config.Vec2i";
    private static final int GAP = 2;
    private static final Map<AbstractWidget, int[]> BASE = new WeakHashMap<>();
    private static final Map<AbstractWidget, Integer> APPLIED = new WeakHashMap<>();

    private static boolean resolved;
    private static MethodHandle getOffset;
    private static MethodHandle setOffset;
    private static MethodHandle vecX;
    private static MethodHandle vecY;
    private static MethodHandle newVec;

    private ClientSortCompat() {}

    private static boolean resolve() {
        if (resolved) {
            return getOffset != null;
        }
        resolved = true;
        try {
            Class<?> button = Class.forName(BUTTON_CLASS);
            Class<?> vec = Class.forName(VEC_CLASS);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            getOffset = lookup.findGetter(button, "offset", vec);
            setOffset = lookup.findSetter(button, "offset", vec);
            vecX = lookup.findVirtual(vec, "x", MethodType.methodType(int.class));
            vecY = lookup.findVirtual(vec, "y", MethodType.methodType(int.class));
            newVec = lookup.findConstructor(vec, MethodType.methodType(void.class, int.class, int.class));
        } catch (ReflectiveOperationException | RuntimeException e) {
            getOffset = null;
        }
        return getOffset != null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInit(ScreenEvent.Init.Post event) {
        reposition(event.getScreen());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        reposition(event.getScreen());
    }

    private static void reposition(Screen raw) {
        AbstractContainerScreen<?> screen = AugmentedScreens.of(raw);
        if (screen == null || !resolve()) {
            return;
        }
        int shift = InventoryScrollBar.shownOn(screen) ? InventoryScrollBar.width() + GAP : 0;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget && widget.getClass().getName().startsWith(WIDGET_PACKAGE)) {
                apply(widget, shift);
            }
        }
    }

    private static void apply(AbstractWidget widget, int shift) {
        try {
            int[] base = BASE.get(widget);
            if (base == null) {
                Object offset = getOffset.invoke(widget);
                base = new int[] {(int) vecX.invoke(offset), (int) vecY.invoke(offset)};
                BASE.put(widget, base);
            }
            Integer applied = APPLIED.get(widget);
            if (applied != null && applied == shift) {
                return;
            }
            setOffset.invoke(widget, newVec.invoke(base[0] + shift, base[1]));
            APPLIED.put(widget, shift);
        } catch (Throwable ignored) {
        }
    }
}