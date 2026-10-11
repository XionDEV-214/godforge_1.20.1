package com.eisiadev.godforge.client;

import com.eisiadev.godforge.GodforgeMod;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

@OnlyIn(Dist.CLIENT)
public final class GodforgeClient {

    private GodforgeClient() {}

    public static void init(IEventBus modBus) {
        DynamicRenderManager.register(ResourceLocation.fromNamespaceAndPath(GodforgeMod.MODID, "forge_of_the_gods"),
                GodforgeRender.TYPE);

        modBus.addListener(GodforgeShaders::onRegisterShaders);
        modBus.addListener((RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener((ResourceManagerReloadListener) rm -> GodforgeMeshes.release()));
    }
}
