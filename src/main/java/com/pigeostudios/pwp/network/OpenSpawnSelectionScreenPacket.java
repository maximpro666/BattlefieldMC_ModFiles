package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.FOBData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class OpenSpawnSelectionScreenPacket {

    public record SquadmateInfo(UUID uuid, String callsign, int teamOrdinal, int cooldownTicks) {}

    private final List<SquadmateInfo> squadmates;
    private final List<FOBData>       fobs;
    private final int                 teamOrdinal;
    private final String              selectedKit;

    public OpenSpawnSelectionScreenPacket(List<SquadmateInfo> squadmates,
                                          List<FOBData> fobs,
                                          int teamOrdinal,
                                          String selectedKit) {
        this.squadmates  = squadmates  != null ? squadmates  : new ArrayList<>();
        this.fobs        = fobs        != null ? fobs        : new ArrayList<>();
        this.teamOrdinal = teamOrdinal;
        this.selectedKit = selectedKit != null ? selectedKit : "";
    }

    public OpenSpawnSelectionScreenPacket(FriendlyByteBuf buf) {
        int smSize = buf.readInt();
        this.squadmates = new ArrayList<>(smSize);
        for (int i = 0; i < smSize; i++) {
            UUID   uuid          = buf.readUUID();
            String callsign      = buf.readUtf(64);
            int    teamOrd       = buf.readInt();
            int    cooldownTicks = buf.readInt();
            squadmates.add(new SquadmateInfo(uuid, callsign, teamOrd, cooldownTicks));
        }
        int fobSize = buf.readInt();
        this.fobs = new ArrayList<>(fobSize);
        for (int i = 0; i < fobSize; i++) {
            int    fobId      = buf.readInt();
            String name       = buf.readUtf(128);
            double x          = buf.readDouble();
            double y          = buf.readDouble();
            double z          = buf.readDouble();
            String worldKey   = buf.readUtf(64);
            int    teamOrd    = buf.readInt();
            float  health     = buf.readFloat();
            fobs.add(new FOBData(fobId, name, x, y, z, worldKey, teamOrd, health));
        }
        this.teamOrdinal = buf.readInt();
        this.selectedKit = buf.readUtf(64);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(squadmates.size());
        for (SquadmateInfo sm : squadmates) {
            buf.writeUUID(sm.uuid());
            buf.writeUtf(sm.callsign(), 64);
            buf.writeInt(sm.teamOrdinal());
            buf.writeInt(sm.cooldownTicks());
        }
        buf.writeInt(fobs.size());
        for (FOBData f : fobs) {
            buf.writeInt(f.fobId());
            buf.writeUtf(f.name(), 128);
            buf.writeDouble(f.x());
            buf.writeDouble(f.y());
            buf.writeDouble(f.z());
            buf.writeUtf(f.worldKey(), 64);
            buf.writeInt(f.teamOrdinal());
            buf.writeFloat(f.health());
        }
        buf.writeInt(teamOrdinal);
        buf.writeUtf(selectedKit, 64);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    Class<?> helper = Class.forName("com.pigeostudios.pwp.client.gui.screen.SpawnScreenHelper");
                    helper.getMethod("openSpawnSelectionScreen",
                            List.class, List.class, int.class, String.class)
                         .invoke(null, squadmates, fobs, teamOrdinal, selectedKit);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            })
        );
        context.setPacketHandled(true);
        return true;
    }

    public List<SquadmateInfo> getSquadmates() { return squadmates; }
    public List<FOBData>       getFobs()        { return fobs; }
    public int                 getTeamOrdinal() { return teamOrdinal; }
    public String              getSelectedKit() { return selectedKit; }
}
