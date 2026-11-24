package com.arcaneorigins.item;

import com.arcaneorigins.ArcaneOriginsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArcaneOriginsMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARCANE_TAB = CREATIVE_TABS.register(
            "arcane_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.arcaneorigins"))
                    .icon(() -> new ItemStack(ModItems.ARCANE_SPELLBOOK.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.ARCANE_SPELLBOOK.get());
                        output.accept(ModItems.FROST_STAFF.get());
                        output.accept(ModItems.STORM_CATALYST.get());
                        output.accept(ModItems.PATRON_SIGIL.get());
                        output.accept(ModItems.SCROLL_OF_REBIRTH.get());

                        output.accept(ModItems.SCROLL_GLACIAL_LANCE.get());
                        output.accept(ModItems.SCROLL_FROST_NOVA.get());
                        output.accept(ModItems.SCROLL_GLACIAL_WALL.get());
                        output.accept(ModItems.SCROLL_FROST_SPIKE_WAVE.get());
                        output.accept(ModItems.SCROLL_BLIZZARD.get());

                        output.accept(ModItems.SCROLL_ARC_LIGHTNING.get());
                        output.accept(ModItems.SCROLL_THUNDERSTEP.get());
                        output.accept(ModItems.SCROLL_STORM_ORB.get());
                        output.accept(ModItems.SCROLL_SHOCKWAVE_NOVA.get());
                        output.accept(ModItems.SCROLL_TEMPEST.get());

                        output.accept(ModItems.SCROLL_VOID_RIFT.get());
                        output.accept(ModItems.SCROLL_GRAVITY_WELL.get());
                        output.accept(ModItems.SCROLL_ZERO_G_SLAM.get());
                        output.accept(ModItems.SCROLL_VOID_PULL.get());
                        output.accept(ModItems.SCROLL_ABYSSAL_METEOR.get());

                        output.accept(ModItems.SCROLL_FIREBALL.get());
                        output.accept(ModItems.SCROLL_FIRE_TORNADO.get());
                        output.accept(ModItems.SCROLL_PLASMA_BLAST.get());
                        output.accept(ModItems.SCROLL_ERUPTION_PILLARS.get());
                        output.accept(ModItems.SCROLL_METEOR_SHOWER.get());
                    })
                    .build()
    );
}
