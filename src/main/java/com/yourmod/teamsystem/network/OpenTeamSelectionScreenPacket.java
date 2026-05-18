package com.yourmod.teamsystem.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenTeamSelectionScreenPacket {

    public OpenTeamSelectionScreenPacket() {
    }

    public OpenTeamSelectionScreenPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
                    Object mc = mcClass.getMethod("getInstance").invoke(null);
                    if (mcClass.getMethod("getPlayer").invoke(mc) == null) return;
                    if (mcClass.getMethod("getLevel").invoke(mc) == null) return;
                    Class<?> screenClass = Class.forName("com.yourmod.teamsystem.client.gui.screen.TeamSelectionScreen");
                    Object screen = screenClass.getConstructor().newInstance();
                    mcClass.getMethod("setScreen", Class.forName("net.minecraft.client.gui.screens.Screen")).invoke(mc, screen);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
        return true;
    }
}
