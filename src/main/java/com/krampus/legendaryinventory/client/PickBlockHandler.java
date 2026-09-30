package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.inventory.ExtendedAccess;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.PickExtendedPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class PickBlockHandler {

    private PickBlockHandler() {}

    @SubscribeEvent
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || mc.level == null) {
            return;
        }
        if (!mc.options.keyPickItem.matchesMouse(event.getButton())) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        ItemStack target = targetStack(mc, player);
        if (target.isEmpty()) {
            return;
        }
        if (player.getInventory().findSlotMatchingItem(target) != -1) {
            return;
        }
        ExtendedInventory ext = ExtendedAccess.of(player);
        if (ext == null) {
            return;
        }
        int index = ExtendedAccess.findMatching(ext, target);
        if (index < 0) {
            return;
        }
        LINet.toServer(new PickExtendedPacket(index));
        event.setCanceled(true);
    }

    private static ItemStack targetStack(Minecraft mc, LocalPlayer player) {
        HitResult hit = mc.hitResult;
        if (hit == null) {
            return ItemStack.EMPTY;
        }
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
            if (state.isAir()) {
                return ItemStack.EMPTY;
            }
            return state.getCloneItemStack(blockHit, mc.level, blockHit.getBlockPos(), player);
        }
        if (hit.getType() == HitResult.Type.ENTITY) {
            ItemStack stack = ((EntityHitResult) hit).getEntity().getPickedResult(hit);
            return stack == null ? ItemStack.EMPTY : stack;
        }
        return ItemStack.EMPTY;
    }
}
