package com.yourmod.teamsystem.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleCreditsSyncPacket {
    private int natoVC;
    private int russiaVC;

    public VehicleCreditsSyncPacket(int natoVC, int russiaVC) {
        this.natoVC = natoVC;
        this.russiaVC = russiaVC;
    }

    public VehicleCreditsSyncPacket(FriendlyByteBuf buf) {
        this.natoVC = buf.readInt();
        this.russiaVC = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(natoVC);
        buf.writeInt(russiaVC);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClientSide(natoVC, russiaVC));
        });
        return true;
    }

    private static void handleClientSide(int natoVC, int russiaVC) {
        com.yourmod.teamsystem.client.ClientTeamData.natoVC = natoVC;
        com.yourmod.teamsystem.client.ClientTeamData.russiaVC = russiaVC;
    }
}
