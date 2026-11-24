package com.arcaneorigins.item;

import com.arcaneorigins.data.ModAttachments;
import com.arcaneorigins.data.PlayerClassData;
import com.arcaneorigins.network.ModNetworking;
import com.arcaneorigins.spell.ArcaneSpell;
import com.arcaneorigins.spell.SpellVfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class SpellScrollItem extends Item {
    private final ArcaneSpell spell;

    public SpellScrollItem(Properties properties, ArcaneSpell spell) {
        super(properties);
        this.spell = spell;
    }

    public ArcaneSpell getSpell() {
        return spell;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            PlayerClassData data = sp.getData(ModAttachments.CLASS_DATA);
            if (!data.hasChosenClass()) {
                sp.displayClientMessage(Component.literal("Сначала выбери класс (нажми G)!").withStyle(ChatFormatting.YELLOW), true);
                return InteractionResultHolder.fail(stack);
            }
            if (data.hasSpellUnlocked(spell)) {
                sp.displayClientMessage(Component.literal("Ты уже изучил заклинание «" + spell.getDisplayName() + "»!").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }

            data.unlockSpell(spell);
            data.setSelectedSpellId(spell.getId());
            if (!sp.isCreative()) {
                stack.shrink(1);
            }

            ServerLevel sl = sp.serverLevel();
            SpellVfx.spawnHorizontalCircle(sl, sp.position().add(0, 1.0, 0), 1.6, 24, SpellVfx.ARCANE_GOLD);
            sl.sendParticles(ParticleTypes.ENCHANT, sp.getX(), sp.getY() + 1.2, sp.getZ(), 35, 0.7, 0.7, 0.7, 0.6);
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.1f);
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.25f);

            sp.sendSystemMessage(Component.literal("✦ Изучено заклинание: ")
                    .withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(spell.getDisplayName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
            ModNetworking.syncToPlayer(sp, false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Запечатанный свиток: " + spell.getDisplayName() + " [" + spell.getSchool().getDisplayName() + "]")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(spell.getDescription()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Нажми ПКМ, чтобы сорвать печать и изучить заклинание.").withStyle(ChatFormatting.AQUA));
    }
}
