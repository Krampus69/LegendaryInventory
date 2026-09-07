package com.krampus.legendaryinventory.inventory;

import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class LIAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LegendaryInventory.MODID);

    public static final Supplier<AttachmentType<ExtendedInventory>> EXTENDED = ATTACHMENTS.register(
        "extended_inventory",
        () -> AttachmentType.serializable(ExtendedInventory::new).copyOnDeath().build()
    );

    public static final Supplier<AttachmentType<WeightProfile>> PROFILE = ATTACHMENTS.register(
        "profile",
        () -> AttachmentType.builder(() -> WeightProfile.DEFAULT).serialize(WeightProfile.CODEC).copyOnDeath().build()
    );

    private LIAttachments() {}

    public static ExtendedInventory extended(Player player) {
        return player.getData(EXTENDED);
    }

    public static WeightProfile profile(Player player) {
        return player.getData(PROFILE);
    }

    public static void setProfile(Player player, WeightProfile profile) {
        player.setData(PROFILE, profile);
    }
}
