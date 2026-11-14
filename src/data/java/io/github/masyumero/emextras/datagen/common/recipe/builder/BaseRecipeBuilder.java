package io.github.masyumero.emextras.datagen.common.recipe.builder;

import mekanism.api.datagen.recipe.MekanismRecipeBuilder;
import net.minecraft.core.Holder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public abstract class BaseRecipeBuilder<BUILDER extends BaseRecipeBuilder<BUILDER>> extends MekanismRecipeBuilder<BUILDER> {

    private final Holder<Item> result;
    private final int count;
    protected RecipeCategory category = RecipeCategory.MISC;
    @Nullable
    protected String group;

    protected BaseRecipeBuilder(Holder<Item> result, int count) {
        this.result = result;
        this.count = count;
    }

    @SuppressWarnings("unchecked")
    private BUILDER self() {
        return (BUILDER) this;
    }

    public BUILDER group(String group) {
        this.group = group;
        return self();
    }

    public BUILDER category(RecipeCategory category) {
        this.category = category;
        return self();
    }

    public void build(RecipeOutput recipeOutput) {
        save(recipeOutput, result);
    }

    @Override
    public net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> defaultId() {
        return net.minecraft.data.recipes.RecipeBuilder.getDefaultRecipeId(resultStack());
    }

    protected net.minecraft.world.item.ItemStackTemplate resultStack() {
        return new net.minecraft.world.item.ItemStackTemplate(result, count);
    }
}
