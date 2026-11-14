package io.github.masyumero.emextras.common.integration.mekaf.tile.factory;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.minecraft.world.level.material.Fluid;
import mekanism.api.chemical.ChemicalResource;
import mekanism.api.chemical.Chemical;
// port-26.1.2: typed container holders and server-level update context.
import mekanism.api.fluid.IFluidTank;
import mekanism.common.capabilities.holder.container.IContainerHolder;
// port-26.1.2: migrate removed annotations and Mekanism/MC resource APIs.
import com.jerry.mekaf.common.upgrade.FluidChemicalToChemicalUpgradeData;
import io.github.masyumero.emextras.common.integration.mekaf.tile.factory.base.TileEntityEMExtraChemicalToChemicalFactory;
import mekanism.api.IContentsListener;
import mekanism.api.Upgrade;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.math.MathUtils;
import mekanism.api.recipes.FluidChemicalToChemicalRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.cache.TwoInputCachedRecipe;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.FluidInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.IDoubleRecipeLookupHandler.FluidChemicalRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.DoubleInputRecipeCache;
import mekanism.common.recipe.lookup.cache.InputRecipeCache;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.ChemicalSlotInfo;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.interfaces.IHasDumpButton;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class TileEntityEMExtraWashingFactory extends TileEntityEMExtraChemicalToChemicalFactory<FluidChemicalToChemicalRecipe> implements IHasDumpButton, FluidChemicalRecipeLookupHandler<FluidChemicalToChemicalRecipe> {

    // port-26.1.2: compare resource identities against recipe output templates.
    protected static final DoubleInputRecipeCache.CheckRecipeType<Fluid, FluidResource, Chemical, ChemicalResource, FluidChemicalToChemicalRecipe, ChemicalResource> OUTPUT_CHECK = (recipe, fluidInput, chemicalInput, output) -> output.isEmpty() || output.is(recipe.getOutput(fluidInput, chemicalInput).typeHolder());
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

    public BasicFluidTank fluidTank;

    private final IInputHandler<net.minecraft.world.level.material.Fluid, @NotNull FluidStack> fluidInputHandler;

    FluidInventorySlot fluidSlot;
    OutputInventorySlot fluidOutputSlot;

    public TileEntityEMExtraWashingFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state, TRACKED_ERROR_TYPES, GLOBAL_ERROR_TYPES);
        ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        if (itemConfig != null) {
            itemConfig.addSlotInfo(DataType.INPUT, new InventorySlotInfo(true, false, fluidSlot));
            itemConfig.addSlotInfo(DataType.OUTPUT, new InventorySlotInfo(false, true, fluidOutputSlot));
            itemConfig.addSlotInfo(DataType.INPUT_OUTPUT, new InventorySlotInfo(true, true, fluidSlot, fluidOutputSlot));
        }
        ConfigInfo config = configComponent.getConfig(TransmissionType.CHEMICAL);
        if (config != null) {
            config.addSlotInfo(DataType.INPUT, new ChemicalSlotInfo(true, false, inputChemicalTanks));
            List<IChemicalTank> ioTank = outputChemicalTanks;
            ioTank.addAll(inputChemicalTanks);
            config.addSlotInfo(DataType.INPUT_OUTPUT, new ChemicalSlotInfo(true, true, ioTank));
        }

        configComponent.setupInputConfig(TransmissionType.FLUID, fluidTank);

        // port-26.1.2: superclass initializes ejector.
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM, TransmissionType.CHEMICAL)
                .setCanTankEject(tank -> !inputChemicalTanks.contains(tank));

        fluidInputHandler = InputHelper.getInputHandler(fluidTank, RecipeError.NOT_ENOUGH_SECONDARY_INPUT);
    }

    @NotNull
    @Override
    protected IContainerHolder<IFluidTank> getInitialFluidTanks(IContentsListener listener) {
        MekContainerHelper<IFluidTank> builder = MekContainerHelper.forSideWithFluidConfig(this);
        builder.addContainer(fluidTank = BasicFluidTank.input(MAX_FLUID * tier.processes, this::containsRecipeA, markAllMonitorsChanged(listener)));
        return builder.build();
    }

    @Override
    protected void addSlots(MekContainerHelper<IInventorySlot> builder, IContentsListener listener, IContentsListener updateSortingListener) {
        builder.addContainer(fluidSlot = FluidInventorySlot.fill(fluidTank, listener, 180 + tier.imageWidth, 71));
        builder.addContainer(fluidOutputSlot = OutputInventorySlot.at(listener, 180 + tier.imageWidth, 102));
        fluidSlot.setSlotOverlay(SlotOverlay.MINUS);
    }

    public BasicFluidTank getFluidTankBar() {
        return fluidTank;
    }

    @Override
    public boolean hasExtraResourceBar() {
        return true;
    }

    @Override
    protected void handleSecondaryFuel() {
        fluidSlot.fillTankFromSlot(fluidOutputSlot, null);
    }

    @Override
    protected boolean isCachedRecipeValid(@Nullable CachedRecipe<FluidChemicalToChemicalRecipe> cached, @NotNull mekanism.api.chemical.ChemicalResource stack) {
        if (cached != null) {
            FluidChemicalToChemicalRecipe cachedRecipe = cached.getRecipe();
            return cachedRecipe.getChemicalInput().testType(stack) && (fluidTank.isEmpty() || cachedRecipe.getFluidInput().testType(fluidTank.resource()));
        }
        return false;
    }

    @Override
    protected @Nullable FluidChemicalToChemicalRecipe findRecipe(int process, @NotNull mekanism.api.chemical.ChemicalResource fallbackInput, @NotNull IChemicalTank outputSlot) {
        return getRecipeType().getInputCache().findTypeBasedRecipe(level, fluidTank.resource(), fallbackInput, outputSlot.resource(), OUTPUT_CHECK);
    }

    @Override
    public boolean isChemicalValidForTank(@NotNull mekanism.api.chemical.ChemicalResource stack) {
        return containsRecipeAB(fluidTank.resource(), stack);
    }

    @Override
    public boolean isValidInputChemical(@NotNull mekanism.api.chemical.ChemicalResource stack) {
        return containsRecipeB(stack);
    }

    @Override
    protected int getNeededInput(FluidChemicalToChemicalRecipe recipe, mekanism.api.chemical.ChemicalResource inputStack) {
        return MathUtils.clampToInt(recipe.getChemicalInput().getNeededAmount(inputStack));
    }

    @Override
    public @NotNull IMekanismRecipeTypeProvider<?, FluidChemicalToChemicalRecipe, InputRecipeCache.FluidChemical<FluidChemicalToChemicalRecipe>> getRecipeType() {
        return MekanismRecipeType.WASHING;
    }

    @Override
    public @Nullable IRecipeViewerRecipeType<FluidChemicalToChemicalRecipe> recipeViewerType() {
        return RecipeViewerRecipeType.WASHING;
    }

    @Override
    public @Nullable FluidChemicalToChemicalRecipe getRecipe(int cacheIndex) {
        return findFirstRecipe(fluidInputHandler, chemicalInputHandlers[cacheIndex]);
    }

    @Override
    public @NotNull CachedRecipe<FluidChemicalToChemicalRecipe> createNewCachedRecipe(@NotNull FluidChemicalToChemicalRecipe recipe, int cacheIndex) {
        return new TwoInputCachedRecipe<>(recipe, recheckAllRecipeErrors[cacheIndex], fluidInputHandler, chemicalInputHandlers[cacheIndex], chemicalOutputHandlers[cacheIndex])
                .setErrorsChanged(errors -> errorTracker.onErrorsChanged(errors, cacheIndex))
                .setCanHolderFunction(this::canFunction)
                .setActive(active -> setActiveState(active, cacheIndex))
                .setEnergyRequirements(energyContainer::getEnergyPerTick, energyContainer)
                .setBaselineMaxOperations(() -> baselineMaxOperations)
                .setOnFinish(this::markForSave);
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.SPEED) {
            baselineMaxOperations = (int) Math.pow(2, getComponent().getUpgrades(Upgrade.SPEED));
        }
    }

    // 更改加速升级的显示的，默认是10x，气体工厂是256x，当然只有速度升级需要更改
    @NotNull
    @Override
    public List<Component> getInfo(@NotNull Upgrade upgrade) {
        return upgrade == Upgrade.SPEED ? UpgradeUtils.getExpScaledInfo(this, upgrade) : super.getInfo(upgrade);
    }

    @Override
// port-26.1.2: persistence and transaction signatures from ported factory.
    public void parseUpgradeData(@NotNull IUpgradeData upgradeData, HolderLookup.Provider provider, TransactionContext transaction) {
        if (upgradeData instanceof FluidChemicalToChemicalUpgradeData data) {
            super.parseUpgradeData(upgradeData, provider, transaction);
            fluidTank.copyContents(data.inputTank, transaction);
            fluidSlot.copyContents(data.fluidInputSlot, transaction);
            fluidOutputSlot.copyContents(data.fluidOutputSlot, transaction);
        } else {
            Mekanism.logger.warn("Unhandled upgrade data.", new Throwable());
        }
    }

    @Override
    public @Nullable IUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        return new FluidChemicalToChemicalUpgradeData(provider, redstone, getControlType(), getEnergyContainer(), progress, null,
                energySlot, fluidSlot, fluidOutputSlot, inputChemicalTanks, fluidTank, outputChemicalTanks, isSorting(), getComponents(), problemPath());
    }

    @Override
    public void dump() {
        fluidTank.setContents(net.neoforged.neoforge.transfer.fluid.FluidResource.EMPTY, 0, null);
    }
}