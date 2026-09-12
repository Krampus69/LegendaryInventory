package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.inventory.LIAttachments;
import com.krampus.legendaryinventory.inventory.RecipePlacer;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RecipeTransferPacket(ResourceLocation recipeId, boolean maxTransfer) implements CustomPacketPayload {

    public static final Type<RecipeTransferPacket> TYPE = new Type<>(LINet.id("recipe_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeTransferPacket> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, RecipeTransferPacket::recipeId,
        ByteBufCodecs.BOOL, RecipeTransferPacket::maxTransfer,
        RecipeTransferPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RecipeTransferPacket packet, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.containerMenu instanceof RecipeBookMenu<?, ?> menu) || !ScrollRegistry.has(menu)) {
            return;
        }
        RecipeHolder<?> holder = player.server.getRecipeManager().byKey(packet.recipeId()).orElse(null);
        if (holder == null || !(holder.value() instanceof CraftingRecipe crafting)
            || !crafting.canCraftInDimensions(menu.getGridWidth(), menu.getGridHeight())) {
            return;
        }
        RecipePlacer.place(player, menu, LIAttachments.extended(player), crafting, packet.maxTransfer());
        menu.broadcastChanges();
    }
}
