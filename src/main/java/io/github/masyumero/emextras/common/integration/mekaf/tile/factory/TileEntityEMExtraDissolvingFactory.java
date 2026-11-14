package io.github.masyumero.emextras.common.integration.mekaf.tile.factory;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.world.item.Item;
import mekanism.api.chemical.ChemicalResource;
import mekanism.api.chemical.Chemical;
// port-26.1.2: typed container holders and server-level update context.
import mekanism.api.fluid.IFluidTank;
import mekanism.common.capabilities.holder.container.IContainerHolder;
// port-26.1.2: migrate removed annotations and Mekanism/MC resource APIs.
import com.jerry.mekaf.common.upgrade.ItemChemicalToChemicalUpgradeData;
import com.jerry.mekextras.api.recipes.cache.StackableItemStackConstantChemicalToObjectCachedRecipe;
import com.jerry.mekextras.api.recipes.cache.StackableItemStackConstantChemicalToObjectCachedRecipe.StackableChemicalUsageMultiplier;
import io.github.masyumero.emextras.common.integration.mekaf.tile.factory.base.TileEntityEMExtraItemToChemicalFactory;
import mekanism.api.IContentsListener;
import mekanism.api.SerializationConstants;
import mekanism.api.Upgrade;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.math.MathUtils;
import mekanism.api.recipes.ChemicalDissolutionRecipe;
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
import mekanism.common.inventory.slot.ChemicalInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.IDoubleRecipeLookupHandler.ItemChemicalRecipeLookupHandler;
import mekanism.common.recipe.lookup.IRecipeLookupHandler.ConstantUsageRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.DoubleInputRecipeCache;
import mekanism.common.recipe.lookup.cache.InputRecipeCache;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.ChemicalSlotInfo;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.interfaces.IHasDumpButton;
import mekanism.common.upgrade.IUpgradeData;
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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class TileEntityEMExtraDissolvingFactory extends TileEntityEMExtraItemToChemicalFactory<ChemicalDissolutionRecipe> implements IHasDumpButton, ConstantUsageRecipeLookupHandler,
        ItemChemicalRecipeLookupHandler<ChemicalDissolutionRecipe> {

    // port-26.1.2: compare resource identities against recipe output templates.
    private static final DoubleInputRecipeCache.CheckRecipeType<Item, ItemResource, Chemical, ChemicalResource, ChemicalDissolutionRecipe, ChemicalResource> OUTPUT_CHECK = (recipe, input, extra, output) -> output.isEmpty() || output.is(recipe.getOutput(input, extra).typeHolder());
    private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
            RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_ENERGY_REDUCED_RATE,
            RecipeError.NOT_ENOUGH_INPUT,
            RecipeError.NOT_ENOUGH_SECONDARY_INPUT,
            RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
            RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
    private static final Set<RecipeError> GLOBAL_ERROR_TYPES = Set.of(
            RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_SECONDARY_INPUT);

    private final IInputHandler<mekanism.api.chemical.Chemical, @NotNull ChemicalStack> chemicalInputHandler;

    private final StackableChemicalUsageMultiplier injectUsageMultiplier;
    private double injectUsage = 1;
    private final int[] usedSoFar;

    public IChemicalTank injectTank;

    ChemicalInventorySlot chemicalInputSlot;

    public TileEntityEMExtraDissolvingFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state, TRACKED_ERROR_TYPES, GLOBAL_ERROR_TYPES);

        ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        if (itemConfig != null) {
            itemConfig.addSlotInfo(DataType.EXTRA, new InventorySlotInfo(true, true, chemicalInputSlot));
        }
        ConfigInfo chemicalConfig = configComponent.getConfig(TransmissionType.CHEMICAL);
        if (chemicalConfig != null) {
            chemicalConfig.addSlotInfo(DataType.INPUT, new ChemicalSlotInfo(true, false, injectTank));
            List<IChemicalTank> ioTank = new ArrayList<>(List.of(injectTank));
            ioTank.addAll(outputChemicalTanks);
            // 这个只能设定一个
            chemicalConfig.addSlotInfo(DataType.INPUT_OUTPUT, new ChemicalSlotInfo(true, true, ioTank));
        }

        // port-26.1.2: superclass initializes ejector.
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM, TransmissionType.CHEMICAL)
                // 有多个储罐时可以使用该方法指定某个罐是否可以弹出
                .setCanTankEject(tank -> tank != injectTank);
        usedSoFar = new int[tier.processes];

        chemicalInputHandler = InputHelper.getConstantInputHandler(injectTank);

        injectUsageMultiplier = (usedSoFar, operatingTicks, operationsSoFar) -> StatUtils.inversePoisson(injectUsage);
    }

    @Override
    protected void addTanks(MekContainerHelper<IChemicalTank> builder, IContentsListener listener, IContentsListener updateSortingListener) {
        super.addTanks(builder, listener, updateSortingListener);
        builder.addContainer(injectTank = BasicChemicalTank.input(MAX_CHEMICAL * tier.processes * tier.processes, this::containsRecipeB, markAllMonitorsChanged(listener)));
    }

    @Override
    protected void addSlots(MekContainerHelper<IInventorySlot> builder, IContentsListener listener, IContentsListener updateSortingListener) {
        super.addSlots(builder, listener, updateSortingListener);
        builder.addContainer(chemicalInputSlot = ChemicalInventorySlot.fillOrConvert(injectTank, this::getLevel, listener, 7, 70));
    }

    public IChemicalTank getChemicalTankBar() {
        return injectTank;
    }

    @Override
    protected void handleSecondaryFuel() {
        chemicalInputSlot.fillTankOrConvert(null);
    }

    @Override
    public boolean hasExtraResourceBar() {
        return true;
    }

    @Override
    @Contract("null, _ -> false")
    protected boolean isCachedRecipeValid(@Nullable CachedRecipe<ChemicalDissolutionRecipe> cached, @NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        if (cached != null) {
            ChemicalDissolutionRecipe cachedRecipe = cached.getRecipe();
            return cachedRecipe.getItemInput().testType(stack) && (injectTank.isEmpty() || cachedRecipe.getChemicalInput().testType(injectTank.resource()));
        }
        return false;
    }

    @Override
    protected @Nullable ChemicalDissolutionRecipe findRecipe(int process, @NotNull net.neoforged.neoforge.transfer.item.ItemResource fallbackInput, @NotNull IChemicalTank outputSlot) {
        return getRecipeType().getInputCache().findTypeBasedRecipe(level, fallbackInput, injectTank.resource(), outputSlot.resource(), OUTPUT_CHECK);
    }

    @Override
    protected int getNeededInput(ChemicalDissolutionRecipe recipe, net.neoforged.neoforge.transfer.item.ItemResource inputStack) {
        return MathUtils.clampToInt(recipe.getItemInput().getNeededAmount(inputStack));
    }

    @Override
    public boolean isItemValidForSlot(@NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        return containsRecipeBA(stack, injectTank.resource());
    }

    @Override
    public boolean isValidInputItem(@NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        return containsRecipeA(stack);
    }

    @Override
    public @NotNull IMekanismRecipeTypeProvider<?, ChemicalDissolutionRecipe, InputRecipeCache.ItemChemical<ChemicalDissolutionRecipe>> getRecipeType() {
        return MekanismRecipeType.DISSOLUTION;
    }

    @Override
    public @Nullable IRecipeViewerRecipeType<ChemicalDissolutionRecipe> recipeViewerType() {
        return RecipeViewerRecipeType.DISSOLUTION;
    }

    @Override
    public @Nullable ChemicalDissolutionRecipe getRecipe(int cacheIndex) {
        return findFirstRecipe(itemInputHandlers[cacheIndex], chemicalInputHandler);
    }

    @Override
    public @NotNull CachedRecipe<ChemicalDissolutionRecipe> createNewCachedRecipe(@NotNull ChemicalDissolutionRecipe recipe, int cacheIndex) {
        CachedRecipe<ChemicalDissolutionRecipe> cachedRecipe;
        if (recipe.perTickUsage()) {
            cachedRecipe = StackableItemStackConstantChemicalToObjectCachedRecipe.dissolution(recipe, recheckAllRecipeErrors[cacheIndex], itemInputHandlers[cacheIndex], chemicalInputHandler,
                    injectUsageMultiplier, used -> usedSoFar[cacheIndex] = Math.toIntExact(used), chemicalOutputHandlers[cacheIndex]);
        } else {
            cachedRecipe = new TwoInputCachedRecipe<>(recipe, recheckAllRecipeErrors[cacheIndex], itemInputHandlers[cacheIndex], chemicalInputHandler, chemicalOutputHandlers[cacheIndex]);
        }
        return cachedRecipe
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
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.CHEMICAL || upgrade == Upgrade.SPEED) {
            injectUsage = MekanismUtils.getGasPerTickMeanMultiplier(this);
        }
    }

    @Override
    public int getSavedUsedSoFar(int cacheIndex) {
        return usedSoFar[cacheIndex];
    }

    @Override
// port-26.1.2: persistence and transaction signatures from ported factory.
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        input.read(SerializationConstants.USED_SO_FAR, Codec.INT_STREAM).ifPresentOrElse(savedUsedStream -> {
            int[] savedUsed = savedUsedStream.toArray();
            if (tier.processes != savedUsed.length) {
                Arrays.fill(usedSoFar, 0);
            }
            for (int i = 0; i < tier.processes && i < savedUsed.length; i++) {
                usedSoFar[i] = savedUsed[i];
            }
        }, () -> Arrays.fill(usedSoFar, 0));
    }

    @Override
// port-26.1.2: persistence and transaction signatures from ported factory.
    public void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        output.store(SerializationConstants.USED_SO_FAR, Codec.INT_STREAM, Arrays.stream(usedSoFar));
    }

    @Override
// port-26.1.2: persistence and transaction signatures from ported factory.
    public void parseUpgradeData(@NotNull IUpgradeData upgradeData, HolderLookup.Provider provider, TransactionContext transaction) {
        if (upgradeData instanceof ItemChemicalToChemicalUpgradeData data) {
            super.parseUpgradeData(upgradeData, provider, transaction);
            injectTank.copyContents(data.inputTank, transaction);
            chemicalInputSlot.copyContents(data.chemicalSlot, transaction);
            System.arraycopy(data.usedSoFar, 0, usedSoFar, 0, data.usedSoFar.length);
        } else {
            Mekanism.logger.warn("Unhandled upgrade data.", new Throwable());
        }
    }

    @Override
    public @Nullable IUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        return new ItemChemicalToChemicalUpgradeData(provider, redstone, getControlType(), getEnergyContainer(),
                progress, usedSoFar, energySlot, chemicalInputSlot, inputItemSlots, injectTank, outputChemicalTanks, isSorting(), getComponents(), problemPath());
    }

    @Override
    public void dump() {
        injectTank.setContents(injectTank.resource(), 0, null); // port-26.1.2: clear resource container
    }
}