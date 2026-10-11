package com.eisiadev.godforge.client;

import com.eisiadev.godforge.GodforgeMod;

import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;
import com.gregtechceu.gtceu.data.pack.event.RegisterDynamicResourcesEvent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GodforgeMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GodforgeModels {

    private static final String MODID = GodforgeMod.MODID;

    private static final String CASING_TEX = "gtceu:block/casings/godforge/godforge_support_casing";
    private static final String OVERLAY_DIR = "gtceu:block/multiblock/hpca";

    @SubscribeEvent
    public static void onRegisterDynamicResources(RegisterDynamicResourcesEvent event) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, "forge_of_the_gods");
        String blockModelPath = MODID + ":block/forge_of_the_gods";
        String machineId = MODID + ":forge_of_the_gods";

        JsonObject variants = new JsonObject();
        addVariant(variants, "facing=north", blockModelPath, 0, 0);
        addVariant(variants, "facing=south", blockModelPath, 0, 180);
        addVariant(variants, "facing=east", blockModelPath, 0, 90);
        addVariant(variants, "facing=west", blockModelPath, 0, 270);
        JsonObject blockstate = new JsonObject();
        blockstate.add("variants", variants);
        GTDynamicResourcePack.addBlockState(id, blockstate);

        JsonObject blockModel = new JsonObject();
        blockModel.addProperty("parent", "minecraft:block/block");
        blockModel.addProperty("loader", "gtceu:machine");
        blockModel.addProperty("machine", machineId);

        JsonObject textureOverrides = new JsonObject();
        textureOverrides.addProperty("all", CASING_TEX);
        blockModel.add("texture_overrides", textureOverrides);

        JsonObject modelVariants = new JsonObject();
        for (boolean formed : new boolean[] { false, true }) {
            addWorkableVariant(modelVariants, formed, "idle", OVERLAY_DIR + "/overlay_front", OVERLAY_DIR + "/overlay_front_emissive");
            addWorkableVariant(modelVariants, formed, "suspend", OVERLAY_DIR + "/overlay_front_paused", OVERLAY_DIR + "/overlay_front_paused_emissive");
            addWorkableVariant(modelVariants, formed, "waiting", OVERLAY_DIR + "/overlay_front_active", OVERLAY_DIR + "/overlay_front_active_emissive");
            addWorkableVariant(modelVariants, formed, "working", OVERLAY_DIR + "/overlay_front_active", OVERLAY_DIR + "/overlay_front_active_emissive");
        }
        blockModel.add("variants", modelVariants);

        JsonArray dynamicRenders = new JsonArray();
        JsonObject render = new JsonObject();
        render.addProperty("type", MODID + ":forge_of_the_gods");
        dynamicRenders.add(render);
        blockModel.add("dynamic_renders", dynamicRenders);

        GTDynamicResourcePack.addBlockModel(id, blockModel);

        JsonObject item = new JsonObject();
        item.addProperty("parent", blockModelPath);
        GTDynamicResourcePack.addItemModel(id, item);
    }

    private static void addVariant(JsonObject variants, String key, String model, int x, int y) {
        JsonObject v = new JsonObject();
        v.addProperty("model", model);
        if (x != 0) v.addProperty("x", x);
        if (y != 0) v.addProperty("y", y);
        variants.add(key, v);
    }

    private static void addWorkableVariant(JsonObject variants, boolean formed, String status,
                                           String overlay, String overlayEmissive) {
        String key = "is_formed=" + formed + ",recipe_logic_status=" + status;

        JsonObject inner = new JsonObject();
        inner.addProperty("parent", "gtceu:block/machine/template/cube_all/sided");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", CASING_TEX);
        textures.addProperty("overlay_front", overlay);
        textures.addProperty("overlay_front_emissive", overlayEmissive);
        inner.add("textures", textures);

        JsonObject wrapper = new JsonObject();
        wrapper.add("model", inner);
        variants.add(key, wrapper);
    }
}
