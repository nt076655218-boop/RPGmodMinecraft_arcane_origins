package com.arcaneorigins.classes;

import com.arcaneorigins.ArcaneOriginsMod;
import com.arcaneorigins.data.ModAttachments;
import com.arcaneorigins.data.PlayerClassData;
import com.arcaneorigins.item.ModItems;
import com.arcaneorigins.network.ModNetworking;
import com.arcaneorigins.spell.ArcaneSpell;
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
        if (data.hasChosenClass()) return;
        if (rpgClass == RpgClass.SORCERER && !data.isSorcererUnlocked()) return;

        data.setClassId(rpgClass.getId());
        data.setSubclassId(subclass.getId());
        data.setLevel(1);
        data.setClassXp(0);
        data.clearProgression(); // Starts with 1 Skill Point!

        applyPassives(player);
        data.setMana(data.getMaxMana());

        if (rpgClass == RpgClass.WIZARD) {
            giveIfMissing(player, new ItemStack(ModItems.ARCANE_SPELLBOOK.get()));
            switch (subclass) {
                case SCHOOL_ICE -> {
                    data.unlockSpell(ArcaneSpell.GLACIAL_LANCE);
                    giveIfMissing(player, new ItemStack(ModItems.SCROLL_FROST_NOVA.get()));
                }
                case SCHOOL_LIGHTNING -> {
                    data.unlockSpell(ArcaneSpell.ARC_LIGHTNING);
                    giveIfMissing(player, new ItemStack(ModItems.SCROLL_THUNDERSTEP.get()));
                }
                case SCHOOL_GRAVITY -> {
                    data.unlockSpell(ArcaneSpell.VOID_RIFT);
                    giveIfMissing(player, new ItemStack(ModItems.SCROLL_GRAVITY_WELL.get()));
                }
                case SCHOOL_FIRE -> {
                    data.unlockSpell(ArcaneSpell.FIREBALL);
                    giveIfMissing(player, new ItemStack(ModItems.SCROLL_FIRE_TORNADO.get()));
                }
                default -> data.unlockSpell(ArcaneSpell.GLACIAL_LANCE);
            }
            player.sendSystemMessage(Component.literal("Как Волшебник, ты изучаешь любые Свитки Заклинаний (ПКМ) и тратишь очки навыков в [G]!")
                    .withStyle(ChatFormatting.AQUA));

        } else if (rpgClass == RpgClass.WARLOCK) {
            giveIfMissing(player, new ItemStack(ModItems.PATRON_SIGIL.get()));
            data.setPatronFavor(25);
            if (subclass == RpgSubclass.PATRON_ABYSS) {
                data.unlockSpell(ArcaneSpell.VOID_RIFT);
            } else if (subclass == RpgSubclass.PATRON_INFERNO) {
                data.unlockSpell(ArcaneSpell.FIREBALL);
            } else {
                data.unlockSpell(ArcaneSpell.ARC_LIGHTNING);
            }
            player.sendSystemMessage(Component.literal("Пакт заключён! Открой меню прокачки [G], чтобы потратить 1-е очко развития на дары Покровителя.")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));

        } else if (rpgClass == RpgClass.SORCERER) {
            data.unlockSpell(ArcaneSpell.GLACIAL_LANCE);
            data.unlockSpell(ArcaneSpell.FIREBALL);
            player.sendSystemMessage(Component.literal("Магия крови пробудилась! Открой меню прокачки [G], чтобы развивать врождённые чары.")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            player.sendSystemMessage(Component.literal("Доступно 1 очко развития! Нажми [G], чтобы открыть дерево прокачки класса.")
                    .withStyle(ChatFormatting.GOLD));
        }

        player.setHealth(player.getMaxHealth());
        player.sendSystemMessage(Component.literal("Вы выбрали путь: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(rpgClass.getDisplayName() + " — " + subclass.getDisplayName()).withStyle(ChatFormatting.GOLD)));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.1f);

        ModNetworking.syncToPlayer(player, false);
    }

    public static void unlockPerkNode(ServerPlayer player, String perkId) {
        PlayerClassData data = player.getData(ModAttachments.CLASS_DATA);
        ClassPerk perk = ClassPerk.byId(perkId);
        if (!data.hasChosenClass() || perk == null) return;
        if (perk.getRpgClass() != data.getRpgClass()) return;
        if (data.hasPerk(perk)) return;
        if (data.getSkillPoints() <= 0 || data.getLevel() < perk.getRequiredLevel()) return;

        data.addSkillPoints(-1);
        data.unlockPerk(perk);

        // Grant spells tied to Sorcerer / Warlock perk nodes
        switch (perk) {
            case SORC_ARCANE_PULSE -> {
                data.unlockSpell(ArcaneSpell.ARC_LIGHTNING);
                data.unlockSpell(ArcaneSpell.VOID_RIFT);
            }
            case SORC_TWIN_FLOW -> {
                data.unlockSpell(ArcaneSpell.FIRE_TORNADO);
                data.unlockSpell(ArcaneSpell.FROST_NOVA);
            }
            case SORC_OVERCHARGE -> {
                data.unlockSpell(ArcaneSpell.STORM_ORB);
                data.unlockSpell(ArcaneSpell.GRAVITY_WELL);
            }
            case SORC_METAMAGIC -> {
                data.unlockSpell(ArcaneSpell.ZERO_G_SLAM);
                data.unlockSpell(ArcaneSpell.PLASMA_BLAST);
            }
            case SORC_APOTHEOSIS -> {
                data.unlockSpell(ArcaneSpell.METEOR_SHOWER);
                data.unlockSpell(ArcaneSpell.ABYSSAL_METEOR);
            }
            case LOCK_PACT_BOON -> {
                data.unlockSpell(ArcaneSpell.GRAVITY_WELL);
                data.addPatronFavor(15);
            }
            case LOCK_SOUL_SIPHON -> data.unlockSpell(ArcaneSpell.FIRE_TORNADO);
            case LOCK_ELDRITCH_SIGHT -> {
                data.unlockSpell(ArcaneSpell.VOID_PULL);
                data.unlockSpell(ArcaneSpell.SHOCKWAVE_NOVA);
            }
            case LOCK_ABYSSAL_ARMOR -> data.unlockSpell(ArcaneSpell.ZERO_G_SLAM);
            case LOCK_HERALD_DOOM -> {
                data.unlockSpell(ArcaneSpell.ABYSSAL_METEOR);
                data.unlockSpell(ArcaneSpell.TEMPEST_JUDGMENT);
                data.setPatronFavor(100);
            }
            default -> {
                if (perk.getPrimarySpell() != null) {
                    data.unlockSpell(perk.getPrimarySpell());
                }
            }
        }

        applyPassives(player);
        player.sendSystemMessage(Component.literal("✦ Изучен талант: ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(perk.getTitle()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.25f);
        ModNetworking.syncToPlayer(player, false);
    }

    public static void onClassLevelUp(ServerPlayer player, PlayerClassData data) {
        applyPassives(player);
        int lvl = data.getLevel();
        player.sendSystemMessage(Component.literal("★ Уровень класса повышен до " + lvl + "! (+1 Очко развития — нажми [G])")
                .withStyle(ChatFormatting.GOLD));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.2f);
    }

    public static void applyPassives(ServerPlayer player) {
        clearModifiers(player);
        PlayerClassData data = player.getData(ModAttachments.CLASS_DATA);
        RpgClass rpgClass = data.getRpgClass();
        RpgSubclass sub = data.getRpgSubclass();
        if (rpgClass == null || sub == null) return;

        int lvlBonus = data.getLevel() - 1;
        float maxMana = rpgClass.getBaseMaxMana() + lvlBonus * 12.0f;

        double bonusHp = 0.0;
        double bonusArmor = 0.0;
        double bonusAtk = 0.0;
        double bonusAtkSpd = 0.0;
        double bonusSpeed = 0.0;
        double bonusKb = 0.0;

        switch (sub) {
            case KNIGHT -> { bonusArmor += 4.0 + lvlBonus * 0.4; bonusHp += 4.0 + lvlBonus * 0.5; }
            case BLADEMASTER -> { bonusAtkSpd += 0.18 + lvlBonus * 0.015; bonusAtk += 2.0 + lvlBonus * 0.3; }
            case GUARDIAN -> { bonusArmor += 2.0 + lvlBonus * 0.3; bonusHp += 6.0 + lvlBonus * 0.6; bonusKb += 0.40; }
            case BERSERKER -> { bonusAtk += 3.0 + lvlBonus * 0.35; bonusHp += 2.0; }
            case TOTEMIC -> { bonusHp += 6.0 + lvlBonus * 0.6; }
            case THUNDERBREAKER -> { bonusSpeed += 0.15; bonusAtk += 1.5 + lvlBonus * 0.25; }
            case SCHOOL_ICE, SCHOOL_GRAVITY, SCHOOL_FIRE -> maxMana *= 1.15f;
            case SCHOOL_LIGHTNING -> { maxMana *= 1.15f; bonusSpeed += 0.12; }
            case PATRON_ABYSS -> maxMana *= 1.20f;
            case PATRON_ELDER -> maxMana *= 1.10f;
            case PATRON_INFERNO -> bonusAtk += 2.0;
            case DRACONIC -> { bonusArmor += 4.0; bonusHp += 4.0; maxMana *= 1.20f; }
            case WILD_MAGIC -> maxMana *= 1.10f;
            case ASTRAL -> { bonusSpeed += 0.15; maxMana *= 1.15f; }
        }

        // Perk tree attribute bonuses
        if (data.hasPerk(ClassPerk.WAR_IRON_SKIN)) { bonusArmor += 3.0; bonusHp += 4.0; }
        if (data.hasPerk(ClassPerk.WAR_WEAPON_MASTERY)) { bonusAtk += 2.5; bonusAtkSpd += 0.12; }
        if (data.hasPerk(ClassPerk.WAR_SECOND_WIND)) { bonusHp += 4.0; }
        if (data.hasPerk(ClassPerk.WAR_WARLORD)) { bonusArmor += 4.0; bonusHp += 6.0; bonusKb += 0.30; }

        if (data.hasPerk(ClassPerk.BARB_THICK_HIDE)) { bonusHp += 6.0; bonusArmor += 2.0; }
        if (data.hasPerk(ClassPerk.BARB_UNYIELDING)) { bonusSpeed += 0.18; bonusAtk += 3.0; }
        if (data.hasPerk(ClassPerk.BARB_TITAN_RAGE)) { bonusHp += 8.0; bonusAtk += 4.0; }

        if (data.hasPerk(ClassPerk.SORC_ARCANE_PULSE)) { maxMana += 25.0f; }
        if (data.hasPerk(ClassPerk.SORC_APOTHEOSIS)) { maxMana += 50.0f; }

        if (data.hasPerk(ClassPerk.LOCK_ELDRITCH_SIGHT)) { maxMana += 35.0f; }
        if (data.hasPerk(ClassPerk.LOCK_ABYSSAL_ARMOR)) { bonusArmor += 4.0; }

        if (data.hasPerk(ClassPerk.WIZ_SCHOLAR)) { maxMana += 35.0f; }
        if (data.hasPerk(ClassPerk.WIZ_ARCANE_WARD)) { bonusArmor += 4.0; bonusHp += 4.0; }
        if (data.hasPerk(ClassPerk.WIZ_ARCHMAGE)) { maxMana += 60.0f; }

        if (bonusHp > 0) setModifier(player, Attributes.MAX_HEALTH, MOD_HP, bonusHp, AttributeModifier.Operation.ADD_VALUE);
        if (bonusArmor > 0) setModifier(player, Attributes.ARMOR, MOD_ARMOR, bonusArmor, AttributeModifier.Operation.ADD_VALUE);
        if (bonusAtk > 0) setModifier(player, Attributes.ATTACK_DAMAGE, MOD_ATK, bonusAtk, AttributeModifier.Operation.ADD_VALUE);
        if (bonusAtkSpd > 0) setModifier(player, Attributes.ATTACK_SPEED, MOD_ATK_SPD, bonusAtkSpd, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (bonusSpeed > 0) setModifier(player, Attributes.MOVEMENT_SPEED, MOD_SPEED, bonusSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (bonusKb > 0) setModifier(player, Attributes.KNOCKBACK_RESISTANCE, MOD_KB, Math.min(1.0, bonusKb), AttributeModifier.Operation.ADD_VALUE);

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
