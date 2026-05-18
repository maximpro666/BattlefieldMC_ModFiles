package com.yourmod.teamsystem.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SoundPacket {
    private final String soundId;
    private final boolean positioned;
    private final double x, y, z;
    private final float volume;

    public SoundPacket(String soundId) {
        this(soundId, false, 0, 0, 0, 1.0f);
    }

    public SoundPacket(String soundId, double x, double y, double z, float volume) {
        this(soundId, true, x, y, z, volume);
    }

    private SoundPacket(String soundId, boolean positioned, double x, double y, double z, float volume) {
        this.soundId = soundId;
        this.positioned = positioned;
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
    }

    public SoundPacket(FriendlyByteBuf buf) {
        this.soundId = buf.readUtf(256);
        this.positioned = buf.readBoolean();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.volume = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(soundId);
        buf.writeBoolean(positioned);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(volume);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.yourmod.teamsystem.client.ClientSoundHandler.handleSoundPacket(soundId, positioned, x, y, z, volume)
            );
        });
        return true;
    }
}
