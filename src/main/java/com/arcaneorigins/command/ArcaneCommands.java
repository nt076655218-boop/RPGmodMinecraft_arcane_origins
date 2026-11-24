package com.arcaneorigins.command;

import com.arcaneorigins.classes.ClassManager;
import com.arcaneorigins.data.ModAttachments;
import com.arcaneorigins.data.PlayerClassData;
import com.arcaneorigins.network.ModNetworking;
import com.arcaneorigins.spell.ArcaneSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class ArcaneCommands {
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("arcane")
                        .then(Commands.literal("reset")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    PlayerClassData data = sp.getData(ModAttachments.CLASS_DATA);
                                    data.setClassId("");
                                    data.setSubclassId("");
                                    data.clearSpells();
                                    ClassManager.clearModifiers(sp);
                                    ModNetworking.syncToPlayer(sp, true);
                                    sp.sendSystemMessage(Component.literal("Класс сброшен! Выбери новый путь.").withStyle(ChatFormatting.GOLD));
                                    return 1;
                                }))
                        .then(Commands.literal("levelup")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    PlayerClassData data = sp.getData(ModAttachments.CLASS_DATA);
                                    if (data.getLevel() < PlayerClassData.MAX_LEVEL) {
                                        data.setLevel(data.getLevel() + 1);
                                        ClassManager.onClassLevelUp(sp, data);
                                        ModNetworking.syncToPlayer(sp, false);
                                    }
                                    return 1;
                                }))
                        .then(Commands.literal("unlock_sorcerer")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    PlayerClassData data = sp.getData(ModAttachments.CLASS_DATA);
                                    data.setSorcererRolled(true);
                                    data.setSorcererUnlocked(true);
                                    ModNetworking.syncToPlayer(sp, !data.hasChosenClass());
                                    sp.sendSystemMessage(Component.literal("★ Редкий класс ЧАРОДЕЙ разблокирован!").withStyle(ChatFormatting.GOLD));
                                    return 1;
                                }))
                        .then(Commands.literal("learn_all")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    PlayerClassData data = sp.getData(ModAttachments.CLASS_DATA);
                                    for (ArcaneSpell s : ArcaneSpell.values()) {
                                        data.unlockSpell(s);
                                    }
                                    ModNetworking.syncToPlayer(sp, false);
                                    sp.sendSystemMessage(Component.literal("Все 16 заклинаний изучены!").withStyle(ChatFormatting.AQUA));
                                    return 1;
                                }))
                        .then(Commands.literal("mana")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    PlayerClassData data = sp.getData(ModAttachments.CLASS_DATA);
                                    data.setMana(data.getMaxMana());
                                    ModNetworking.syncToPlayer(sp, false);
                                    sp.sendSystemMessage(Component.literal("Мана полностью восстановлена!").withStyle(ChatFormatting.AQUA));
                                    return 1;
                                }))
        );
    }
}
