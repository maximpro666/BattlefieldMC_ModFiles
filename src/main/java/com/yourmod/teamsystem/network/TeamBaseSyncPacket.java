package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.ClientTeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TeamBaseSyncPacket {
    private final int natoX, natoY, natoZ;
    private final int russiaX, russiaY, russiaZ;
    private final int baseRadius;

    public TeamBaseSyncPacket(int natoX, int natoY, int natoZ, int russiaX, int russiaY, int russiaZ, int baseRadius) {
        this.natoX = natoX; this.natoY = natoY; this.natoZ = natoZ;
        this.russiaX = russiaX; this.russiaY = russiaY; this.russiaZ = russiaZ;
        this.baseRadius = baseRadius;
    }

    public TeamBaseSyncPacket(FriendlyByteBuf buf) {
        this.natoX = buf.readInt(); this.natoY = buf.readInt(); this.natoZ = buf.readInt();
        this.russiaX = buf.readInt(); this.russiaY = buf.readInt(); this.russiaZ = buf.readInt();
        this.baseRadius = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(natoX); buf.writeInt(natoY); buf.writeInt(natoZ);
        buf.writeInt(russiaX); buf.writeInt(russiaY); buf.writeInt(russiaZ);
        buf.writeInt(baseRadius);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient());
        });
        return true;
    }

    private void handleClient() {
        ClientTeamData.setNatoBase(natoX, natoY, natoZ);
        ClientTeamData.setRussiaBase(russiaX, russiaY, russiaZ);
        ClientTeamData.setBaseRadius(baseRadius);
        com.yourmod.teamsystem.client.journeymap.JourneyMapIntegration.updateBaseWaypoints();
    }
}
