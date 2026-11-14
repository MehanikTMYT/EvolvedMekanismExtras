package io.github.masyumero.emextras.common.integration.mekmm.tile.factory;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Item;
import mekanism.api.chemical.ChemicalResource;
import mekanism.api.chemical.Chemical;
// port-26.1.2: typed container holders and server-level update context.
import mekanism.api.fluid.IFluidTank;
import mekanism.common.capabilities.holder.container.IContainerHolder;
// port-26.1.2: migrate removed annotations and Mekanism/MC resource APIs.
import com.jerry.mekextras.api.recipes.cache.StackableItemStackConstantChemicalToObjectCachedRecipe;
import com.jerry.mekextras.api.recipes.cache.StackablePlantingCachedRecipe;
import com.jerry.mekmm.api.recipes.PlantingRecipe;
import com.jerry.mekmm.api.recipes.cache.PlantingNoPerTickUsageCacheRecipe;
import com.jerry.mekmm.client.recipe_viewer.MMRecipeViewerRecipeType;
import com.jerry.mekmm.common.recipe.MoreMachineRecipeType;
import com.jerry.mekmm.common.tile.machine.TileEntityPlantingStation;
import com.jerry.mekmm.common.upgrade.PlantingUpgradeData;
import io.github.masyumero.emextras.common.integration.mekmm.inventory.slot.EMExtraMoreMachineFactoryInputInventorySlot;
import io.github.masyumero.emextras.common.integration.mekmm.inventory.slot.EMExtraMoreMachineFactoryOutputInventorySlot;
import lombok.Getter;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.SerializationConstants;
import mekanism.api.Upgrade;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.math.MathUtils;
import mekanism.api.recipes.SawmillRecipe.ChanceOutput;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.api.recipes.outputs.OutputHelper;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.config.MekanismConfig;
import mekanism.common.inventory.slot.ChemicalInventorySlot;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.lookup.IDoubleRecipeLookupHandler.ItemChemicalRecipeLookupHandler;
import mekanism.common.recipe.lookup.IRecipeLookupHandler.ConstantUsageRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.DoubleInputRecipeCache;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.ItemChemical;
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import mekanism.common.tile.interfaces.IBoundingBlock;
import mekanism.common.tile.interfaces.IHasDumpButton;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.StatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class TileEntityEMExtraPlantingFactory extends TileEntityEMExtraMoreMachineFactory<PlantingRecipe> implements IBoundingBlock, IHasDumpButton, ConstantUsageRecipeLookupHandler,
        ItemChemicalRecipeLookupHandler<PlantingRecipe> {

    // port-26.1.2: compare resource identities against recipe output templates.
    protected static final DoubleInputRecipeCache.CheckRecipeType<Item, ItemResource, Chemical, ChemicalResource, PlantingRecipe, PackedResource> OUTPUT_CHECK = (recipe, itemStack, chemicalStack, output) -> {
        ChanceOutput chanceOutput = recipe.getOutput(itemStack.toStack(), chemicalStack.toStack(1));
        ItemResource firstStack = output.firstStack;
        ItemResource secondaryStack = output.secondaryStack;
        if (firstStack.isEmpty() || firstStack.matches(chanceOutput.getMainOutput())) {
            if (secondaryStack.isEmpty()) {
                return true;
            }
            ItemStackTemplate secondaryOutput = chanceOutput.getMaxSecondaryOutput();
            return secondaryOutput == null || secondaryStack.matches(secondaryOutput);
        }
        return false;
    };


    private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
            RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_INPUT,
            RecipeError.NOT_ENOUGH_SECONDARY_INPUT,
            RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
            TileEntityPlantingStation.NOT_ENOUGH_SPACE_SECONDARY_OUTPUT_ERROR,
            RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
    private static final Set<RecipeError> GLOBAL_ERROR_TYPES = Set.of(
            RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_SECONDARY_INPUT);

    private IInputHandler<net.minecraft.world.item.Item, @NotNull ItemStack>[] inputHandlers;
    private final IInputHandler<mekanism.api.chemical.Chemical, @NotNull ChemicalStack> chemicalInputHandler;
    private IOutputHandler<ChanceOutput>[] outputHandlers;

    ChemicalInventorySlot chemicalSlot;

    @Getter
    IChemicalTank chemicalTank;

    private final StackableItemStackConstantChemicalToObjectCachedRecipe.StackableChemicalUsageMultiplier chemicalUsageMultiplier;
    private double chemicalPerTickMeanMultiplier = 1;
    private long baseTotalUsage;
    private final int[] usedSoFar;

    public TileEntityEMExtraPlantingFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state, TRACKED_ERROR_TYPES, GLOBAL_ERROR_TYPES);
        configComponent.setupInputConfig(TransmissionType.CHEMICAL, chemicalTank);
        configComponent.addDisabledSides(RelativeSide.TOP);

        chemicalInputHandler = InputHelper.getConstantInputHandler(chemicalTank);

        baseTotalUsage = BASE_TICKS_REQUIRED;
        usedSoFar = new int[tier.processes];
        if (useStatisticalMechanics()) {
            chemicalUsageMultiplier = (usedSoFar, operatingTicks, operationsSoFar) -> StatUtils.inversePoisson(chemicalPerTickMeanMultiplier);
        } else {
            chemicalUsageMultiplier = StackableItemStackConstantChemicalToObjectCachedRecipe.StackableChemicalUsageMultiplier.constantUse(() -> baseTotalUsage, this::getChemicalTicksRequired);
        }
    }

    @Override
    public @Nullable IContainerHolder<IChemicalTank> getInitialChemicalTanks(IContentsListener listener) {
        MekContainerHelper<IChemicalTank> builder = MekContainerHelper.forSideWithChemicalConfig(this);
        chemicalTank = BasicChemicalTank.input(TileEntityPlantingStation.MAX_GAS * tier.processes * tier.processes, this::containsRecipeB, markAllMonitorsChanged(listener));
        builder.addContainer(chemicalTank);
        return builder.build();
    }

    @Override
    protected void addSlots(MekContainerHelper<IInventorySlot> builder, IContentsListener listener, IContentsListener updateSortingListener) {
        inputHandlers = new IInputHandler[tier.processes];
        outputHandlers = new IOutputHandler[tier.processes];
        processInfoSlots = new ProcessInfo[tier.processes];
        for (int i = 0; i < tier.processes; i++) {
            FactoryRecipeCacheLookupMonitor<PlantingRecipe> lookupMonitor = recipeCacheLookupMonitors[i];
            IContentsListener updateSortingAndUnpause = () -> {
                updateSortingListener.onContentsChanged();
                lookupMonitor.unpause();
            };
            EMExtraMoreMachineFactoryOutputInventorySlot outputSlot = EMExtraMoreMachineFactoryOutputInventorySlot.at(this, updateSortingAndUnpause, getXPos(i), 57);
            EMExtraMoreMachineFactoryOutputInventorySlot secondaryOutputSlot = EMExtraMoreMachineFactoryOutputInventorySlot.at(this, updateSortingAndUnpause, getXPos(i), 77);
            // Note: As we are an item factory that has comparator's based on items we can just use the monitor as a
            // listener directly
            EMExtraMoreMachineFactoryInputInventorySlot inputSlot = EMExtraMoreMachineFactoryInputInventorySlot.create(this, i, outputSlot, secondaryOutputSlot, lookupMonitor, getXPos(i), 13);
            int index = i;
            builder.addContainer(inputSlot).tracksWarnings(slot -> slot.warning(WarningType.NO_MATCHING_RECIPE, getWarningCheck(RecipeError.NOT_ENOUGH_INPUT, index)));
            builder.addContainer(outputSlot).tracksWarnings(slot -> slot.warning(WarningType.NO_SPACE_IN_OUTPUT, getWarningCheck(RecipeError.NOT_ENOUGH_OUTPUT_SPACE, index)));
            builder.addContainer(secondaryOutputSlot).tracksWarnings(slot -> slot.warning(WarningType.NO_SPACE_IN_OUTPUT,
                    getWarningCheck(TileEntityPlantingStation.NOT_ENOUGH_SPACE_SECONDARY_OUTPUT_ERROR, index)));
            inputHandlers[i] = InputHelper.getInputHandler(inputSlot, RecipeError.NOT_ENOUGH_INPUT);
            outputHandlers[i] = OutputHelper.getOutputHandler(outputSlot, RecipeError.NOT_ENOUGH_OUTPUT_SPACE, secondaryOutputSlot,
                    TileEntityPlantingStation.NOT_ENOUGH_SPACE_SECONDARY_OUTPUT_ERROR);
            processInfoSlots[i] = new ProcessInfo(i, inputSlot, outputSlot, secondaryOutputSlot);
        }
        builder.addContainer(chemicalSlot = ChemicalInventorySlot.fillOrConvert(chemicalTank, this::getLevel, listener, 7, 77));
    }

    protected boolean useStatisticalMechanics() {
        return MekanismConfig.usage.randomizedConsumption.get();
    }

    @Nullable
    @Override
    public ChemicalInventorySlot getExtraSlot() {
        return chemicalSlot;
    }

    @Override
    public @NotNull IMekanismRecipeTypeProvider<?, PlantingRecipe, ItemChemical<PlantingRecipe>> getRecipeType() {
        return MoreMachineRecipeType.PLANTING_STATION;
    }

    @Override
    public @Nullable IRecipeViewerRecipeType<PlantingRecipe> recipeViewerType() {
        return MMRecipeViewerRecipeType.PLANTING_STATION;
    }

    @Override
    public @Nullable PlantingRecipe getRecipe(int cacheIndex) {
        return findFirstRecipe(inputHandlers[cacheIndex], chemicalInputHandler);
    }

    @Override
    public @NotNull CachedRecipe<PlantingRecipe> createNewCachedRecipe(@NotNull PlantingRecipe recipe, int cacheIndex) {
        CachedRecipe<PlantingRecipe> cachedRecipe;
        if (recipe.perTickUsage()) {
            cachedRecipe = StackablePlantingCachedRecipe.planting(recipe, recheckAllRecipeErrors[cacheIndex], inputHandlers[cacheIndex], chemicalInputHandler,
                    chemicalUsageMultiplier, used -> usedSoFar[cacheIndex] = Math.toIntExact(used), outputHandlers[cacheIndex]);
        } else {
            cachedRecipe = PlantingNoPerTickUsageCacheRecipe.planting(recipe, recheckAllRecipeErrors[cacheIndex], inputHandlers[cacheIndex], chemicalInputHandler, outputHandlers[cacheIndex]);
        }
        return cachedRecipe
                // 设置错误更改
                .setErrorsChanged(errors -> errorTracker.onErrorsChanged(errors, cacheIndex))
                .setCanHolderFunction(this::canFunction)
                .setActive(active -> setActiveState(active, cacheIndex))
                .setEnergyRequirements(energyContainer::getEnergyPerTick, energyContainer)
                .setRequiredTicks(this::getTicksRequired)
                .setOnFinish(this::markForSave)
                .setOperatingTicksChanged(operatingTicks -> progress[cacheIndex] = operatingTicks)
                .setBaselineMaxOperations(this::getOperationsPerTick);
    }

    @Override
    protected boolean isCachedRecipeValid(@Nullable CachedRecipe<PlantingRecipe> cached, @NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        if (cached != null) {
            PlantingRecipe cachedRecipe = cached.getRecipe();
            return cachedRecipe.getItemInput().testType(stack) && (chemicalTank.isEmpty() || cachedRecipe.getChemicalInput().testType(chemicalTank.resource()));
        }
        return false;
    }

    @Override
    protected @Nullable PlantingRecipe findRecipe(int process, @NotNull net.neoforged.neoforge.transfer.item.ItemResource fallbackInput, @NotNull IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot) {
        net.neoforged.neoforge.transfer.item.ItemResource extra = secondaryOutputSlot == null ? net.neoforged.neoforge.transfer.item.ItemResource.EMPTY : secondaryOutputSlot.resource();
        return getRecipeType().getInputCache().findTypeBasedRecipe(level, fallbackInput, chemicalTank.resource(), new PackedResource(outputSlot.resource(), extra), OUTPUT_CHECK);
    }

    @Override
    protected void handleSecondaryFuel() {
        chemicalSlot.fillTankOrConvert(null);
    }

    @Override
    protected int getNeededInput(PlantingRecipe recipe, net.neoforged.neoforge.transfer.item.ItemResource inputStack) {
        return MathUtils.clampToInt(recipe.getItemInput().getNeededAmount(inputStack));
    }

    @Override
    public boolean isItemValidForSlot(@NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        return containsRecipeAB(stack, chemicalTank.resource());
    }

    @Override
    public boolean isValidInputItem(@NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        return containsRecipeA(stack);
    }

    @Override
    public boolean hasSecondaryResourceBar() {
        return true;
    }

    @Override
// port-26.1.2: persistence and transaction signatures from ported factory.
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        Optional<java.util.stream.IntStream> savedUsage = input.read(SerializationConstants.USED_SO_FAR, Codec.INT_STREAM);
        if (savedUsage.isPresent()) {
            int[] savedUsed = savedUsage.get().toArray();
            if (tier.processes > savedUsed.length) {
                // If we have more elements than were saved make sure to zero everything so that the ones past the end
                // get properly reset
                Arrays.fill(usedSoFar, 0);
            }
            for (int i = 0; i < tier.processes && i < savedUsed.length; i++) {
                usedSoFar[i] = savedUsed[i];
            }
        } else {
            Arrays.fill(usedSoFar, 0);
        }
    }

    @Override
// port-26.1.2: persistence and transaction signatures from ported factory.
    public void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        output.store(SerializationConstants.USED_SO_FAR, Codec.INT_STREAM, Arrays.stream(usedSoFar));
    }

    @Override
    public int getSavedUsedSoFar(int cacheIndex) {
        return usedSoFar[cacheIndex];
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.SPEED || upgrade == Upgrade.CHEMICAL) {
            if (useStatisticalMechanics()) {
                chemicalPerTickMeanMultiplier = MekanismUtils.getGasPerTickMeanMultiplier(this);
            } else {
                baseTotalUsage = MekanismUtils.getBaseUsage(this, BASE_TICKS_REQUIRED);
            }
        }
    }

    @Override
// port-26.1.2: persistence and transaction signatures from ported factory.
    public void parseUpgradeData(@NotNull IUpgradeData upgradeData, HolderLookup.Provider provider, TransactionContext transaction) {
        if (upgradeData instanceof PlantingUpgradeData data) {
            // Generic factory upgrade data handling
            super.parseUpgradeData(upgradeData, provider, transaction);
            // Copy the contents using NBT so that if it is not actually valid due to a reload we don't crash
            chemicalTank.copyContents(data.chemicalTank, transaction);
            chemicalSlot.copyContents(data.chemicalSlot, transaction);
            System.arraycopy(data.usedSoFar, 0, usedSoFar, 0, data.usedSoFar.length);
        } else {
            Mekanism.logger.warn("Unhandled upgrade data.", new Throwable());
        }
    }

    @NotNull
    @Override
    public PlantingUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        return new PlantingUpgradeData(provider, redstone, getControlType(), getEnergyContainer(), progress, usedSoFar, chemicalTank, energySlot, chemicalSlot,
                inputSlots, outputSlots, isSorting(), getComponents(), problemPath());
    }

    @Override
    public void dump() {
        chemicalTank.setContents(chemicalTank.resource(), 0, null); // port-26.1.2: clear resource container
    }

    protected record PackedResource(ItemResource firstStack, ItemResource secondaryStack) {

    }
}