package com.arcaneorigins.network;

import com.arcaneorigins.ArcaneOriginsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CycleSpellPayload(int delta) implements CustomPacketPayload {
    public static final Type<CycleSpellPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcaneOriginsMod.MOD_ID, "cycle_spell"));

    public static final StreamCodec<FriendlyByteBuf, CycleSpellPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> buf.writeInt(p.delta), buf -> new CycleSpellPayload(buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
