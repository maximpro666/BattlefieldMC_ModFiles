package com.yourmod.teamsystem.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Timer;
import java.util.TimerTask;
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
                tryToOpen(0);
            });
        });
        return true;
    }

    private static void tryToOpen(int attempt) {
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("getInstance").invoke(null);
            Object player = mcClass.getMethod("getPlayer").invoke(mc);
            Object level  = mcClass.getMethod("getLevel").invoke(mc);
            if (player == null || level == null) {
                if (attempt < 5) {
                    int delay = 1000 * (attempt + 1);
                    int next = attempt + 1;
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            tryToOpen(next);
                        }
                    }, delay);
                }
                return;
            }
            Class<?> screenClass = Class.forName("com.yourmod.teamsystem.client.gui.screen.TeamSelectionScreen");
            Object screen = screenClass.getConstructor().newInstance();
            mcClass.getMethod("setScreen", Class.forName("net.minecraft.client.gui.screens.Screen")).invoke(mc, screen);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
