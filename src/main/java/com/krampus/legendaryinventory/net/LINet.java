package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.client.ClientPackets;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = LegendaryInventory.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class LINet {

    private static final String VERSION = "1";

    private LINet() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, path);
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        registrar.playToServer(OpenPacket.TYPE, OpenPacket.STREAM_CODEC, OpenPacket::handle);
        registrar.playToServer(ScrollPacket.TYPE, ScrollPacket.STREAM_CODEC, ScrollPacket::handle);
        registrar.playToServer(SortPacket.TYPE, SortPacket.STREAM_CODEC, SortPacket::handle);
        registrar.playToServer(FilterPacket.TYPE, FilterPacket.STREAM_CODEC, FilterPacket::handle);
        registrar.playToServer(ClearExtendedPacket.TYPE, ClearExtendedPacket.STREAM_CODEC, ClearExtendedPacket::handle);
        registrar.playToServer(CollectPacket.TYPE, CollectPacket.STREAM_CODEC, CollectPacket::handle);
        registrar.playToServer(RecipeTransferPacket.TYPE, RecipeTransferPacket.STREAM_CODEC, RecipeTransferPacket::handle);

        registrar.playToClient(WeightPacket.TYPE, WeightPacket.STREAM_CODEC, ClientPackets::handleWeight);
        registrar.playToClient(MirrorPacket.TYPE, MirrorPacket.STREAM_CODEC, ClientPackets::handleMirror);
        registrar.playToClient(IntroCuePacket.TYPE, IntroCuePacket.STREAM_CODEC, ClientPackets::handleIntroCue);
        registrar.playToClient(RulesSyncPacket.TYPE, RulesSyncPacket.STREAM_CODEC, RulesSyncPacket::handle);
    }

    public static void toPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void toServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
