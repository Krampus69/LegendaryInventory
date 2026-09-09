package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class LINet {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(LegendaryInventory.MODID, "main"),
        () -> VERSION,
        VERSION::equals,
        VERSION::equals
    );

    private static int nextId = 0;

    private LINet() {}

    public static void init() {
        CHANNEL.messageBuilder(OpenPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(OpenPacket::encode).decoder(OpenPacket::new).consumerMainThread(OpenPacket::handle).add();

        CHANNEL.messageBuilder(ScrollPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ScrollPacket::encode).decoder(ScrollPacket::new).consumerMainThread(ScrollPacket::handle).add();

        CHANNEL.messageBuilder(FilterPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(FilterPacket::encode).decoder(FilterPacket::new).consumerMainThread(FilterPacket::handle).add();

        CHANNEL.messageBuilder(SortPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(SortPacket::encode).decoder(SortPacket::new).consumerMainThread(SortPacket::handle).add();

        CHANNEL.messageBuilder(MirrorPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(MirrorPacket::encode).decoder(MirrorPacket::new).consumerMainThread(MirrorPacket::handle).add();

        CHANNEL.messageBuilder(WeightPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(WeightPacket::encode).decoder(WeightPacket::new).consumerMainThread(WeightPacket::handle).add();

        CHANNEL.messageBuilder(RulesSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(RulesSyncPacket::encode).decoder(RulesSyncPacket::new).consumerMainThread(RulesSyncPacket::handle).add();

        CHANNEL.messageBuilder(IntroCuePacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(IntroCuePacket::encode).decoder(IntroCuePacket::new).consumerMainThread(IntroCuePacket::handle).add();

        CHANNEL.messageBuilder(PickupNotifyPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(PickupNotifyPacket::encode).decoder(PickupNotifyPacket::new).consumerMainThread(PickupNotifyPacket::handle).add();

        CHANNEL.messageBuilder(ClearExtendedPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ClearExtendedPacket::encode).decoder(ClearExtendedPacket::new).consumerMainThread(ClearExtendedPacket::handle).add();

        CHANNEL.messageBuilder(CollectPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(CollectPacket::encode).decoder(CollectPacket::new).consumerMainThread(CollectPacket::handle).add();

        CHANNEL.messageBuilder(RecipeTransferPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(RecipeTransferPacket::encode).decoder(RecipeTransferPacket::new).consumerMainThread(RecipeTransferPacket::handle).add();
    }

    public static void toPlayer(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void toServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
