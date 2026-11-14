package io.github.masyumero.emextras.common.tile.factory;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Item;
// port-26.1.2: migrate removed annotations and Mekanism/MC resource APIs.
import com.jerry.mekextras.api.recipes.outputs.ExtraOutputHelper;
import io.github.masyumero.emextras.common.inventory.slot.EMExtraFactoryInputInventorySlot;
import io.github.masyumero.emextras.common.inventory.slot.EMExtraFactoryOutputInventorySlot;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.math.MathUtils;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.SawmillRecipe.ChanceOutput;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.cache.OneInputCachedRecipe;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.ISingleRecipeLookupHandler.ItemRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.SingleItem;
import mekanism.common.recipe.lookup.cache.SingleInputRecipeCache.CheckRecipeType;
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import mekanism.common.tile.machine.TileEntityPrecisionSawmill;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.upgrade.SawmillUpgradeData;
import mekanism.common.util.InventoryUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class TileEntityEMExtraSawingFactory extends TileEntityEMExtraFactory<SawmillRecipe> implements ItemRecipeLookupHandler<SawmillRecipe> {

    // port-26.1.2: compare resource identities against recipe output templates.
    private static final CheckRecipeType<Item, ItemResource, SawmillRecipe, ItemResource, ItemResource> OUTPUT_CHECK = (recipe, input, outputContents, secondaryOutputContents) -> {
        ChanceOutput chanceOutput = recipe.getOutput(input);
        if (outputContents.isEmpty() || outputContents.matches(chanceOutput.getMainOutput())) {
            //If the input is good and the primary output matches, make sure that the secondary
            // output of this recipe will stack with what is currently in the secondary slot
            if (secondaryOutputContents.isEmpty()) {
                return true;
            }
            ItemStackTemplate secondaryOutput = chanceOutput.getMaxSecondaryOutput();
            return secondaryOutput == null || secondaryOutputContents.matches(secondaryOutput);
        }
        return false;
    };

    private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
            RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_INPUT,
            RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
            TileEntityPrecisionSawmill.NOT_ENOUGH_SPACE_SECONDARY_OUTPUT_ERROR,
            RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
    private static final Set<RecipeError> GLOBAL_ERROR_TYPES = Set.of(RecipeError.NOT_ENOUGH_ENERGY);

    protected IInputHandler<net.minecraft.world.item.Item, @NotNull ItemStack>[] inputHandlers;
    protected IOutputHandler<@NotNull ChanceOutput>[] outputHandlers;

    public TileEntityEMExtraSawingFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state, TRACKED_ERROR_TYPES, GLOBAL_ERROR_TYPES);
    }

    @Override
    protected void addSlots(MekContainerHelper<IInventorySlot> builder, IContentsListener listener, IContentsListener updateSortingListener) {
        inputHandlers = new IInputHandler[tier.processes];
        outputHandlers = new IOutputHandler[tier.processes];
        processInfoSlots = new ProcessInfo[tier.processes];
        int baseX = 27;
        int baseXMult = 19;
        for (int i = 0; i < tier.processes; i++) {
            int xPos = baseX + (i * baseXMult);
            FactoryRecipeCacheLookupMonitor<SawmillRecipe> lookupMonitor = recipeCacheLookupMonitors[i];
            IContentsListener updateSortingAndUnpause = () -> {
                updateSortingListener.onContentsChanged();
                lookupMonitor.unpause();
            };
            EMExtraFactoryOutputInventorySlot outputSlot = EMExtraFactoryOutputInventorySlot.at(this, updateSortingAndUnpause, xPos, 57);
            EMExtraFactoryOutputInventorySlot secondaryOutputSlot = EMExtraFactoryOutputInventorySlot.at(this, updateSortingAndUnpause, xPos, 77);
            // Note: As we are an item factory that has comparator's based on items we can just use the monitor as a
            // listener directly
            EMExtraFactoryInputInventorySlot inputSlot = EMExtraFactoryInputInventorySlot.create(this, i, outputSlot, secondaryOutputSlot, lookupMonitor, xPos, 13);
            int index = i;
            builder.addContainer(inputSlot).tracksWarnings(slot -> slot.warning(WarningType.NO_MATCHING_RECIPE, getWarningCheck(RecipeError.NOT_ENOUGH_INPUT, index)));
            builder.addContainer(outputSlot).tracksWarnings(slot -> slot.warning(WarningType.NO_SPACE_IN_OUTPUT, getWarningCheck(RecipeError.NOT_ENOUGH_OUTPUT_SPACE, index)));
            builder.addContainer(secondaryOutputSlot).tracksWarnings(slot -> slot.warning(WarningType.NO_SPACE_IN_OUTPUT,
                    getWarningCheck(TileEntityPrecisionSawmill.NOT_ENOUGH_SPACE_SECONDARY_OUTPUT_ERROR, index)));
            inputHandlers[i] = InputHelper.getInputHandler(inputSlot, RecipeError.NOT_ENOUGH_INPUT);
            outputHandlers[i] = ExtraOutputHelper.getOutputHandler(outputSlot, RecipeError.NOT_ENOUGH_OUTPUT_SPACE, secondaryOutputSlot,
                    TileEntityPrecisionSawmill.NOT_ENOUGH_SPACE_SECONDARY_OUTPUT_ERROR, this::getOperationsPerTick);
            processInfoSlots[i] = new ProcessInfo(i, inputSlot, outputSlot, secondaryOutputSlot);
        }
    }

    @Override
    public boolean isItemValidForSlot(@NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        // contains recipe in general already validated by isValidInputItem
        return true;
    }

    @Override
    public boolean isValidInputItem(@NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        return containsRecipe(stack);
    }

    @Override
    protected int getNeededInput(SawmillRecipe recipe, net.neoforged.neoforge.transfer.item.ItemResource inputStack) {
        return MathUtils.clampToInt(recipe.getInput().getNeededAmount(inputStack));
    }

    @Override
    protected boolean isCachedRecipeValid(@Nullable CachedRecipe<SawmillRecipe> cached, @NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        return cached != null && cached.getRecipe().getInput().testType(stack);
    }

    @Override
    protected SawmillRecipe findRecipe(int process, @NotNull net.neoforged.neoforge.transfer.item.ItemResource fallbackInput, @NotNull IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot) {
        net.neoforged.neoforge.transfer.item.ItemResource extra = secondaryOutputSlot == null ? net.neoforged.neoforge.transfer.item.ItemResource.EMPTY : secondaryOutputSlot.resource();
        return getRecipeType().getInputCache().findTypeBasedRecipe(level, fallbackInput, outputSlot.resource(), extra, OUTPUT_CHECK);
    }

    @NotNull
    @Override
    public IMekanismRecipeTypeProvider<SingleRecipeInput, SawmillRecipe, SingleItem<SawmillRecipe>> getRecipeType() {
        return MekanismRecipeType.SAWING;
    }

    @Override
    public IRecipeViewerRecipeType<SawmillRecipe> recipeViewerType() {
        return RecipeViewerRecipeType.SAWING;
    }

    @Nullable
    @Override
    public SawmillRecipe getRecipe(int cacheIndex) {
        return findFirstRecipe(inputHandlers[cacheIndex]);
    }

    @NotNull
    @Override
    public CachedRecipe<SawmillRecipe> createNewCachedRecipe(@NotNull SawmillRecipe recipe, int cacheIndex) {
        return new OneInputCachedRecipe<>(recipe, recheckAllRecipeErrors[cacheIndex], inputHandlers[cacheIndex], outputHandlers[cacheIndex])
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
        if (upgradeData instanceof SawmillUpgradeData) {
            //Validate we have the correct type of data before passing it upwards
            super.parseUpgradeData(upgradeData, provider, transaction);
        } else {
            Mekanism.logger.warn("Unhandled upgrade data.", new Throwable());
        }
    }

    @NotNull
    @Override
    public SawmillUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        return new SawmillUpgradeData(provider, redstone, getControlType(), getEnergyContainer(), progress, energySlot, inputSlots, outputSlots, isSorting(), getComponents(), problemPath());
    }

    // Methods relating to IComputerTile
    @ComputerMethod
    ItemStack getSecondaryOutput(int process) throws ComputerException {
        validateValidProcess(process);
        IInventorySlot secondaryOutputSlot = processInfoSlots[process].secondaryOutputSlot();
        // This should never be null, but in case it is, handle it
        return secondaryOutputSlot == null ? ItemStack.EMPTY : secondaryOutputSlot.resource().toStack(secondaryOutputSlot.amountAsInt());
    }
    // End methods IComputerTile
}
