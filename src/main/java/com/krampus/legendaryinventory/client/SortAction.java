package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.SortPacket;

public final class SortAction {

    private SortAction() {}

    public static void send(boolean byWeight) {
        LINet.toServer(new SortPacket(byWeight));
    }
}
