package com.arcaneorigins.network;

import com.arcaneorigins.ArcaneOriginsMod;
import com.arcaneorigins.data.PlayerClassData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncClassDataPayload(
        String classId,
        String subclassId,
        boolean sorcererUnlocked,
        float mana,
        float maxMana,
        int level,
        int classXp,
        int selectedSpellIndex,
        int patronFavor,
        boolean openMenu
) implements CustomPacketPayload {

    public static final Type<SyncClassDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcaneOriginsMod.MOD_ID, "sync_class_data"));

    public static final StreamCodec<FriendlyByteBuf, SyncClassDataPayload> STREAM_CODEC =
            StreamCodec.of(SyncClassDataPayload::encode, SyncClassDataPayload::decode);

    public static SyncClassDataPayload from(PlayerClassData data, boolean openMenu) {
        return new SyncClassDataPayload(
                data.getClassId(),
                data.getSubclassId(),
                data.isSorcererUnlocked(),
                data.getMana(),
                data.getMaxMana(),
                data.getLevel(),
                data.getClassXp(),
                data.getSelectedSpellIndex(),
                data.getPatronFavor(),
                openMenu
        );
    }

    private static void encode(FriendlyByteBuf buf, SyncClassDataPayload p) {
        buf.writeUtf(p.classId);
        buf.writeUtf(p.subclassId);
        buf.writeBoolean(p.sorcererUnlocked);
        buf.writeFloat(p.mana);
        buf.writeFloat(p.maxMana);
        buf.writeInt(p.level);
        buf.writeInt(p.classXp);
        buf.writeInt(p.selectedSpellIndex);
        buf.writeInt(p.patronFavor);
        buf.writeBoolean(p.openMenu);
    }

    private static SyncClassDataPayload decode(FriendlyByteBuf buf) {
        return new SyncClassDataPayload(
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
