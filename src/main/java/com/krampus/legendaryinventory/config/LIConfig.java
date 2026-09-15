package com.krampus.legendaryinventory.config;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.weight.WeightRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

@EventBusSubscriber(modid = LegendaryInventory.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class LIConfig {

    public static final String FOLDER = LegendaryInventory.MODID;

    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;
    public static final Client CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    static {
        Pair<Common, ModConfigSpec> common = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = common.getLeft();
        COMMON_SPEC = common.getRight();
        Pair<Client, ModConfigSpec> client = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = client.getLeft();
        CLIENT_SPEC = client.getRight();
    }

    private LIConfig() {}

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, FOLDER + "/common.toml");
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, FOLDER + "/client.toml");
    }

    public enum WeightBarMode {
        FLASH,
        ALWAYS,
        OFF
    }

    public static final class Common {

        public final ModConfigSpec.IntValue fallbackStackable;
        public final ModConfigSpec.IntValue fallbackSemiStackable;
        public final ModConfigSpec.IntValue fallbackUnstackable;

        public final ModConfigSpec.IntValue baseCapacity;
        public final ModConfigSpec.BooleanValue maxCapacityEnabled;
        public final ModConfigSpec.IntValue maxCapacity;
        public final ModConfigSpec.BooleanValue tabletsEnabled;
        public final ModConfigSpec.IntValue tabletTier1;
        public final ModConfigSpec.IntValue tabletTier2;
        public final ModConfigSpec.IntValue tabletTier3;

        public final ModConfigSpec.DoubleValue slowStart;
        public final ModConfigSpec.DoubleValue heavy;
        public final ModConfigSpec.DoubleValue sprintBlock;
        public final ModConfigSpec.DoubleValue stall;

        public final ModConfigSpec.BooleanValue blockPickupWhenFull;

        public final ModConfigSpec.BooleanValue logMenuAugment;
        public final ModConfigSpec.BooleanValue waystonesBlockWhenStalled;
        public final ModConfigSpec.BooleanValue firstRowCue;
        public final ModConfigSpec.BooleanValue sortEnabled;
        public final ModConfigSpec.DoubleValue mountBlockRatio;
        public final ModConfigSpec.BooleanValue mountBlockVehicles;
        public final ModConfigSpec.BooleanValue mountAnimalPenalty;

        Common(ModConfigSpec.Builder b) {
            b.comment(
                "Per item weights live in " + FOLDER + "/weights.json.",
                "Items missing from that file use the fallback values below, based on max stack size.",
                "Edit weights.json then run /inventory reload to apply without restarting."
            ).push("weight");
            fallbackStackable = b.comment("Items that stack to 64 or more").defineInRange("fallbackStackable", 1, 0, 100000);
            fallbackSemiStackable = b.comment("Items that stack to between 2 and 63").defineInRange("fallbackSemiStackable", 4, 0, 100000);
            fallbackUnstackable = b.comment("Items that do not stack").defineInRange("fallbackUnstackable", 24, 0, 100000);
            b.pop();

            b.push("capacity");
            baseCapacity = b.comment("Carry capacity in pods before any penalty").defineInRange("base", 1000, 1, 1000000);
            maxCapacityEnabled = b.comment(
                "true: capacity cannot be raised above the max value below.",
                "false: no limit."
            ).define("maxEnabled", true);
            maxCapacity = b.comment("Maximum carry capacity in pods, including bonuses from tablets")
                .defineInRange("max", 10000, 1, 1000000);
            b.pop();

            b.push("tablets");
            tabletsEnabled = b.comment(
                "Enable the weight tablets: dungeon loot that permanently raises carry capacity.",
                "false: tablets do nothing when used, do not appear in loot or the creative tab."
            ).define("enabled", true);
            tabletTier1 = b.comment("Pods granted by Weight Tablet I").defineInRange("tier1", 20, 1, 1000000);
            tabletTier2 = b.comment("Pods granted by Weight Tablet II").defineInRange("tier2", 50, 1, 1000000);
            tabletTier3 = b.comment("Pods granted by Weight Tablet III").defineInRange("tier3", 100, 1, 1000000);
            b.pop();

            b.comment("Ratios of carried weight over capacity. 1.0 means 100%. Keep them in ascending order.").push("penalty");
            slowStart = b.comment("Above this ratio the player starts slowing down").defineInRange("slowStart", 1.00D, 0.0D, 100.0D);
            heavy = b.comment("Above this ratio the bar turns to the heavy color and jumps get shorter").defineInRange("heavy", 1.20D, 0.0D, 100.0D);
            sprintBlock = b.comment("Above this ratio sprinting is disabled").defineInRange("sprintBlock", 1.50D, 0.0D, 100.0D);
            stall = b.comment("Above this ratio the player cannot move or jump").defineInRange("stall", 2.00D, 0.0D, 100.0D);
            b.pop();

            b.push("pickup");
            blockPickupWhenFull = b.comment(
                "true: stop picking up items once carried weight reaches 100% of capacity.",
                "false: only stop once the stall ratio is reached."
            ).define("blockWhenFull", false);
            b.pop();

            b.push("mounts");
            mountBlockRatio = b.comment(
                "Load ratio (carried / capacity) from which the player can no longer mount.",
                "1.5 means 150% of capacity."
            ).defineInRange("blockRatio", 1.5D, 0.01D, 100.0D);
            mountBlockVehicles = b.comment(
                "true: the mount block also applies to vehicles such as boats and minecarts.",
                "false: only living mounts are blocked, vehicles can always be entered."
            ).define("blockVehicles", true);
            mountAnimalPenalty = b.comment(
                "Living mounts carrying an overloaded player move with the same speed penalty the player would have on foot."
            ).define("animalPenalty", true);
            b.pop();

            b.push("sort");
            sortEnabled = b.comment(
                "Enable the sort button and the sort keybind in inventories and containers."
            ).define("enabled", true);
            b.pop();

            b.push("hints");
            firstRowCue = b.comment(
                "The first time a player fills a 4th inventory row, show an action bar hint",
                "and briefly scroll the inventory when it is next opened."
            ).define("firstRowCue", true);
            b.pop();

            b.push("compat");
            waystonesBlockWhenStalled = b.comment(
                "Waystones: block teleporting through any waystone, warp stone or scroll while immobilised by weight."
            ).define("waystonesBlockWhenStalled", true);
            b.pop();

            b.push("debug");
            logMenuAugment = b.comment("Log one line per container opened, saying whether it received scroll rows and why not").define("logMenuAugment", false);
            b.pop();
        }
    }

    public static final class Client {

        public final ModConfigSpec.EnumValue<WeightBarMode> weightBar;
        public final ModConfigSpec.BooleanValue itemTooltipWeight;
        public final ModConfigSpec.BooleanValue scrollBarVisible;

        Client(ModConfigSpec.Builder b) {
            b.push("hud");
            weightBar = b.comment(
                "FLASH: the weight bar replaces the XP bar for a moment when the weight changes and while stalled.",
                "ALWAYS: the weight bar permanently replaces the XP bar.",
                "OFF: never show the weight bar."
            ).defineEnum("weightBar", WeightBarMode.FLASH);
            b.pop();

            b.push("tooltip");
            itemTooltipWeight = b.comment(
                "Show the weight number and icon next to the item name in item tooltips."
            ).define("itemWeight", true);
            b.pop();

            b.push("inventory");
            scrollBarVisible = b.comment(
                "Show the scroll bar next to the extended inventory rows. Toggled in game with the button on the weight bar."
            ).define("scrollBarVisible", false);
            b.pop();
        }
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == COMMON_SPEC) {
            WeightRules.loadLocal();
        }
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == COMMON_SPEC) {
            WeightRules.reloadAndSync();
        }
    }
}
