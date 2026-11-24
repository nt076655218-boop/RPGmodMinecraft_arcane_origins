package com.arcaneorigins.network;

import com.arcaneorigins.ArcaneOriginsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CastSpellPayload() implements CustomPacketPayload {
    public static final Type<CastSpellPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcaneOriginsMod.MOD_ID, "cast_spell"));

    public static final StreamCodec<FriendlyByteBuf, CastSpellPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> {}, buf -> new CastSpellPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
