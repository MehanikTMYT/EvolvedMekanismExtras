package io.github.masyumero.emextras.datagen.common.recipe;

import mekanism.common.resource.PrimaryResource;
import mekanism.common.resource.ResourceType;
import mekanism.common.tags.MekanismTags;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public abstract class BaseRecipeProvider extends RecipeProvider.Runner {

    // port-26.1.2: Runner owns asynchronous registry lookup and recipe output.
    protected BaseRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new RecipeProvider(registries, output) {
            @Override
            protected void buildRecipes() {
                addRecipes(output, registries);
                for (ISubRecipeProvider provider : getSubRecipeProviders()) {
                    provider.addRecipes(output, registries);
                }
            }
        };
    }

    @Override
    public String getName() {
        return "EMExtras recipes";
    }

    protected abstract void addRecipes(RecipeOutput output, HolderLookup.Provider registries);

    /**
     * Gets all the sub/offloaded recipe providers that this recipe provider has.
     *
     * @implNote This is only called once per provider so there is no need to bother caching the list that this returns
     */
    protected List<ISubRecipeProvider> getSubRecipeProviders() {
        return Collections.emptyList();
    }

    public static Ingredient createIngredient(TagKey<Item> itemTag, Item... items) {
        return createIngredient(Stream.of(itemTag), Arrays.stream(items));
    }

    private static Ingredient createIngredient(Stream<TagKey<Item>> itemTags, Stream<Item> items) {
        return net.neoforged.neoforge.common.crafting.CompoundIngredient.of(Stream.concat(
                itemTags.map(BaseRecipeProvider::tagIngredient),
                items.map(Ingredient::of)
        ).toArray(Ingredient[]::new));
    }

    // port-26.1.2: datagen needs a named holder set to encode a tag reference.
    public static Ingredient tagIngredient(TagKey<Item> tag) {
        return Ingredient.of(net.minecraft.core.HolderSet.emptyNamed(net.minecraft.core.registries.BuiltInRegistries.ITEM, tag));
    }

    @SafeVarargs
    public static Ingredient createIngredient(Holder<Item>... items) {
        return Ingredient.of(Arrays.stream(items).map(Holder::value));
    }

    public static Ingredient difference(TagKey<Item> base, Holder<Item> subtracted) {
        return DifferenceIngredient.of(tagIngredient(base), Ingredient.of(subtracted.value()));
    }

    public static TagKey<Item> osmiumIngot() {
        return Objects.requireNonNull(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.OSMIUM));
    }

    public static TagKey<Item> leadIngot() {
        return Objects.requireNonNull(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.LEAD));
    }

    public static TagKey<Item> tinIngot() {
        return Objects.requireNonNull(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.TIN));
    }
}
