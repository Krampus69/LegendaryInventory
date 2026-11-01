package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.inventory.ExtendedAccess;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;

@EventBusSubscriber(modid = LegendaryInventory.MODID)
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
        ItemStack weaponStack = event.getProjectileWeaponItemStack();
        if (!(weaponStack.getItem() instanceof ProjectileWeaponItem weapon)) {
            return;
        }
        ExtendedInventory ext = ExtendedAccess.of(player);
        if (ext == null) {
            return;
        }
        int slot = ExtendedAccess.findMatching(ext, weapon.getAllSupportedProjectiles(weaponStack));
        if (slot < 0) {
            return;
        }
        event.setProjectileItemStack(ext.getStackInSlot(slot));
        if (!player.level().isClientSide()) {
            ext.markChanged(slot);
        }
    }
}
