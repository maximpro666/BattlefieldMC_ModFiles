package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.ClientTeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NotificationPacket {
    private final String text;
    private final String type;
    private final int duration;
    private final String sound;

    public NotificationPacket(String text, String type, int duration, String sound) {
        this.text = text != null ? text : "";
        this.type = type != null ? type : "info";
        this.duration = Math.max(1000, duration);
        this.sound = sound != null ? sound : "";
    }

    public NotificationPacket(FriendlyByteBuf buf) {
        this.text = buf.readUtf(512);
        this.type = buf.readUtf(32);
        this.duration = buf.readInt();
        this.sound = buf.readUtf(128);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(text);
        buf.writeUtf(type);
        buf.writeInt(duration);
        buf.writeUtf(sound);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.pigeostudios.pwp.client.gui.overlay.NotificationOverlay.addNotification(text, type, duration);
            });
        });
        return true;
    }

    public String getText() { return text; }
    public String getType() { return type; }
    public int getDuration() { return duration; }
    public String getSound() { return sound; }
}
