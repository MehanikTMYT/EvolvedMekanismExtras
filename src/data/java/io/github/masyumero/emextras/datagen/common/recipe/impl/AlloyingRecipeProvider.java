package io.github.masyumero.emextras.datagen.common.recipe.impl;

import com.jerry.mekextras.common.registries.ExtraItems;
import com.jerry.mekextras.common.tags.ExtraTags;
import fr.iglee42.evolvedmekanism.registries.EMItems;
import fr.iglee42.evolvedmekanism.registries.EMTags;
import io.github.masyumero.emextras.EMExtras;
import io.github.masyumero.emextras.api.datagen.recipe.builder.AlloyerRecipeBuilder;
import io.github.masyumero.emextras.common.registry.EMExtraItems;
import io.github.masyumero.emextras.datagen.common.recipe.ISubRecipeProvider;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.registries.MekanismItems;
import mekanism.common.tags.MekanismTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;

public class AlloyingRecipeProvider implements ISubRecipeProvider {

    @Override
    public void addRecipes(RecipeOutput consumer, HolderLookup.Provider registries) {
        String basePath = "alloying/";
        //Tier Installer
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(EMExtraItems.ABSOLUTE_OVERCLOCKED_CONTROL_CIRCUIT, 2),
                IngredientCreatorAccess.item().from(ExtraItems.ABSOLUTE_TIER_INSTALLER),
                IngredientCreatorAccess.item().from(EMItems.OVERCLOCKED_TIER_INSTALLER),
                new net.minecraft.world.item.ItemStackTemplate(EMExtraItems.ABSOLUTE_OVERCLOCKED_TIER_INSTALLER)).save(consumer, EMExtras.rl(basePath + "tier_installer/absolute_overclocked"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(EMExtraItems.SUPREME_QUANTUM_CONTROL_CIRCUIT, 2),
                IngredientCreatorAccess.item().from(ExtraItems.SUPREME_TIER_INSTALLER),
                IngredientCreatorAccess.item().from(EMItems.QUANTUM_TIER_INSTALLER),
                new net.minecraft.world.item.ItemStackTemplate(EMExtraItems.SUPREME_QUANTUM_TIER_INSTALLER)).save(consumer, EMExtras.rl(basePath + "tier_installer/supreme_quantum"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(EMExtraItems.COSMIC_DENSE_CONTROL_CIRCUIT, 2),
                IngredientCreatorAccess.item().from(ExtraItems.COSMIC_TIER_INSTALLER),
                IngredientCreatorAccess.item().from(EMItems.DENSE_TIER_INSTALLER),
                new net.minecraft.world.item.ItemStackTemplate(EMExtraItems.COSMIC_DENSE_TIER_INSTALLER)).save(consumer, EMExtras.rl(basePath + "tier_installer/cosmic_dense"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(EMExtraItems.INFINITE_MULTIVERSAL_CONTROL_CIRCUIT, 2),
                IngredientCreatorAccess.item().from(ExtraItems.INFINITE_TIER_INSTALLER),
                IngredientCreatorAccess.item().from(EMItems.MULTIVERSAL_TIER_INSTALLER),
                new net.minecraft.world.item.ItemStackTemplate(EMExtraItems.INFINITE_MULTIVERSAL_TIER_INSTALLER)).save(consumer, EMExtras.rl(basePath + "tier_installer/infinite_multiversal"));
        //Circuit
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), MekanismTags.Items.ALLOYS_INFUSED, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), MekanismTags.Items.CIRCUITS_BASIC),
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT),
                new net.minecraft.world.item.ItemStackTemplate(MekanismItems.ADVANCED_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/advanced"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), MekanismTags.Items.ALLOYS_REINFORCED, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), MekanismTags.Items.CIRCUITS_ADVANCED),
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT),
                new net.minecraft.world.item.ItemStackTemplate(MekanismItems.ELITE_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/elite"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), MekanismTags.Items.ALLOYS_ATOMIC, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), MekanismTags.Items.CIRCUITS_ELITE),
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT),
                new net.minecraft.world.item.ItemStackTemplate(MekanismItems.ULTIMATE_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/ultimate"));
        //ExtraCircuit
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), ExtraTags.Items.ALLOYS_RADIANCE, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), MekanismTags.Items.CIRCUITS_ULTIMATE),
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT),
                new net.minecraft.world.item.ItemStackTemplate(ExtraItems.ABSOLUTE_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/absolute"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), ExtraTags.Items.ALLOYS_THERMONUCLEAR, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), ExtraTags.Items.CIRCUITS_ABSOLUTE),
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT),
                new net.minecraft.world.item.ItemStackTemplate(ExtraItems.SUPREME_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/supreme"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), ExtraTags.Items.ALLOYS_SHINING, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), ExtraTags.Items.CIRCUITS_SUPREME),
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT),
                new net.minecraft.world.item.ItemStackTemplate(ExtraItems.COSMIC_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/cosmic"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), ExtraTags.Items.ALLOYS_SPECTRUM, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), ExtraTags.Items.CIRCUITS_COSMIC),
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT),
                new net.minecraft.world.item.ItemStackTemplate(ExtraItems.INFINITE_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/infinity"));
        //EvolvedCircuit
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), EMTags.Items.ALLOYS_HYPERCHARGED, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), MekanismTags.Items.CIRCUITS_ULTIMATE),
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT),
                new net.minecraft.world.item.ItemStackTemplate(EMItems.OVERCLOCKED_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/overclocked"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), EMTags.Items.ALLOYS_SUBATOMIC, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), EMTags.Items.CIRCUITS_OVERCLOCKED),
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT),
                new net.minecraft.world.item.ItemStackTemplate(EMItems.QUANTUM_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/quantum"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), EMTags.Items.ALLOYS_SINGULAR, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), EMTags.Items.CIRCUITS_QUANTUM),
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT),
                new net.minecraft.world.item.ItemStackTemplate(EMItems.DENSE_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/dense"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), EMTags.Items.ALLOYS_EXOVERSAL, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), EMTags.Items.CIRCUITS_DENSE),
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT),
                new net.minecraft.world.item.ItemStackTemplate(EMItems.MULTIVERSAL_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/multiversal"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), EMTags.Items.ALLOYS_CREATIVE, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), EMTags.Items.CIRCUITS_MULTIVERSAL),
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT),
                new net.minecraft.world.item.ItemStackTemplate(EMItems.CREATIVE_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/creative"));
        //CombinedCircuit
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), ExtraTags.Items.CIRCUITS_ABSOLUTE),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), EMTags.Items.CIRCUITS_OVERCLOCKED),
                new net.minecraft.world.item.ItemStackTemplate(EMExtraItems.ABSOLUTE_OVERCLOCKED_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/absolute_overclocked"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), ExtraTags.Items.CIRCUITS_SUPREME),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), EMTags.Items.CIRCUITS_QUANTUM),
                new net.minecraft.world.item.ItemStackTemplate(EMExtraItems.SUPREME_QUANTUM_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/supreme_quantum"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), ExtraTags.Items.CIRCUITS_COSMIC),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), EMTags.Items.CIRCUITS_DENSE),
                new net.minecraft.world.item.ItemStackTemplate(EMExtraItems.COSMIC_DENSE_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/cosmic_dense"));
        AlloyerRecipeBuilder.alloying(
                IngredientCreatorAccess.item().from(EMExtraItems.BASE_CONTROL_CIRCUIT, 2),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), ExtraTags.Items.CIRCUITS_INFINITE),
                IngredientCreatorAccess.item().from(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), EMTags.Items.CIRCUITS_MULTIVERSAL),
                new net.minecraft.world.item.ItemStackTemplate(EMExtraItems.INFINITE_MULTIVERSAL_CONTROL_CIRCUIT, 2)).save(consumer, EMExtras.rl(basePath + "circuit/infinite_multiversal"));
    }
}
