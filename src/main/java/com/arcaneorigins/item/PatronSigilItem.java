package com.arcaneorigins.item;

import com.arcaneorigins.classes.ClassManager;
import com.arcaneorigins.classes.RpgClass;
import com.arcaneorigins.data.ModAttachments;
import com.arcaneorigins.data.PlayerClassData;
import com.arcaneorigins.network.ModNetworking;
import com.arcaneorigins.spell.ArcaneSpell;
import com.arcaneorigins.spell.SpellVfx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class PatronSigilItem extends Item {
    public PatronSigilItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack sigil = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            PlayerClassData data = sp.getData(ModAttachments.CLASS_DATA);
            if (data.getRpgClass() != RpgClass.WARLOCK) {
                sp.displayClientMessage(Component.literal("Лишь Колдун слышит зов Покровителя...").withStyle(ChatFormatting.DARK_PURPLE), true);
                return InteractionResultHolder.fail(sigil);
            }

            InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack offering = sp.getItemInHand(otherHand);
            int favorGain = getOfferingValue(offering);

            if (favorGain <= 0) {
                sp.displayClientMessage(Component.literal("Возьми во вторую руку подношение: Золото, Аметист, Алмаз, Жемчуг Края или Гнилую плоть!").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return InteractionResultHolder.fail(sigil);
            }

            if (!sp.isCreative()) {
                offering.shrink(1);
            }

            data.addPatronFavor(favorGain);
            data.setMana(data.getMana() + favorGain * 2.5f);
            data.addXp(favorGain / 2);

            // Unlock deeper Warlock spells as Patron Favor reaches milestones!
            int favor = data.getPatronFavor();
            if (favor >= 40) {
                ClassManager.tryAnnounceSpellUnlock(sp, data, ArcaneSpell.GLACIAL_LANCE);
                ClassManager.tryAnnounceSpellUnlock(sp, data, ArcaneSpell.THUNDERSTEP);
            }
            if (favor >= 75) {
                ClassManager.tryAnnounceSpellUnlock(sp, data, ArcaneSpell.FROST_NOVA);
                ClassManager.tryAnnounceSpellUnlock(sp, data, ArcaneSpell.TEMPEST_JUDGMENT);
            }
            if (favor >= 95) {
                ClassManager.tryAnnounceSpellUnlock(sp, data, ArcaneSpell.BLIZZARD_TEMPEST);
            }

            ServerLevel sl = sp.serverLevel();
            SpellVfx.spawnRuneHexagram(sl, sp.position().add(0, 0.2, 0), 2.0, sl.getGameTime() * 0.1, SpellVfx.WARLOCK_VOID);
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.2f, 0.8f);

            sp.displayClientMessage(Component.literal("Покровитель принял жертву! Благосклонность: " + favor + "/100").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            ModNetworking.syncToPlayer(sp, false);
        }
        return InteractionResultHolder.sidedSuccess(sigil, level.isClientSide);
    }

    private int getOfferingValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(Items.DIAMOND) || stack.is(Items.ENDER_PEARL) || stack.is(Items.NETHER_STAR)) return 35;
        if (stack.is(Items.GOLD_INGOT) || stack.is(Items.AMETHYST_SHARD) || stack.is(Items.EMERALD)) return 20;
        if (stack.is(Items.LAPIS_LAZULI) || stack.is(Items.BLAZE_POWDER) || stack.is(Items.GHAST_TEAR)) return 18;
        if (stack.is(Items.ROTTEN_FLESH) || stack.is(Items.BONE) || stack.is(Items.SPIDER_EYE)) return 8;
        return 0;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Ритуальная печать Колдуна.").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Подноси дары во второй руке (ПКМ) для Маны и новых даров Покровителя.").withStyle(ChatFormatting.GRAY));
    }
}
