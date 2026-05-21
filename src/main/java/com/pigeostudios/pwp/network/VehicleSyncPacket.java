package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.VehicleData;
import com.pigeostudios.pwp.client.VehicleEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class VehicleSyncPacket {
    private List<String> vehicleIds;
    private List<String> vehicleDisplayNames;
    private List<Integer> vehicleTicketCosts;
    private List<Integer> vehicleMinRanks;

    public VehicleSyncPacket(List<String> vehicleIds, List<String> vehicleDisplayNames,
                             List<Integer> vehicleTicketCosts, List<Integer> vehicleMinRanks) {
        this.vehicleIds = vehicleIds;
        this.vehicleDisplayNames = vehicleDisplayNames;
        this.vehicleTicketCosts = vehicleTicketCosts;
        this.vehicleMinRanks = vehicleMinRanks;
    }

    public VehicleSyncPacket(FriendlyByteBuf buf) {
        int vehicleCount = buf.readInt();
        this.vehicleIds = new ArrayList<>();
        this.vehicleDisplayNames = new ArrayList<>();
        this.vehicleTicketCosts = new ArrayList<>();
        this.vehicleMinRanks = new ArrayList<>();

        for (int i = 0; i < vehicleCount; i++) {
            vehicleIds.add(buf.readUtf(256));
            vehicleDisplayNames.add(buf.readUtf(256));
            vehicleTicketCosts.add(buf.readInt());
            vehicleMinRanks.add(buf.readInt());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(vehicleIds.size());
        for (int i = 0; i < vehicleIds.size(); i++) {
            buf.writeUtf(vehicleIds.get(i));
            buf.writeUtf(vehicleDisplayNames.get(i));
            buf.writeInt(vehicleTicketCosts.get(i));
            buf.writeInt(vehicleMinRanks.get(i));
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                handleClientSide(vehicleIds, vehicleDisplayNames, vehicleTicketCosts, vehicleMinRanks));
        });
        return true;
    }

    private static void handleClientSide(List<String> vehicleIds, List<String> vehicleDisplayNames,
                                         List<Integer> vehicleTicketCosts, List<Integer> vehicleMinRanks) {
        List<VehicleData> vehicleList = new ArrayList<>();
        List<VehicleEntry> entryList = new ArrayList<>();
        for (int i = 0; i < vehicleIds.size(); i++) {
            String id = vehicleIds.get(i);
            String displayName = vehicleDisplayNames.get(i);
            int ticketCost = vehicleTicketCosts.get(i);
            int minRank = vehicleMinRanks.get(i);
            boolean available = com.pigeostudios.pwp.core.Rank.fromOrdinal(ClientTeamData.localPlayerRank).ordinal() >= minRank;
            vehicleList.add(new VehicleData(id, displayName, "", "", ticketCost, minRank, 0, available));
            entryList.add(new VehicleEntry(id, displayName, "", "", ticketCost, ticketCost, minRank, 0));
        }
        ClientTeamData.vehicles = vehicleList;
        ClientTeamData.availableVehicles.clear();
        ClientTeamData.availableVehicles.addAll(entryList);
    }

    public List<String> getVehicleIds() { return vehicleIds; }
    public List<String> getVehicleDisplayNames() { return vehicleDisplayNames; }
    public List<Integer> getVehicleTicketCosts() { return vehicleTicketCosts; }
    public List<Integer> getVehicleMinRanks() { return vehicleMinRanks; }
}
