package com.arcaneorigins.network;

import com.arcaneorigins.classes.ClassManager;
import com.arcaneorigins.data.ModAttachments;
import com.arcaneorigins.data.PlayerClassData;
import com.arcaneorigins.spell.ArcaneSpell;
import com.arcaneorigins.spell.SpellExecutor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("4");

        registrar.playToClient(
                SyncClassDataPayload.TYPE,
                SyncClassDataPayload.STREAM_CODEC,
                ModNetworking::handleClientSync
        );
        registrar.playToServer(
                SelectClassPayload.TYPE,
                SelectClassPayload.STREAM_CODEC,
                ModNetworking::handleSelectClass
        );
        registrar.playToServer(
                CastSpellPayload.TYPE,
                CastSpellPayload.STREAM_CODEC,
                ModNetworking::handleCastSpell
        );
        registrar.playToServer(
                CycleSpellPayload.TYPE,
                CycleSpellPayload.STREAM_CODEC,
                ModNetworking::handleCycleSpell
        );
    }

    public static void syncToPlayer(ServerPlayer player, boolean openMenu) {
        PlayerClassData data = player.getData(ModAttachments.CLASS_DATA);
        PacketDistributor.sendToPlayer(player, SyncClassDataPayload.from(data, openMenu));
    }

    private static void handleClientSync(SyncClassDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientClassCache.update(payload));
    }

    private static void handleSelectClass(SelectClassPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                ClassManager.chooseClass(sp, payload.classId(), payload.subclassId());
            }
        });
    }

    private static void handleCastSpell(CastSpellPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                SpellExecutor.tryCastSelectedSpell(sp);
            }
        });
    }

    private static void handleCycleSpell(CycleSpellPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                PlayerClassData data = sp.getData(ModAttachments.CLASS_DATA);
                if (data.getUnlockedSpells().isEmpty()) {
                    sp.displayClientMessage(Component.literal("Нет изученных заклинаний!")
                            .withStyle(ChatFormatting.YELLOW), true);
                    return;
                }
                data.cycleSelectedSpell(payload.delta());
                ArcaneSpell spell = data.getSelectedSpell();
                if (spell != null) {
                    sp.displayClientMessage(Component.literal("Заклинание: " + spell.getDisplayName()
                            + " [" + spell.getSchool().getDisplayName() + "] (" + (int) spell.getManaCost() + " маны)")
                            .withStyle(ChatFormatting.AQUA), true);
                    sp.serverLevel().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4f, 1.5f);
                }
                syncToPlayer(sp, false);
            }
        });
    }
}
