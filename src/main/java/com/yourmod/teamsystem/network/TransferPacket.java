package com.yourmod.teamsystem.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class TransferPacket {
    private final String address;

    public TransferPacket(String address) {
        this.address = address;
    }

    public TransferPacket(FriendlyByteBuf buf) {
        this.address = buf.readUtf(32767);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(address);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            ServerAddress serverAddress = ServerAddress.parseString(address);
            ServerData serverData = new ServerData(address, address, false);
            ConnectScreen.startConnecting(null, mc, serverAddress, serverData, false);
        });
        ctx.get().setPacketHandled(true);
    }
}
