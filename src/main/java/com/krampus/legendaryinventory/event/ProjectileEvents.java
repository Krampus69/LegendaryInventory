package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.inventory.ExtendedAccess;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraftforge.event.entity.living.LivingGetProjectileEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID)
public final class ProjectileEvents {

    private ProjectileEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGetProjectile(LivingGetProjectileEvent event) {
        if (!event.getProjectileItemStack().isEmpty()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getProjectileWeaponItemStack().getItem() instanceof ProjectileWeaponItem weapon)) {
            return;
        }
        ExtendedInventory ext = ExtendedAccess.of(player);
        if (ext == null) {
            return;
        }
        int slot = ExtendedAccess.findMatching(ext, weapon.getAllSupportedProjectiles());
        if (slot < 0) {
            return;
        }
        event.setProjectileItemStack(ext.getStackInSlot(slot));
        if (!player.level().isClientSide()) {
            ext.markChanged(slot);
        }
    }
}
