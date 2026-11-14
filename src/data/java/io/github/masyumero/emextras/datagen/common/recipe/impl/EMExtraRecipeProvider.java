package io.github.masyumero.emextras.datagen.common.recipe.impl;

import io.github.masyumero.emextras.datagen.common.recipe.BaseRecipeProvider;
import io.github.masyumero.emextras.datagen.common.recipe.ISubRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EMExtraRecipeProvider extends BaseRecipeProvider {

    public EMExtraRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
        super(output, provider);
    }

    @Override
    protected void addRecipes(RecipeOutput consumer, HolderLookup.Provider registries) {
    }

    @Override
    protected List<ISubRecipeProvider> getSubRecipeProviders() {
        return List.of(
                new AlloyingRecipeProvider(),
                new CombinerRecipeProvider(),
                new FactoryRecipeProvider()
        );
    }
}
