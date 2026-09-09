package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.inventory.LICaps;
import com.krampus.legendaryinventory.inventory.RecipePlacer;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RecipeTransferPacket {

    private final ResourceLocation recipeId;
    private final boolean maxTransfer;

    public RecipeTransferPacket(ResourceLocation recipeId, boolean maxTransfer) {
        this.recipeId = recipeId;
        this.maxTransfer = maxTransfer;
    }

    public RecipeTransferPacket(FriendlyByteBuf buf) {
        this.recipeId = buf.readResourceLocation();
        this.maxTransfer = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(recipeId);
        buf.writeBoolean(maxTransfer);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            apply(player);
        }
        ctx.get().setPacketHandled(true);
    }

    private void apply(ServerPlayer player) {
        if (!(player.containerMenu instanceof RecipeBookMenu<?> menu) || !ScrollRegistry.has(menu)) {
            return;
        }
        Recipe<?> recipe = player.server.getRecipeManager().byKey(recipeId).orElse(null);
        if (!(recipe instanceof CraftingRecipe crafting)
            || !crafting.canCraftInDimensions(menu.getGridWidth(), menu.getGridHeight())) {
            return;
        }
        RecipePlacer.place(player, menu, LICaps.get(player), crafting, maxTransfer);
        menu.broadcastChanges();
    }
}
