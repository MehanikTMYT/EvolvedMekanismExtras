package io.github.masyumero.emextras.datagen.common.recipe.builder;

import mekanism.common.recipe.upgrade.MekanismShapedRecipe;
import mekanism.common.registration.impl.BlockRegistryObject;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class MekDataShapedRecipeBuilder extends ExtendedShapedRecipeBuilder {

    private MekDataShapedRecipeBuilder(Holder<Item> result, int count) {
        super(result, count);
    }

    public static MekDataShapedRecipeBuilder shapedRecipe(BlockRegistryObject<?, ?> result) {
        return shapedRecipe(result, 1);
    }

    public static MekDataShapedRecipeBuilder shapedRecipe(BlockRegistryObject<?, ?> result, int count) {
        return shapedRecipe(result.getItemHolder(), count);
    }

    public static MekDataShapedRecipeBuilder shapedRecipe(Holder<Item> result) {
        return shapedRecipe(result, 1);
    }

    public static MekDataShapedRecipeBuilder shapedRecipe(Holder<Item> result, int count) {
        return new MekDataShapedRecipeBuilder(result, count);
    }

    @Override
    protected Recipe<?> wrapRecipe(ShapedRecipe recipe) {
        return new MekanismShapedRecipe(new Recipe.CommonInfo(recipe.showNotification()), new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(recipe.category(), recipe.group()), recipe.pattern, resultStack());
    }
}
