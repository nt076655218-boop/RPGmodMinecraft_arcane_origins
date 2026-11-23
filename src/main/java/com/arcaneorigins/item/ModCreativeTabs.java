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
                    })
                    .build()
    );
}
