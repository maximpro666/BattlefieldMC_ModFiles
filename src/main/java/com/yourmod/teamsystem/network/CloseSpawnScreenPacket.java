package com.yourmod.teamsystem.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CloseSpawnScreenPacket {

    public CloseSpawnScreenPacket() {}

    public CloseSpawnScreenPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    Class<?> helper = Class.forName("com.yourmod.teamsystem.client.gui.screen.SpawnScreenHelper");
                    helper.getMethod("closeSpawnScreen").invoke(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            })
        );
        context.setPacketHandled(true);
        return true;
    }
}
