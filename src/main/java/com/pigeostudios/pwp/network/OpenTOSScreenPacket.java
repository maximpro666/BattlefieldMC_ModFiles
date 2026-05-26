package com.pigeostudios.pwp.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenTOSScreenPacket {
    private final String tosUrl;
    private final String privacyUrl;

    public OpenTOSScreenPacket(String tosUrl, String privacyUrl) {
        this.tosUrl = tosUrl;
        this.privacyUrl = privacyUrl;
    }

    public OpenTOSScreenPacket(FriendlyByteBuf buf) {
        this.tosUrl = buf.readUtf();
        this.privacyUrl = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(tosUrl);
        buf.writeUtf(privacyUrl);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientScreenAccessor.openTOS(tosUrl, privacyUrl);
            });
        });
        return true;
    }
}
