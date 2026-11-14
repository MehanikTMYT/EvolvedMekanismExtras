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
import io.github.masyumero.emextras.common.integration.mekaf.tile.factory.base.TileEntityEMExtraItemToItemAdvancedFactory;
import mekanism.api.IContentsListener;
import mekanism.api.SerializationConstants;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.math.MathUtils;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.cache.ItemStackConstantChemicalToObjectCachedRecipe;
import mekanism.api.recipes.cache.ItemStackConstantChemicalToObjectCachedRecipe.ChemicalUsageMultiplier;
import mekanism.api.recipes.cache.TwoInputCachedRecipe;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.api.recipes.vanilla_input.SingleItemChemicalRecipeInput;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerChemicalTankWrapper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.slot.ChemicalInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.IDoubleRecipeLookupHandler.ItemChemicalRecipeLookupHandler;
import mekanism.common.recipe.lookup.IRecipeLookupHandler.ConstantUsageRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.DoubleInputRecipeCache.CheckRecipeType;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.ItemChemical;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.interfaces.IHasDumpButton;
import mekanism.common.upgrade.AdvancedMachineUpgradeData;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.InventoryUtils;

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

public class TileEntityEMExtraPaintingFactory extends TileEntityEMExtraItemToItemAdvancedFactory<ItemStackChemicalToItemStackRecipe> implements ConstantUsageRecipeLookupHandler,
        ItemChemicalRecipeLookupHandler<ItemStackChemicalToItemStackRecipe>, IHasDumpButton {

    // port-26.1.2: compare resource identities against recipe output templates.
    private static final CheckRecipeType<Item, ItemResource, Chemical, ChemicalResource, ItemStackChemicalToItemStackRecipe, ItemResource> OUTPUT_CHECK = (recipe, input, extra, output) -> output.isEmpty() || output.matches(recipe.getOutput(input, extra));
    private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
            RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_INPUT,
            RecipeError.NOT_ENOUGH_SECONDARY_INPUT,
            RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
            RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
    private static final Set<RecipeError> GLOBAL_ERROR_TYPES = Set.of(
            RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_SECONDARY_INPUT);

    // Pigment Tank
    @WrappingComputerMethod(wrapper = ComputerChemicalTankWrapper.class,
            methodNames = { "getChemicalInput", "getChemicalInputCapacity", "getChemicalInputNeeded",
                    "getChemicalInputFilledPercentage" },
            docPlaceholder = "chemical tank")
    public IChemicalTank chemicalTank;

    private final ChemicalUsageMultiplier chemicalUsageMultiplier = ChemicalUsageMultiplier.constantUse(this::getTicksRequired, this::getTicksRequired);
    private final int[] usedSoFar;

    private final IInputHandler<mekanism.api.chemical.Chemical, @NotNull ChemicalStack> chemicalInputHandler;

    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getInputChemicalItem", docPlaceholder = "chemical slot")
    ChemicalInventorySlot chemicalInputSlot;

    public TileEntityEMExtraPaintingFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state, TRACKED_ERROR_TYPES, GLOBAL_ERROR_TYPES);

        configComponent.setupInputConfig(TransmissionType.CHEMICAL, chemicalTank);

        // port-26.1.2: superclass initializes ejector.
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM, TransmissionType.CHEMICAL);
        usedSoFar = new int[tier.processes];

        chemicalInputHandler = InputHelper.getInputHandler(chemicalTank, RecipeError.NOT_ENOUGH_SECONDARY_INPUT);
    }

    @Override
    protected void addTanks(MekContainerHelper<IChemicalTank> builder, IContentsListener listener, IContentsListener updateSortingListener) {
        builder.addContainer(chemicalTank = BasicChemicalTank.input(MAX_CHEMICAL * tier.processes, this::containsRecipeB, markAllMonitorsChanged(listener)));
    }

    @Override
    protected void addSlots(MekContainerHelper<IInventorySlot> builder, IContentsListener listener, IContentsListener updateSortingListener) {
        super.addSlots(builder, listener, updateSortingListener);
        builder.addContainer(chemicalInputSlot = ChemicalInventorySlot.fillOrConvert(chemicalTank, this::getLevel, listener, 7, 57));
    }

    @Override
    protected @Nullable IInventorySlot getExtraSlot() {
        return chemicalInputSlot;
    }

    @Override
    protected boolean isCachedRecipeValid(@Nullable CachedRecipe<ItemStackChemicalToItemStackRecipe> cached, @NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        if (cached != null) {
            ItemStackChemicalToItemStackRecipe cachedRecipe = cached.getRecipe();
            return cachedRecipe.getItemInput().testType(stack) && (chemicalTank.isEmpty() || cachedRecipe.getChemicalInput().testType(chemicalTank.resource()));
        }
        return false;
    }

    @Override
    protected @Nullable ItemStackChemicalToItemStackRecipe findRecipe(int process, @NotNull net.neoforged.neoforge.transfer.item.ItemResource fallbackInput, @NotNull IInventorySlot outputSlot) {
        return getRecipeType().getInputCache().findTypeBasedRecipe(level, fallbackInput, chemicalTank.resource(), outputSlot.resource(), OUTPUT_CHECK);
    }

    @Override
    public boolean isItemValidForSlot(@NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        return containsRecipeBA(stack, chemicalTank.resource());
    }

    @Override
    public boolean isValidInputItem(@NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        return containsRecipeA(stack);
    }

    @Override
    protected int getNeededInput(ItemStackChemicalToItemStackRecipe recipe, net.neoforged.neoforge.transfer.item.ItemResource inputStack) {
        return MathUtils.clampToInt(recipe.getItemInput().getNeededAmount(inputStack));
    }

    public IChemicalTank getChemicalTankBar() {
        return chemicalTank;
    }

    @Override
    public boolean hasExtraResourceBar() {
        return true;
    }

    @NotNull
    @Override
    public IMekanismRecipeTypeProvider<SingleItemChemicalRecipeInput, ItemStackChemicalToItemStackRecipe, ItemChemical<ItemStackChemicalToItemStackRecipe>> getRecipeType() {
        return MekanismRecipeType.PAINTING;
    }

    @Override
    public IRecipeViewerRecipeType<ItemStackChemicalToItemStackRecipe> recipeViewerType() {
        return RecipeViewerRecipeType.PAINTING;
    }

    @Nullable
    @Override
    public ItemStackChemicalToItemStackRecipe getRecipe(int cacheIndex) {
        return findFirstRecipe(itemInputHandlers[cacheIndex], chemicalInputHandler);
    }

    @Override
    public @NotNull CachedRecipe<ItemStackChemicalToItemStackRecipe> createNewCachedRecipe(@NotNull ItemStackChemicalToItemStackRecipe recipe, int cacheIndex) {
        CachedRecipe<ItemStackChemicalToItemStackRecipe> cachedRecipe;
        if (recipe.perTickUsage()) {
            cachedRecipe = new ItemStackConstantChemicalToObjectCachedRecipe<>(recipe, recheckAllRecipeErrors[cacheIndex], itemInputHandlers[cacheIndex], chemicalInputHandler,
                    chemicalUsageMultiplier, used -> usedSoFar[cacheIndex] = Math.toIntExact(used), itemOutputHandlers[cacheIndex]);
        } else {
            cachedRecipe = new TwoInputCachedRecipe<>(recipe, recheckAllRecipeErrors[cacheIndex], itemInputHandlers[cacheIndex], chemicalInputHandler, itemOutputHandlers[cacheIndex]);
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
    protected void writeUpdatedTag(net.minecraft.world.level.storage.ValueOutput output) {
        super.writeUpdatedTag(output); // port-26.1.2: update-tag customization hook
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
        if (upgradeData instanceof AdvancedMachineUpgradeData data) {
            super.parseUpgradeData(upgradeData, provider, transaction);
            chemicalTank.copyContents(data.stored, transaction);
            chemicalInputSlot.copyContents(data.chemicalSlot, transaction);
            System.arraycopy(data.usedSoFar, 0, usedSoFar, 0, data.usedSoFar.length);
        } else {
            Mekanism.logger.warn("Unhandled upgrade data.", new Throwable());
        }
    }

    @NotNull
    @Override
    public AdvancedMachineUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        return new AdvancedMachineUpgradeData(provider, redstone, getControlType(), getEnergyContainer(), progress, usedSoFar, chemicalTank, chemicalInputSlot, energySlot,
                inputItemSlots, outputItemSlots, isSorting(), getComponents(), problemPath());
    }

    @Override
    public void dump() {
        chemicalTank.setContents(chemicalTank.resource(), 0, null); // port-26.1.2: clear resource container
    }
}