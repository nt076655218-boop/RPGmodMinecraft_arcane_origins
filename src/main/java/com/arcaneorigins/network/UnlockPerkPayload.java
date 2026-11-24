package com.arcaneorigins.network;

import com.arcaneorigins.ArcaneOriginsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UnlockPerkPayload(String perkId) implements CustomPacketPayload {
    public static final Type<UnlockPerkPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcaneOriginsMod.MOD_ID, "unlock_perk"));

    public static final StreamCodec<FriendlyByteBuf, UnlockPerkPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> buf.writeUtf(p.perkId), buf -> new UnlockPerkPayload(buf.readUtf()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
