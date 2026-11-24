package com.arcaneorigins.network;

import com.arcaneorigins.ArcaneOriginsMod;
import com.arcaneorigins.data.PlayerClassData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record SyncClassDataPayload(
        String classId,
        String subclassId,
        boolean sorcererUnlocked,
        float mana,
        float maxMana,
        int level,
        int classXp,
        String selectedSpellId,
        int patronFavor,
        List<String> unlockedSpells,
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
                data.getSelectedSpellId(),
                data.getPatronFavor(),
                data.getUnlockedSpellIds(),
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
        buf.writeUtf(p.selectedSpellId);
        buf.writeInt(p.patronFavor);
        buf.writeInt(p.unlockedSpells.size());
        for (String id : p.unlockedSpells) {
            buf.writeUtf(id);
        }
        buf.writeBoolean(p.openMenu);
    }

    private static SyncClassDataPayload decode(FriendlyByteBuf buf) {
        String classId = buf.readUtf();
        String subclassId = buf.readUtf();
        boolean sorcererUnlocked = buf.readBoolean();
        float mana = buf.readFloat();
        float maxMana = buf.readFloat();
        int level = buf.readInt();
        int classXp = buf.readInt();
        String selectedSpellId = buf.readUtf();
        int patronFavor = buf.readInt();
        int count = buf.readInt();
        List<String> spells = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            spells.add(buf.readUtf());
        }
        boolean openMenu = buf.readBoolean();
        return new SyncClassDataPayload(classId, subclassId, sorcererUnlocked, mana, maxMana,
                level, classXp, selectedSpellId, patronFavor, spells, openMenu);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
