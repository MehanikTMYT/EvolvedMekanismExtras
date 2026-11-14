package io.github.masyumero.emextras.common.tile.factory;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;
// port-26.1.2: migrate removed annotations and Mekanism/MC resource APIs.
import io.github.masyumero.emextras.common.inventory.slot.EMExtraStackableInputInventorySlot;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.math.MathUtils;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.cache.TwoInputCachedRecipe;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.IDoubleRecipeLookupHandler.DoubleItemRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.DoubleInputRecipeCache.CheckRecipeType;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.DoubleItem;
import mekanism.common.upgrade.CombinerUpgradeData;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.InventoryUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class TileEntityEMExtraCombiningFactory extends TileEntityEMExtraItemToItemFactory<CombinerRecipe> implements DoubleItemRecipeLookupHandler<CombinerRecipe> {

    private static final CheckRecipeType<net.minecraft.world.item.Item, net.neoforged.neoforge.transfer.item.ItemResource, net.minecraft.world.item.Item, net.neoforged.neoforge.transfer.item.ItemResource, CombinerRecipe, net.neoforged.neoforge.transfer.item.ItemResource> OUTPUT_CHECK = (recipe, input, extra, output) -> output.isEmpty() || output.matches(recipe.getOutput(input, extra));
    private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
            RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_INPUT,
            RecipeError.NOT_ENOUGH_SECONDARY_INPUT,
            RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
            RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
    private static final Set<RecipeError> GLOBAL_ERROR_TYPES = Set.of(
            RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_SECONDARY_INPUT);

    private final IInputHandler<net.minecraft.world.item.Item, @NotNull ItemStack> extraInputHandler;

    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getSecondaryInput", docPlaceholder = "secondary input slot")
    EMExtraStackableInputInventorySlot extraSlot;

    public TileEntityEMExtraCombiningFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state, TRACKED_ERROR_TYPES, GLOBAL_ERROR_TYPES);
        extraInputHandler = InputHelper.getInputHandler(extraSlot, RecipeError.NOT_ENOUGH_SECONDARY_INPUT);
    }

    @Override
    protected void addSlots(MekContainerHelper<IInventorySlot> builder, IContentsListener listener, IContentsListener updateSortingListener) {
        super.addSlots(builder, listener, updateSortingListener);
        builder.addContainer(extraSlot = EMExtraStackableInputInventorySlot.at(tier, this::containsRecipeB, markAllMonitorsChanged(listener), 7, 57));
        extraSlot.setSlotType(ContainerSlotType.EXTRA);
    }

    @Nullable
    @Override
    protected EMExtraStackableInputInventorySlot getExtraSlot() {
        return extraSlot;
    }

    @Override
    public boolean isItemValidForSlot(@NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        return containsRecipeAB(stack, extraSlot.resource());
    }

    @Override
    public boolean isValidInputItem(@NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        return containsRecipeA(stack);
    }

    @Override
    protected int getNeededInput(CombinerRecipe recipe, net.neoforged.neoforge.transfer.item.ItemResource inputStack) {
        return MathUtils.clampToInt(recipe.getMainInput().getNeededAmount(inputStack));
    }

    @Override
    protected boolean isCachedRecipeValid(@Nullable CachedRecipe<CombinerRecipe> cached, @NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        if (cached != null) {
            CombinerRecipe cachedRecipe = cached.getRecipe();
            return cachedRecipe.getMainInput().testType(stack) && (extraSlot.isEmpty() || cachedRecipe.getExtraInput().testType(extraSlot.resource()));
        }
        return false;
    }

    @Override
    protected CombinerRecipe findRecipe(int process, @NotNull net.neoforged.neoforge.transfer.item.ItemResource fallbackInput, @NotNull IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot) {
        // TODO: Give it something that is not empty when we don't have a stored secondary stack for getting the output?
        return getRecipeType().getInputCache().findTypeBasedRecipe(level, fallbackInput, extraSlot.resource(), outputSlot.resource(), OUTPUT_CHECK);
    }

    @NotNull
    @Override
    public IMekanismRecipeTypeProvider<RecipeInput, CombinerRecipe, DoubleItem<CombinerRecipe>> getRecipeType() {
        return MekanismRecipeType.COMBINING;
    }

    @Override
    public IRecipeViewerRecipeType<CombinerRecipe> recipeViewerType() {
        return RecipeViewerRecipeType.COMBINING;
    }

    @Nullable
    @Override
    public CombinerRecipe getRecipe(int cacheIndex) {
        return findFirstRecipe(inputHandlers[cacheIndex], extraInputHandler);
    }

    @NotNull
    @Override
    public CachedRecipe<CombinerRecipe> createNewCachedRecipe(@NotNull CombinerRecipe recipe, int cacheIndex) {
        return new TwoInputCachedRecipe<>(recipe, recheckAllRecipeErrors[cacheIndex], inputHandlers[cacheIndex], extraInputHandler, outputHandlers[cacheIndex])
                .setErrorsChanged(errors -> errorTracker.onErrorsChanged(errors, cacheIndex))
                .setCanHolderFunction(this::canFunction)
                .setActive(active -> setActiveState(active, cacheIndex))
                .setEnergyRequirements(energyContainer::getEnergyPerTick, energyContainer)
                .setRequiredTicks(this::getTicksRequired)
                .setOnFinish(this::markForSave)
                .setBaselineMaxOperations(this::getOperationsPerTick)
                .setOperatingTicksChanged(operatingTicks -> progress[cacheIndex] = operatingTicks);
    }

    @Override
// port-26.1.2: persistence and transaction signatures from ported factory.
    public void parseUpgradeData(IUpgradeData upgradeData, HolderLookup.Provider provider, TransactionContext transaction) {
        if (upgradeData instanceof CombinerUpgradeData data) {
            //Generic factory upgrade data handling
            super.parseUpgradeData(upgradeData, provider, transaction);
            extraSlot.copyContents(data.extraSlot, transaction);
        } else {
            Mekanism.logger.warn("Unhandled upgrade data.", new Throwable());
        }
    }

    @NotNull
    @Override
    public CombinerUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        return new CombinerUpgradeData(provider, redstone, getControlType(), getEnergyContainer(), progress, energySlot, extraSlot, inputSlots, outputSlots, isSorting(), getComponents(), problemPath());
    }
}