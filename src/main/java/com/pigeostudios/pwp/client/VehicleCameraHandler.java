package com.pigeostudios.pwp.client;

import com.pigeostudios.pwp.vehicle.adapter.VehicleAdapterRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "pwp", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class VehicleCameraHandler {

    public static void handlePerspectiveKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (isInBattleVehicle(mc)) {
            if (mc.options.keyTogglePerspective.consumeClick()) {
                CameraType next = switch (mc.options.getCameraType()) {
                    case FIRST_PERSON -> CameraType.THIRD_PERSON_BACK;
                    case THIRD_PERSON_BACK -> CameraType.THIRD_PERSON_FRONT;
                    case THIRD_PERSON_FRONT -> CameraType.FIRST_PERSON;
                };
                mc.options.setCameraType(next);
            }
        } else if (mc.options.keyTogglePerspective.consumeClick()) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    private static boolean isInBattleVehicle(Minecraft mc) {
        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) return false;
        return VehicleAdapterRegistry.getInstance().hasAdapter(vehicle);
    }

    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent event) {
        if (event.isMounting()) return;
        if (!(event.getEntityMounting() instanceof net.minecraft.client.player.LocalPlayer)) return;
        Minecraft mc = Minecraft.getInstance();
        Entity vehicle = event.getEntityBeingMounted();
        if (vehicle != null && VehicleAdapterRegistry.getInstance().hasAdapter(vehicle)) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }
}
