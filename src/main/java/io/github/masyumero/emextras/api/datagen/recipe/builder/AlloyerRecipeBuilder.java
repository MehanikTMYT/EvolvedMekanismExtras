package io.github.masyumero.emextras.api.datagen.recipe.builder;

// port-26.1.2: migrate removed annotations and Mekanism/MC resource APIs.
import fr.iglee42.evolvedmekanism.impl.BasicAlloyerRecipe;
import mekanism.api.datagen.recipe.MekanismRecipeBuilder;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;

public class AlloyerRecipeBuilder extends MekanismRecipeBuilder<AlloyerRecipeBuilder> {

    private final ItemStackIngredient mainInput;
    private final ItemStackIngredient extraInput;
    private final ItemStackIngredient tertiaryExtraInput;
    // port-26.1.2: datagen runs before default item components are bound.
    private final ItemStackTemplate output;

    protected AlloyerRecipeBuilder(ItemStackIngredient mainInput, ItemStackIngredient extraInput, ItemStackIngredient tertiaryExtraInput, ItemStackTemplate output) {
        this.mainInput = mainInput;
        this.extraInput = extraInput;
        this.tertiaryExtraInput = tertiaryExtraInput;
        this.output = output;
    }

    public static AlloyerRecipeBuilder alloying(ItemStackIngredient mainInput, ItemStackIngredient extraInput, ItemStackIngredient tertiaryExtraInput, ItemStackTemplate output) {
        if (output.count() <= 0) {
            throw new IllegalArgumentException("This alloying recipe requires a non empty item output.");
        }
        return new AlloyerRecipeBuilder(mainInput, extraInput, tertiaryExtraInput, output);
    }

    // port-26.1.2: recipe builders derive their default id from the result item.
    @Override
    public net.minecraft.resources.ResourceKey<Recipe<?>> defaultId() {
        return net.minecraft.data.recipes.RecipeBuilder.getDefaultRecipeId(output);
    }

    @Override
    protected Recipe<?> asRecipe() {
        return new BasicAlloyerRecipe(mainInput, extraInput, tertiaryExtraInput, output);
    }
}
