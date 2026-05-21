package com.pigeostudios.pwp.events;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.BattlefieldRuntime;
import com.pigeostudios.pwp.core.Team;
import com.pigeostudios.pwp.core.TeamManager;
import com.pigeostudios.pwp.core.PlayerCombatData;
import com.pigeostudios.pwp.vehicle.VehicleDefinition;
import com.pigeostudios.pwp.vehicle.adapter.IVehicleAdapter;
import com.pigeostudios.pwp.vehicle.adapter.VehicleAdapterRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

public class VehicleAccessControl {

    @SubscribeEvent
    public void onEntityMount(EntityMountEvent event) {
        if (!(event.getEntityMounting() instanceof ServerPlayer player)) return;

        Entity vehicle = event.getEntityBeingMounted();
        VehicleAdapterRegistry registry = BattlefieldRuntime.getInstance().getVehicleAdapterRegistry();
        IVehicleAdapter adapter = registry.findAdapter(vehicle);

        if (adapter == null) return;

        TeamManager tm = PWP.getTeamManager();
        if (tm == null) return;

        PlayerCombatData playerData = tm.getOrCreatePlayerData(player.getUUID());
        Team playerTeam = playerData.getTeam();

        if (!playerTeam.isPlayable()) {
            event.setCanceled(true);
            return;
        }

        UUID ownerUUID = adapter.getOwnerUUID(vehicle);
        if (ownerUUID != null) {
            Team ownerTeam = tm.getOrCreatePlayerData(ownerUUID).getTeam();
            if (playerTeam != ownerTeam) {
                player.sendSystemMessage(Component.translatable("pwp.chat.vehicle.wrong_team"));
                event.setCanceled(true);
                return;
            }
        }

        boolean isDriver = adapter.getOccupiedSeatIndex(vehicle, player) < 0
            ? vehicle.getPassengers().isEmpty()
            : adapter.isDriver(vehicle, player);

        if (isDriver) {
            String vehicleId = adapter.getVehicleId(vehicle);
            VehicleDefinition def = BattlefieldRuntime.getInstance().getVehicleDefRegistry().get(vehicleId);

            if (def != null && def.getCertification() != null && !def.getCertification().isEmpty()) {
                String requiredCert = def.getCertification();
                if (!playerData.hasCertification(requiredCert)) {
                    player.sendSystemMessage(Component.translatable("pwp.chat.vehicle.requires_cert", requiredCert));
                    event.setCanceled(true);
                }
            }
        }
    }
}
