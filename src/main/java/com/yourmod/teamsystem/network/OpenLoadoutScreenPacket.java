package com.yourmod.teamsystem.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenLoadoutScreenPacket {

    public OpenLoadoutScreenPacket() {
    }

    public OpenLoadoutScreenPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    Class<?> mcClazz = Class.forName("net.minecraft.client.Minecraft");
                    Object mc = mcClazz.getMethod("getInstance").invoke(null);
                    Object mcLevel = mcClazz.getMethod("getLevel").invoke(mc);
                    Object mcPlayer = mcClazz.getMethod("getPlayer").invoke(mc);
                    if (mcPlayer == null || mcLevel == null) return;

                    Object screen = Class.forName("com.yourmod.teamsystem.client.gui.screen.ClassSelectionScreen")
                        .getConstructor().newInstance();

                    mcClazz.getMethod("setScreen",
                        Class.forName("net.minecraft.client.gui.screens.Screen"))
                        .invoke(mc, screen);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
        return true;
    }
}
