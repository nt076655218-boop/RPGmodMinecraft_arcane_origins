package com.arcaneorigins;

import com.arcaneorigins.command.ArcaneCommands;
import com.arcaneorigins.data.ModAttachments;
import com.arcaneorigins.event.ModEvents;
import com.arcaneorigins.item.ModCreativeTabs;
import com.arcaneorigins.item.ModItems;
import com.arcaneorigins.network.ModNetworking;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(ArcaneOriginsMod.MOD_ID)
public class ArcaneOriginsMod {
    public static final String MOD_ID = "arcaneorigins";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcaneOriginsMod(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);

        modEventBus.addListener(ModNetworking::registerPayloads);

        NeoForge.EVENT_BUS.register(ModEvents.class);
        NeoForge.EVENT_BUS.addListener(ArcaneCommands::onRegisterCommands);

        LOGGER.info("Arcane Origins v0.5.0 initialized for NeoForge 1.21.1!");
    }
}
