package io.github.masyumero.emextras.common.integration.mekmm.tile.factory;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.world.item.ItemStackTemplate;
import mekanism.api.chemical.ChemicalResource;
import mekanism.api.chemical.Chemical;
// port-26.1.2: typed container holders and server-level update context.
import mekanism.api.fluid.IFluidTank;
import mekanism.common.capabilities.holder.container.IContainerHolder;
// port-26.1.2: migrate removed annotations and Mekanism/MC resource APIs.
import com.jerry.mekmm.api.recipes.basic.MMBasicItemStackChemicalToItemStackRecipe;
import com.jerry.mekmm.api.recipes.cache.ReplicatorCachedRecipe;
import com.jerry.mekmm.client.recipe_viewer.MMRecipeViewerRecipeType;
import com.jerry.mekmm.common.config.MoreMachineConfig;
import com.jerry.mekmm.common.recipe.impl.ReplicatorIRecipeSingle;
import com.jerry.mekmm.common.registries.MoreMachineChemicals;
import com.jerry.mekmm.common.util.ValidatorUtils;
import lombok.Getter;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.math.MathUtils;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.slot.ChemicalInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.lookup.IDoubleRecipeLookupHandler.ItemChemicalRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.DoubleInputRecipeCache;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.ItemChemical;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.interfaces.IHasDumpButton;
import mekanism.common.upgrade.AdvancedMachineUpgradeData;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.RegistryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TileEntityEMExtraReplicatingFactory extends TileEntityEMExtraItemToItemMoreMachineFactory<MMBasicItemStackChemicalToItemStackRecipe> implements IHasDumpButton,
        ItemChemicalRecipeLookupHandler<MMBasicItemStackChemicalToItemStackRecipe> {

    // port-26.1.2: compare resource identities against recipe output templates.
    protected static final DoubleInputRecipeCache.CheckRecipeType<Item, ItemResource, Chemical, ChemicalResource, MMBasicItemStackChemicalToItemStackRecipe, ItemResource> OUTPUT_CHECK = (recipe, input, extra, output) -> output.isEmpty() || output.matches(recipe.getOutput(input, extra));
    private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
            RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_INPUT,
            RecipeError.NOT_ENOUGH_SECONDARY_INPUT,
            RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
            RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT);
    private static final Set<RecipeError> GLOBAL_ERROR_TYPES = Set.of(
            RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_SECONDARY_INPUT);

    public static final long MAX_GAS = 10 * FluidType.BUCKET_VOLUME;

    public static HashMap<String, Integer> customRecipeMap = ValidatorUtils.getRecipeFromConfig(MoreMachineConfig.general.itemReplicatorRecipe.get());

    private final IInputHandler<mekanism.api.chemical.Chemical, ChemicalStack> chemicalInputHandler;
    // 化学品存储槽
    @Getter
    public IChemicalTank chemicalTank;
    // 气罐槽
    ChemicalInventorySlot chemicalSlot;

    public TileEntityEMExtraReplicatingFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state, TRACKED_ERROR_TYPES, GLOBAL_ERROR_TYPES);
        configComponent.setupInputConfig(TransmissionType.CHEMICAL, chemicalTank);

        // port-26.1.2: superclass initializes ejector.
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);

        chemicalInputHandler = InputHelper.getConstantInputHandler(chemicalTank);
    }

    @Nullable
    @Override
    public ChemicalInventorySlot getExtraSlot() {
        return chemicalSlot;
    }

    @Override
    public @Nullable IContainerHolder<IChemicalTank> getInitialChemicalTanks(IContentsListener listener) {
        MekContainerHelper<IChemicalTank> builder = MekContainerHelper.forSideWithChemicalConfig(this);
        chemicalTank = BasicChemicalTank.input(MAX_GAS * tier.processes * tier.processes, TileEntityEMExtraReplicatingFactory::isValidChemicalInput, markAllMonitorsChanged(listener));
        builder.addContainer(chemicalTank);
        return builder.build();
    }

    @Override
    protected void addSlots(MekContainerHelper<IInventorySlot> builder, IContentsListener listener, IContentsListener updateSortingListener) {
        super.addSlots(builder, listener, updateSortingListener);
        builder.addContainer(chemicalSlot = ChemicalInventorySlot.fillOrConvert(chemicalTank, this::getLevel, listener, 7, 57));
    }

    @Override
    protected boolean isCachedRecipeValid(@Nullable CachedRecipe<MMBasicItemStackChemicalToItemStackRecipe> cached, @NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        return cached != null && isValidChemicalInput(chemicalTank.resource());
    }

    @Override
    protected @Nullable MMBasicItemStackChemicalToItemStackRecipe findRecipe(int process, @NotNull net.neoforged.neoforge.transfer.item.ItemResource fallbackInput, @NotNull IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot) {
        return null;
    }

    @Override
    protected void handleSecondaryFuel() {
        chemicalSlot.fillTankOrConvert(null);
    }

    @Override
    protected int getNeededInput(MMBasicItemStackChemicalToItemStackRecipe recipe, net.neoforged.neoforge.transfer.item.ItemResource inputStack) {
        return MathUtils.clampToInt(recipe.getItemInput().getNeededAmount(inputStack));
    }

    @Override
    public boolean isItemValidForSlot(@NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        return true;
    }

    public static boolean isValidChemicalInput(mekanism.api.chemical.ChemicalResource stack) {
        return stack.is(MoreMachineChemicals.UU_MATTER);
    }

    @Override
    public boolean isValidInputItem(net.neoforged.neoforge.transfer.item.ItemResource stack) {
        Item item = stack.getItem();
        if (customRecipeMap != null) {
            return customRecipeMap.containsKey(Objects.requireNonNull(RegistryUtils.getName(item.builtInRegistryHolder())).toString());
        }
        return false;
    }

    @Override
    public @NotNull IMekanismRecipeTypeProvider<?, MMBasicItemStackChemicalToItemStackRecipe, ItemChemical<MMBasicItemStackChemicalToItemStackRecipe>> getRecipeType() {
        return null;
    }

    @Override
    public @Nullable MMBasicItemStackChemicalToItemStackRecipe getRecipe(int cacheIndex) {
        return getRecipe(inputHandlers[cacheIndex].getInput(), chemicalInputHandler.getInput());
    }

    @Override
    public @NotNull CachedRecipe<MMBasicItemStackChemicalToItemStackRecipe> createNewCachedRecipe(@NotNull MMBasicItemStackChemicalToItemStackRecipe recipe, int cacheIndex) {
        return ReplicatorCachedRecipe.createItemReplicator(recipe, recheckAllRecipeErrors[cacheIndex], inputHandlers[cacheIndex], chemicalInputHandler, outputHandlers[cacheIndex])
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
    public @Nullable IRecipeViewerRecipeType<MMBasicItemStackChemicalToItemStackRecipe> recipeViewerType() {
        return MMRecipeViewerRecipeType.REPLICATOR;
    }

    public static MMBasicItemStackChemicalToItemStackRecipe getRecipe(ItemStack itemStack, ChemicalStack chemicalStack) {
        if (chemicalStack.isEmpty() || itemStack.isEmpty()) {
            return null;
        }
        if (customRecipeMap != null) {
            Item item = itemStack.getItem();
            // 如果为空则赋值为0
            int amount = customRecipeMap.getOrDefault(RegistryUtils.getName(itemStack.typeHolder()).toString(), 0);
            // 防止null和配置文件中出现0
            if (amount == 0) return null;
            return new ReplicatorIRecipeSingle(
                    IngredientCreatorAccess.item().from(item, 1),
                    IngredientCreatorAccess.chemicalStack().fromHolder(MoreMachineChemicals.UU_MATTER, amount),
                    new net.minecraft.world.item.ItemStackTemplate(item, 1));
        }
        return null;
    }

    @Override
    public boolean hasSecondaryResourceBar() {
        return true;
    }

    @Override
// port-26.1.2: persistence and transaction signatures from ported factory.
    public void parseUpgradeData(@NotNull IUpgradeData upgradeData, HolderLookup.Provider provider, TransactionContext transaction) {
        if (upgradeData instanceof AdvancedMachineUpgradeData data) {
            // Generic factory upgrade data handling
            super.parseUpgradeData(upgradeData, provider, transaction);
            // Copy the contents using NBT so that if it is not actually valid due to a reload we don't crash
            chemicalTank.copyContents(data.stored, transaction);
            chemicalSlot.copyContents(data.chemicalSlot, transaction);
        } else {
            Mekanism.logger.warn("Unhandled upgrade data.", new Throwable());
        }
    }

    @Override
    public @Nullable AdvancedMachineUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        return new AdvancedMachineUpgradeData(provider, redstone, getControlType(), getEnergyContainer(), progress, null, chemicalTank, chemicalSlot, energySlot,
                inputSlots, outputSlots, isSorting(), getComponents(), problemPath());
    }

    @Override
    public void dump() {
        chemicalTank.setContents(chemicalTank.resource(), 0, null); // port-26.1.2: clear resource container
    }
}