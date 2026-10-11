package com.eisiadev.godforge.client;

import com.eisiadev.godforge.GodforgeMod;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@OnlyIn(Dist.CLIENT)
public final class GodforgeShaders {

    private GodforgeShaders() {}

    @Nullable private static ShaderInstance star;
    @Nullable private static ShaderInstance ring;
    @Nullable private static ShaderInstance beam;

    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(GodforgeMod.MODID, "star"), DefaultVertexFormat.POSITION_TEX),
                    s -> star = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(GodforgeMod.MODID, "ring"), DefaultVertexFormat.POSITION_TEX),
                    s -> ring = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(GodforgeMod.MODID, "beam"), DefaultVertexFormat.POSITION_COLOR_TEX),
                    s -> beam = s);
        } catch (IOException e) {
            throw new RuntimeException("[Godforge] Failed to load shaders", e);
        }
    }

    public static boolean ready() {
        return star != null && ring != null && beam != null;
    }

    public static ShaderInstance star() { return star; }
    public static ShaderInstance ring() { return ring; }
    public static ShaderInstance beam() { return beam; }
}
