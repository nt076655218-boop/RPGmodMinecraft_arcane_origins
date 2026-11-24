package com.arcaneorigins.item;

import com.arcaneorigins.classes.ClassManager;
import com.arcaneorigins.data.ModAttachments;
import com.arcaneorigins.data.PlayerClassData;
import com.arcaneorigins.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

public class ScrollOfRebirthItem extends Item {
    public ScrollOfRebirthItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            PlayerClassData data = sp.getData(ModAttachments.CLASS_DATA);
            data.setClassId("");
            data.setSubclassId("");
            data.clearProgression();
            ClassManager.clearModifiers(sp);
            if (!sp.isCreative()) {
                stack.shrink(1);
            }
            sp.serverLevel().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7f, 1.3f);
            ModNetworking.syncToPlayer(sp, true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Сбрасывает текущий класс и открывает меню выбора заново.").withStyle(ChatFormatting.GOLD));
    }
}
