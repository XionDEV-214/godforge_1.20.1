package com.eisiadev.godforge.recipe;

import com.eisiadev.godforge.GodforgeMod;
import com.eisiadev.godforge.GodforgeRecipeTypes;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.MapIngredientPool;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.core.mixins.IngredientAccessor;
import com.gregtechceu.gtceu.core.mixins.ItemValueAccessor;
import com.gregtechceu.gtceu.core.mixins.TagValueAccessor;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TagsUpdatedEvent;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GodforgeRecipeGenerator {

    private GodforgeRecipeGenerator() {}

    private static final int MAX_CHANCE = ChanceLogic.getMaxChancedValue();
    private static final int PLASMA_AMOUNT = 1000;
    private static final int BASE_DURATION = 50;
    private static final long PLASMA_EUT = GTValues.VA[GTValues.ZPM];
    private static final long FURNACE_EUT = 4;

    private static WeakReference<ReloadableServerResources> loadingResources = new WeakReference<>(null);
    private static final Set<ResourceLocation> lastGenerated = new HashSet<>();

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        loadingResources = new WeakReference<>(event.getServerResources());
    }

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) return;
        ReloadableServerResources resources = loadingResources.get();
        if (resources == null) return;
        try {
            inject(resources.getRecipeManager());
        } catch (Throwable t) {
            GodforgeMod.LOGGER.error("[Godforge] Failed to generate Forge of the Gods recipes", t);
        }
    }

    private static synchronized void inject(RecipeManager manager) {
        long start = System.currentTimeMillis();

        List<GTRecipe> generated = new ArrayList<>();
        Set<Material> blasted = new HashSet<>();
        int smeltery = addAll(generated, smelteryRecipes(manager, blasted));
        int furnace = addAll(generated, furnaceRecipes(manager, blasted));
        int plasma = addAll(generated, plasmaRecipes());
        int fusion = addAll(generated, copyAll(manager, GTRecipeTypes.FUSION_RECIPES, GodforgeRecipeTypes.FUSION,
                "godforge_fusion"));
        int exotic = addAll(generated, copyAll(manager, GodforgeRecipeTypes.FORGE_OF_THE_GODS,
                GodforgeRecipeTypes.EXOTICIZER, "godforge_exoticizer"));

        List<Recipe<?>> all = new ArrayList<>(manager.getRecipes().size() + generated.size());
        Set<ResourceLocation> ids = new HashSet<>();
        for (Recipe<?> recipe : manager.getRecipes()) {
            if (lastGenerated.contains(recipe.getId())) continue;
            if (ids.add(recipe.getId())) all.add(recipe);
        }
        lastGenerated.clear();
        for (GTRecipe recipe : generated) {
            if (ids.add(recipe.getId())) {
                all.add(recipe);
                lastGenerated.add(recipe.getId());
            }
        }
        manager.replaceRecipes(all);

        for (GTRecipeType type : new GTRecipeType[] { GodforgeRecipeTypes.SMELTERY, GodforgeRecipeTypes.PLASMAFY,
                GodforgeRecipeTypes.DEPLASMAFY, GodforgeRecipeTypes.FUSION, GodforgeRecipeTypes.EXOTICIZER }) {
            type.beginStagingRecipes();
            for (GTRecipe recipe : manager.getAllRecipesFor(type)) {
                type.getAdditionHandler().addStaging(recipe);
            }
            type.getAdditionHandler().completeStaging();
        }
        MapIngredientPool.clear();

        GodforgeMod.LOGGER.info(
                "[Godforge] Generated {} blast + {} furnace smeltery, {} plasma, {} fusion, {} exoticizer recipes in {} ms",
                smeltery, furnace, plasma, fusion, exotic, System.currentTimeMillis() - start);
    }

    private static int addAll(List<GTRecipe> target, List<GTRecipe> source) {
        target.addAll(source);
        return source.size();
    }

    private static List<GTRecipe> smelteryRecipes(RecipeManager manager, Set<Material> blasted) {
        Map<Material, EnergyStack> freezerEU = new HashMap<>();
        for (GTRecipe recipe : sorted(manager.getAllRecipesFor(GTRecipeTypes.VACUUM_RECIPES))) {
            List<Content> inputs = consumed(recipe.inputs.get(ItemRecipeCapability.CAP));
            if (inputs.size() != 1) continue;
            MaterialEntry entry = entryOf(inputs.get(0));
            if (entry == null || entry.tagPrefix() != TagPrefix.ingotHot) continue;
            freezerEU.merge(entry.material(), recipe.getInputEUt(),
                    (a, b) -> a.getTotalEU() <= b.getTotalEU() ? a : b);
        }

        Map<Material, GTRecipe> result = new LinkedHashMap<>();
        for (GTRecipe recipe : sorted(manager.getAllRecipesFor(GTRecipeTypes.BLAST_RECIPES))) {
            List<Content> inputs = consumed(recipe.inputs.get(ItemRecipeCapability.CAP));
            if (inputs.size() != 1) continue;
            Content input = inputs.get(0);
            MaterialEntry inEntry = entryOf(input);
            if (inEntry == null || inEntry.tagPrefix() != TagPrefix.dust) continue;
            Material material = inEntry.material();
            if (result.containsKey(material)) continue;

            List<Content> outputs = recipe.outputs.get(ItemRecipeCapability.CAP);
            if (outputs == null || outputs.size() != 1) continue;
            Content output = outputs.get(0);
            if (output.chance < output.maxChance) continue;
            MaterialEntry outEntry = entryOf(output);
            if (outEntry == null || !outEntry.material().equals(material)) continue;
            boolean hot = outEntry.tagPrefix() == TagPrefix.ingotHot;
            if (!hot && outEntry.tagPrefix() != TagPrefix.ingot) continue;

            int amount = amountOf(input);
            if (amountOf(output) != amount) continue;
            ItemStack ingot = ChemicalHelper.get(TagPrefix.ingot, material, amount);
            if (ingot.isEmpty()) continue;

            EnergyStack eu = recipe.getInputEUt();
            if (hot) {
                EnergyStack cooling = freezerEU.get(material);
                if (cooling != null && cooling.getTotalEU() > eu.getTotalEU()) eu = cooling;
            }

            Map<RecipeCapability<?>, List<Content>> in = new HashMap<>();
            in.put(ItemRecipeCapability.CAP, new ArrayList<>(List.of(input)));
            Map<RecipeCapability<?>, List<Content>> out = new HashMap<>();
            out.put(ItemRecipeCapability.CAP, new ArrayList<>(List.of(
                    new Content(SizedIngredient.create(ingot), MAX_CHANCE, MAX_CHANCE, 0))));

            String path = "godforge_smeltery/" + material.getResourceLocation().getNamespace() + "/" +
                    material.getResourceLocation().getPath();
            result.put(material, build(GodforgeRecipeTypes.SMELTERY, GodforgeMod.id(path), in, out,
                    new HashMap<>(), eu, BASE_DURATION));
        }
        blasted.addAll(result.keySet());
        return new ArrayList<>(result.values());
    }

    private static List<GTRecipe> furnaceRecipes(RecipeManager manager, Set<Material> blasted) {
        RegistryAccess access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        List<SmeltingRecipe> recipes = new ArrayList<>(manager.getAllRecipesFor(RecipeType.SMELTING));
        recipes.sort(Comparator.comparing(r -> r.getId().toString()));

        List<GTRecipe> result = new ArrayList<>();
        for (SmeltingRecipe recipe : recipes) {
            if (recipe.getIngredients().size() != 1) continue;
            Ingredient ingredient = recipe.getIngredients().get(0);
            if (ingredient == Ingredient.EMPTY) continue;
            ItemStack product = recipe.getResultItem(access);
            if (product.isEmpty()) continue;
            MaterialEntry entry = entryOf(ingredient);
            if (entry != null && entry.tagPrefix() == TagPrefix.dust && blasted.contains(entry.material())) continue;

            Map<RecipeCapability<?>, List<Content>> in = new HashMap<>();
            in.put(ItemRecipeCapability.CAP, new ArrayList<>(List.of(
                    new Content(SizedIngredient.create(ingredient, 1), MAX_CHANCE, MAX_CHANCE, 0))));
            Map<RecipeCapability<?>, List<Content>> out = new HashMap<>();
            out.put(ItemRecipeCapability.CAP, new ArrayList<>(List.of(
                    new Content(SizedIngredient.create(product.copy()), MAX_CHANCE, MAX_CHANCE, 0))));

            ResourceLocation id = recipe.getId();
            String path = "godforge_smeltery/furnace/" + id.getNamespace() + "/" + id.getPath();
            result.add(build(GodforgeRecipeTypes.SMELTERY, GodforgeMod.id(path), in, out, new HashMap<>(),
                    new EnergyStack(FURNACE_EUT), BASE_DURATION));
        }
        return result;
    }

    private static List<GTRecipe> plasmaRecipes() {
        List<GTRecipe> result = new ArrayList<>();
        List<Material> materials = new ArrayList<>(GTCEuAPI.materialManager.getRegisteredMaterials());
        materials.sort(Comparator.comparing(m -> m.getResourceLocation().toString()));
        for (Material material : materials) {
            FluidProperty property = material.getProperty(PropertyKey.FLUID);
            if (property == null) continue;
            Fluid plasma = property.get(FluidStorageKeys.PLASMA);
            if (plasma == null) continue;
            Fluid liquid = property.get(FluidStorageKeys.LIQUID);
            if (liquid == null) liquid = property.get(FluidStorageKeys.MOLTEN);
            if (liquid == null) continue;

            String name = material.getResourceLocation().getNamespace() + "/" +
                    material.getResourceLocation().getPath();
            result.add(fluidConversion(GodforgeRecipeTypes.PLASMAFY, "godforge_plasmafy/" + name, liquid, plasma));
            result.add(fluidConversion(GodforgeRecipeTypes.DEPLASMAFY, "godforge_deplasmafy/" + name, plasma,
                    liquid));
        }
        return result;
    }

    private static GTRecipe fluidConversion(GTRecipeType type, String path, Fluid from, Fluid to) {
        Map<RecipeCapability<?>, List<Content>> in = new HashMap<>();
        in.put(FluidRecipeCapability.CAP, new ArrayList<>(List.of(
                new Content(FluidIngredient.of(from, PLASMA_AMOUNT), MAX_CHANCE, MAX_CHANCE, 0))));
        Map<RecipeCapability<?>, List<Content>> out = new HashMap<>();
        out.put(FluidRecipeCapability.CAP, new ArrayList<>(List.of(
                new Content(FluidIngredient.of(to, PLASMA_AMOUNT), MAX_CHANCE, MAX_CHANCE, 0))));
        return build(type, GodforgeMod.id(path), in, out, new HashMap<>(), new EnergyStack(PLASMA_EUT),
                BASE_DURATION);
    }

    private static List<GTRecipe> copyAll(RecipeManager manager, GTRecipeType from, GTRecipeType to,
                                          String prefix) {
        List<GTRecipe> result = new ArrayList<>();
        for (GTRecipe recipe : sorted(manager.getAllRecipesFor(from))) {
            ResourceLocation id = recipe.getId();
            String path = id.getPath();
            int slash = path.indexOf('/');
            if (slash >= 0) path = path.substring(slash + 1);
            ResourceLocation newId = GodforgeMod.id(prefix + "/" + id.getNamespace() + "/" + path);

            result.add(new GTRecipe(to, newId,
                    copy(recipe.inputs), copy(recipe.outputs), copy(recipe.tickInputs), copy(recipe.tickOutputs),
                    new HashMap<>(recipe.inputChanceLogics), new HashMap<>(recipe.outputChanceLogics),
                    new HashMap<>(recipe.tickInputChanceLogics), new HashMap<>(recipe.tickOutputChanceLogics),
                    new ArrayList<>(recipe.conditions), new ArrayList<>(recipe.ingredientActions),
                    recipe.data.copy(), recipe.duration, GTRecipeCategory.DEFAULT, recipe.groupColor));
        }
        return result;
    }

    private static GTRecipe build(GTRecipeType type, ResourceLocation id,
                                  Map<RecipeCapability<?>, List<Content>> inputs,
                                  Map<RecipeCapability<?>, List<Content>> outputs,
                                  Map<RecipeCapability<?>, ChanceLogic> outputChanceLogics,
                                  @Nullable EnergyStack eu, int duration) {
        Map<RecipeCapability<?>, List<Content>> tickInputs = new HashMap<>();
        if (eu != null && !eu.isEmpty()) EURecipeCapability.putEUContent(tickInputs, eu);
        return new GTRecipe(type, id,
                inputs, outputs, tickInputs, new HashMap<>(),
                new HashMap<>(), outputChanceLogics, new HashMap<>(), new HashMap<>(),
                new ArrayList<>(), new ArrayList<>(), new CompoundTag(), duration, GTRecipeCategory.DEFAULT, -1);
    }

    private static Map<RecipeCapability<?>, List<Content>> copy(Map<RecipeCapability<?>, List<Content>> map) {
        Map<RecipeCapability<?>, List<Content>> result = new HashMap<>();
        map.forEach((cap, list) -> result.put(cap, new ArrayList<>(list)));
        return result;
    }

    private static List<GTRecipe> sorted(List<GTRecipe> recipes) {
        List<GTRecipe> list = new ArrayList<>(recipes);
        list.sort(Comparator.comparing(r -> r.getId().toString()));
        return list;
    }

    private static List<Content> consumed(@Nullable List<Content> contents) {
        List<Content> result = new ArrayList<>();
        if (contents == null) return result;
        for (Content content : contents) {
            if (content.chance > 0) result.add(content);
        }
        return result;
    }

    private static int amountOf(Content content) {
        Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
        return ingredient instanceof SizedIngredient sized ? sized.getAmount() : 1;
    }

    @Nullable
    private static MaterialEntry entryOf(Content content) {
        return entryOf(ItemRecipeCapability.CAP.of(content.content));
    }

    @Nullable
    private static MaterialEntry entryOf(Ingredient ingredient) {
        Ingredient inner = SizedIngredient.getInner(ingredient);
        MaterialEntry entry = null;
        Ingredient.Value[] values = ((IngredientAccessor) inner).getValues();
        if (values != null && values.length > 0) {
            Ingredient.Value value = values[0];
            if (value instanceof Ingredient.TagValue tagValue) {
                entry = ChemicalHelper.getMaterialEntry(((TagValueAccessor) tagValue).getTag());
            } else if (value instanceof Ingredient.ItemValue itemValue) {
                entry = ChemicalHelper.getMaterialEntry(((ItemValueAccessor) itemValue).getItem().getItem());
            }
        } else {
            ItemStack[] stacks = inner.getItems();
            if (stacks.length > 0) entry = ChemicalHelper.getMaterialEntry(stacks[0].getItem());
        }
        return entry == null || entry.isEmpty() ? null : entry;
    }
}
