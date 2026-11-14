package io.github.masyumero.emextras.api.datagen.recipe.builder;

// port-26.1.2: migrate removed annotations and Mekanism/MC resource APIs.
import fr.iglee42.evolvedmekanism.impl.BasicAlloyerRecipe;
import mekanism.api.datagen.recipe.MekanismRecipeBuilder;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public class AlloyerRecipeBuilder extends MekanismRecipeBuilder<AlloyerRecipeBuilder> {

    private final ItemStackIngredient mainInput;
    private final ItemStackIngredient extraInput;
    private final ItemStackIngredient tertiaryExtraInput;
    private final ItemStack output;

    protected AlloyerRecipeBuilder(ItemStackIngredient mainInput, ItemStackIngredient extraInput, ItemStackIngredient tertiaryExtraInput, ItemStack output) {
        this.mainInput = mainInput;
        this.extraInput = extraInput;
        this.tertiaryExtraInput = tertiaryExtraInput;
        this.output = output;
    }

    public static AlloyerRecipeBuilder alloying(ItemStackIngredient mainInput, ItemStackIngredient extraInput, ItemStackIngredient tertiaryExtraInput, ItemStack output) {
        if (output.isEmpty()) {
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
        return new BasicAlloyerRecipe(mainInput, extraInput, tertiaryExtraInput, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(output));
    }
}
