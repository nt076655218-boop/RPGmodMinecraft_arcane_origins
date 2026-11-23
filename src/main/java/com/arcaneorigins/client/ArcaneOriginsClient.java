package com.arcaneorigins.client;

import com.arcaneorigins.ArcaneOriginsMod;
import com.arcaneorigins.network.CastSpellPayload;
import com.arcaneorigins.network.ClientClassCache;
import com.arcaneorigins.network.CycleSpellPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(value = ArcaneOriginsMod.MOD_ID, dist = Dist.CLIENT)
public class ArcaneOriginsClient {
    private static int openMenuDelay = 0;

    public ArcaneOriginsClient(IEventBus modEventBus) {
        modEventBus.addListener(this::onRegisterKeyMappings);
        modEventBus.addListener(this::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ArcaneKeyMappings.CAST_SPELL);
        event.register(ArcaneKeyMappings.CYCLE_SPELL);
        event.register(ArcaneKeyMappings.OPEN_CLASS_MENU);
    }

    private void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(ArcaneOriginsMod.MOD_ID, "arcane_hud"),
                ArcaneHudOverlay::render
        );
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Auto-open class selection shortly after joining world
        if (ClientClassCache.pendingOpenMenu) {
            openMenuDelay++;
            if (openMenuDelay >= 15 && mc.screen == null) {
                ClientClassCache.pendingOpenMenu = false;
                openMenuDelay = 0;
                mc.setScreen(new ClassSelectionScreen());
            }
        }

        while (ArcaneKeyMappings.CAST_SPELL.consumeClick()) {
            PacketDistributor.sendToServer(new CastSpellPayload());
        }
        while (ArcaneKeyMappings.CYCLE_SPELL.consumeClick()) {
            PacketDistributor.sendToServer(new CycleSpellPayload(1));
        }
        while (ArcaneKeyMappings.OPEN_CLASS_MENU.consumeClick()) {
            mc.setScreen(new ClassSelectionScreen());
        }
    }
}
