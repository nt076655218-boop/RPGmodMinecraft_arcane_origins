package com.arcaneorigins.classes;

import com.arcaneorigins.ArcaneOriginsMod;
import com.arcaneorigins.data.ModAttachments;
import com.arcaneorigins.data.PlayerClassData;
import com.arcaneorigins.item.ModItems;
import com.arcaneorigins.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class ClassManager {
    private static final ResourceLocation MOD_HP = ResourceLocation.fromNamespaceAndPath(ArcaneOriginsMod.MOD_ID, "class_max_hp");
    private static final ResourceLocation MOD_ARMOR = ResourceLocation.fromNamespaceAndPath(ArcaneOriginsMod.MOD_ID, "class_armor");
    private static final ResourceLocation MOD_ATK = ResourceLocation.fromNamespaceAndPath(ArcaneOriginsMod.MOD_ID, "class_attack_damage");
    private static final ResourceLocation MOD_ATK_SPD = ResourceLocation.fromNamespaceAndPath(ArcaneOriginsMod.MOD_ID, "class_attack_speed");
    private static final ResourceLocation MOD_SPEED = ResourceLocation.fromNamespaceAndPath(ArcaneOriginsMod.MOD_ID, "class_move_speed");
    private static final ResourceLocation MOD_KB = ResourceLocation.fromNamespaceAndPath(ArcaneOriginsMod.MOD_ID, "class_kb_resist");

    public static void chooseClass(ServerPlayer player, String classId, String subclassId) {
        RpgClass rpgClass = RpgClass.byId(classId);
        RpgSubclass subclass = RpgSubclass.byId(subclassId);
        if (rpgClass == null || subclass == null || subclass.getParent() != rpgClass) return;

        PlayerClassData data = player.getData(ModAttachments.CLASS_DATA);
        if (data.hasChosenClass()) return; // Prevent switching without Scroll of Rebirth / command
        if (rpgClass == RpgClass.SORCERER && !data.isSorcererUnlocked()) return;

        data.setClassId(rpgClass.getId());
        data.setSubclassId(subclass.getId());
        data.setLevel(1);
        data.setClassXp(0);

        applyPassives(player);
        data.setMana(data.getMaxMana());

        // Give class-appropriate starter gear
        if (rpgClass == RpgClass.WIZARD) {
            giveIfMissing(player, new ItemStack(ModItems.ARCANE_SPELLBOOK.get()));
        } else if (rpgClass == RpgClass.WARLOCK) {
            giveIfMissing(player, new ItemStack(ModItems.PATRON_SIGIL.get()));
            data.setPatronFavor(25);
        }

        player.setHealth(player.getMaxHealth());
        player.sendSystemMessage(Component.literal("Вы выбрали путь: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(rpgClass.getDisplayName() + " — " + subclass.getDisplayName()).withStyle(ChatFormatting.GOLD)));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.1f);

        ModNetworking.syncToPlayer(player, false);
    }

    public static void applyPassives(ServerPlayer player) {
        clearModifiers(player);
        PlayerClassData data = player.getData(ModAttachments.CLASS_DATA);
        RpgClass rpgClass = data.getRpgClass();
        RpgSubclass sub = data.getRpgSubclass();
        if (rpgClass == null || sub == null) return;

        int lvlBonus = data.getLevel() - 1;
        float maxMana = rpgClass.getBaseMaxMana() + lvlBonus * 12.0f;

        switch (sub) {
            case KNIGHT -> {
                setModifier(player, Attributes.ARMOR, MOD_ARMOR, 4.0 + lvlBonus * 0.4, AttributeModifier.Operation.ADD_VALUE);
                setModifier(player, Attributes.MAX_HEALTH, MOD_HP, 4.0 + lvlBonus * 0.5, AttributeModifier.Operation.ADD_VALUE);
            }
            case BLADEMASTER -> {
                setModifier(player, Attributes.ATTACK_SPEED, MOD_ATK_SPD, 0.18 + lvlBonus * 0.015, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                setModifier(player, Attributes.ATTACK_DAMAGE, MOD_ATK, 2.0 + lvlBonus * 0.3, AttributeModifier.Operation.ADD_VALUE);
            }
            case GUARDIAN -> {
                setModifier(player, Attributes.ARMOR, MOD_ARMOR, 2.0 + lvlBonus * 0.3, AttributeModifier.Operation.ADD_VALUE);
                setModifier(player, Attributes.MAX_HEALTH, MOD_HP, 6.0 + lvlBonus * 0.6, AttributeModifier.Operation.ADD_VALUE);
                setModifier(player, Attributes.KNOCKBACK_RESISTANCE, MOD_KB, 0.40, AttributeModifier.Operation.ADD_VALUE);
            }
            case BERSERKER -> {
                setModifier(player, Attributes.ATTACK_DAMAGE, MOD_ATK, 3.0 + lvlBonus * 0.35, AttributeModifier.Operation.ADD_VALUE);
                setModifier(player, Attributes.MAX_HEALTH, MOD_HP, 2.0, AttributeModifier.Operation.ADD_VALUE);
            }
            case TOTEMIC -> {
                setModifier(player, Attributes.MAX_HEALTH, MOD_HP, 6.0 + lvlBonus * 0.6, AttributeModifier.Operation.ADD_VALUE);
            }
            case THUNDERBREAKER -> {
                setModifier(player, Attributes.MOVEMENT_SPEED, MOD_SPEED, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                setModifier(player, Attributes.ATTACK_DAMAGE, MOD_ATK, 1.5 + lvlBonus * 0.25, AttributeModifier.Operation.ADD_VALUE);
            }
            case SCHOOL_ICE -> {
                maxMana *= 1.15f;
            }
            case SCHOOL_LIGHTNING -> {
                maxMana *= 1.15f;
                setModifier(player, Attributes.MOVEMENT_SPEED, MOD_SPEED, 0.12, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            }
            case PATRON_ABYSS -> {
                maxMana *= 1.20f;
            }
            case PATRON_ELDER -> {
                maxMana *= 1.10f;
            }
            case PATRON_INFERNO -> {
                setModifier(player, Attributes.ATTACK_DAMAGE, MOD_ATK, 2.0, AttributeModifier.Operation.ADD_VALUE);
            }
            case DRACONIC -> {
                setModifier(player, Attributes.ARMOR, MOD_ARMOR, 4.0, AttributeModifier.Operation.ADD_VALUE);
                setModifier(player, Attributes.MAX_HEALTH, MOD_HP, 4.0, AttributeModifier.Operation.ADD_VALUE);
                maxMana *= 1.20f;
            }
            case WILD_MAGIC -> {
                maxMana *= 1.10f;
            }
            case ASTRAL -> {
                setModifier(player, Attributes.MOVEMENT_SPEED, MOD_SPEED, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                maxMana *= 1.15f;
            }
        }

        data.setMaxMana(maxMana);
    }

    public static void clearModifiers(ServerPlayer player) {
        removeModifier(player, Attributes.MAX_HEALTH, MOD_HP);
        removeModifier(player, Attributes.ARMOR, MOD_ARMOR);
        removeModifier(player, Attributes.ATTACK_DAMAGE, MOD_ATK);
        removeModifier(player, Attributes.ATTACK_SPEED, MOD_ATK_SPD);
        removeModifier(player, Attributes.MOVEMENT_SPEED, MOD_SPEED);
        removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, MOD_KB);
    }

    private static void setModifier(ServerPlayer player, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        if (inst.hasModifier(id)) inst.removeModifier(id);
        inst.addPermanentModifier(new AttributeModifier(id, amount, op));
    }

    private static void removeModifier(ServerPlayer player, Holder<Attribute> attr, ResourceLocation id) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null && inst.hasModifier(id)) {
            inst.removeModifier(id);
        }
    }

    private static void giveIfMissing(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().contains(stack)) {
            player.getInventory().add(stack);
        }
    }
}
