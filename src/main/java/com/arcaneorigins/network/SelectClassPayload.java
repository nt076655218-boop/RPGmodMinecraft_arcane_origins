package com.arcaneorigins.network;

import com.arcaneorigins.ArcaneOriginsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectClassPayload(String classId, String subclassId) implements CustomPacketPayload {
    public static final Type<SelectClassPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcaneOriginsMod.MOD_ID, "select_class"));

    public static final StreamCodec<FriendlyByteBuf, SelectClassPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> { buf.writeUtf(p.classId); buf.writeUtf(p.subclassId); },
                    buf -> new SelectClassPayload(buf.readUtf(), buf.readUtf())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
