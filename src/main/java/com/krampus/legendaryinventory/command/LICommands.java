package com.krampus.legendaryinventory.command;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.inventory.LIAttachments;
import com.krampus.legendaryinventory.item.SackStorage;
import com.krampus.legendaryinventory.weight.CapacityBonus;
import com.krampus.legendaryinventory.weight.CarryLoad;
import com.krampus.legendaryinventory.weight.WeightRules;
import com.krampus.legendaryinventory.weight.WeightTable;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = LegendaryInventory.MODID)
public final class LICommands {

    private LICommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("inventory")
            .requires(source -> source.hasPermission(2));

        root.then(Commands.literal("clear")
            .executes(ctx -> clear(ctx.getSource(), ctx.getSource().getPlayerOrException()))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> clear(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));

        root.then(Commands.literal("info")
            .executes(ctx -> info(ctx.getSource(), ctx.getSource().getPlayerOrException()))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> info(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));

        root.then(Commands.literal("sacks")
            .executes(ctx -> sacks(ctx.getSource())));

        root.then(Commands.literal("reload")
            .executes(ctx -> reload(ctx.getSource())));

        root.then(Commands.literal("capacity")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> showCapacity(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                .then(Commands.argument("total", IntegerArgumentType.integer(1))
                    .executes(ctx -> setCapacity(ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player"),
                        IntegerArgumentType.getInteger(ctx, "total"))))
                .then(Commands.literal("reset")
                    .executes(ctx -> resetCapacity(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))));

        root.then(Commands.literal("set")
            .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                .then(Commands.argument("weight", IntegerArgumentType.integer(0))
                    .executes(ctx -> set(ctx.getSource(),
                        ItemArgument.getItem(ctx, "item").getItem(),
                        IntegerArgumentType.getInteger(ctx, "weight"))))));

        event.getDispatcher().register(root);
    }

    private static int showCapacity(CommandSourceStack source, ServerPlayer target) {
        int base = CapacityBonus.base(target);
        int bonus = CapacityBonus.get(target);
        int total = CarryLoad.capacity(target);
        source.sendSuccess(() -> Component.literal(
            target.getName().getString() + ": " + total + " pods (base " + base + ", bonus " + bonus + ")"), false);
        return total;
    }

    private static int setCapacity(CommandSourceStack source, ServerPlayer target, int total) {
        CapacityBonus.setTotal(target, total);
        source.sendSuccess(() -> Component.literal(
            target.getName().getString() + " capacity set to " + total + " pods"), true);
        return total;
    }

    private static int resetCapacity(CommandSourceStack source, ServerPlayer target) {
        CapacityBonus.set(target, 0);
        int total = CarryLoad.capacity(target);
        source.sendSuccess(() -> Component.literal(
            target.getName().getString() + " bonus cleared, capacity " + total + " pods"), true);
        return total;
    }

    private static int set(CommandSourceStack source, Item item, int weight) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            source.sendFailure(Component.literal("Unknown item"));
            return 0;
        }
        int before = WeightTable.of(new ItemStack(item));
        if (!WeightRules.setOverride(id, weight)) {
            source.sendFailure(Component.literal("Could not write weights.json"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
            id + ": " + before + " -> " + weight + " pods"), true);
        return weight;
    }

    private static int reload(CommandSourceStack source) {
        WeightRules.reloadAndSync();
        int count = WeightRules.current().overrides().size();
        source.sendSuccess(() -> Component.literal(
            "Reloaded weights.json: " + count + " item overrides"), true);
        return count;
    }

    private static int sacks(CommandSourceStack source) {
        int stored = SackStorage.get(source.getServer()).size();
        source.sendSuccess(() -> Component.literal(
            "Stored sacks: " + stored), false);
        return stored;
    }

    private static int clear(CommandSourceStack source, ServerPlayer target) {
        ExtendedInventory extended = LIAttachments.extended(target);
        int cleared = extended.usedSlots();
        extended.clearAll();

        Inventory inventory = target.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (!inventory.getItem(i).isEmpty()) {
                cleared++;
            }
        }
        inventory.clearContent();
        target.containerMenu.setCarried(ItemStack.EMPTY);
        target.inventoryMenu.getCraftSlots().clearContent();
        cleared += ExternalInventories.clearAll(target);
        target.inventoryMenu.broadcastChanges();
        target.containerMenu.broadcastChanges();

        int total = cleared;
        source.sendSuccess(() -> Component.literal(
            "Cleared " + total + " slots from " + target.getName().getString()), true);
        return total;
    }

    private static int info(CommandSourceStack source, ServerPlayer target) {
        ExtendedInventory extended = LIAttachments.extended(target);
        int used = extended.usedSlots();
        int total = extended.getSlots();
        int weight = CarryLoad.carriedWeight(target);
        int capacity = CarryLoad.capacity(target);

        source.sendSuccess(() -> Component.literal(
            target.getName().getString() + ": extended " + used + "/" + total
                + " slots, weight " + weight + "/" + capacity), false);
        return used;
    }
}
