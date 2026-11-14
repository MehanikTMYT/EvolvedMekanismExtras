package io.github.masyumero.emextras.common.integration.mekaf.inventory.slot;

// port-26.1.2: adapt MC 26 and Mekanism 10.8 signatures while preserving behavior.
import io.github.masyumero.emextras.common.integration.mekaf.tile.factory.TileEntityEMExtraLiquifyingFactory;
import io.github.masyumero.emextras.common.integration.mekaf.tile.factory.TileEntityEMExtraPRCFactory;
import io.github.masyumero.emextras.common.integration.mekaf.tile.factory.base.TileEntityEMExtraAdvancedFactoryBase;
import io.github.masyumero.emextras.common.integration.mekaf.tile.factory.base.TileEntityEMExtraItemToChemicalFactory;
import io.github.masyumero.emextras.common.integration.mekaf.tile.factory.base.TileEntityEMExtraItemToItemAdvancedFactory;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.fluid.IFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class EMExtraAdvancedFactoryInputInventorySlot extends InputInventorySlot {

    private final TileEntityEMExtraAdvancedFactoryBase<?> factory;

    public static EMExtraAdvancedFactoryInputInventorySlot create(TileEntityEMExtraItemToItemAdvancedFactory<?> factory, int process, IInventorySlot outputSlot, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(factory, "Factory cannot be null");
        Objects.requireNonNull(outputSlot, "Item output tank cannot be null");
        return new EMExtraAdvancedFactoryInputInventorySlot(factory, process, outputSlot, listener, x, y);
    }

    private EMExtraAdvancedFactoryInputInventorySlot(TileEntityEMExtraItemToItemAdvancedFactory<?> factory, int process, IInventorySlot outputSlot, @Nullable IContentsListener listener, int x, int y) {
        super(net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE, (stack, automationType) -> factory.isItemValidForSlot(stack) && factory.inputProducesOutput(process, stack, outputSlot, false),
                factory::isValidInputItem, null, null, listener, x, y);
        this.factory = factory;
    }

    public static EMExtraAdvancedFactoryInputInventorySlot create(TileEntityEMExtraItemToChemicalFactory<?> factory, int process, IChemicalTank outputTank, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(factory, "Factory cannot be null");
        Objects.requireNonNull(outputTank, "Chemical output tank cannot be null");
        return new EMExtraAdvancedFactoryInputInventorySlot(factory, process, outputTank, listener, x, y);
    }

    private EMExtraAdvancedFactoryInputInventorySlot(TileEntityEMExtraItemToChemicalFactory<?> factory, int process, IChemicalTank outputTank, @Nullable IContentsListener listener, int x, int y) {
        super(net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE, (stack, automationType) -> factory.isItemValidForSlot(stack) && factory.inputProducesOutput(process, stack, outputTank, false),
                factory::isValidInputItem, null, null, listener, x, y);
        this.factory = factory;
    }

    public static EMExtraAdvancedFactoryInputInventorySlot create(TileEntityEMExtraPRCFactory factory, int process, IInventorySlot outputSlot, IChemicalTank outputTank, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(factory, "Factory cannot be null");
        Objects.requireNonNull(outputTank, "Chemical output tank cannot be null");
        return new EMExtraAdvancedFactoryInputInventorySlot(factory, process, outputSlot, outputTank, listener, x, y);
    }

    private EMExtraAdvancedFactoryInputInventorySlot(TileEntityEMExtraPRCFactory factory, int process, IInventorySlot outputSlot, IChemicalTank outputTank, @Nullable IContentsListener listener, int x, int y) {
        super(net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE, (stack, automationType) -> factory.isItemValidForSlot(stack) && factory.inputProducesOutput(process, stack, outputSlot, outputTank, false),
                factory::isValidInputItem, null, null, listener, x, y);
        this.factory = factory;
    }

    public static EMExtraAdvancedFactoryInputInventorySlot create(TileEntityEMExtraLiquifyingFactory factory, int process, IInventorySlot outputSlot, IFluidTank outputTank, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(factory, "Factory cannot be null");
        Objects.requireNonNull(outputTank, "Fluid output tank cannot be null");
        return new EMExtraAdvancedFactoryInputInventorySlot(factory, process, outputSlot, outputTank, listener, x, y);
    }

    private EMExtraAdvancedFactoryInputInventorySlot(TileEntityEMExtraLiquifyingFactory factory, int process, IInventorySlot outputSlot, IFluidTank outputTank, @Nullable IContentsListener listener, int x, int y) {
        super(net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE, (stack, automationType) -> factory.isItemValidForSlot(stack) && factory.inputProducesOutput(process, stack, outputSlot, outputTank, false),
                factory::isValidInputItem, null, null, listener, x, y);
        this.factory = factory;
    }

    @Override
    public long capacityAsLong(@NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        try {
            return Math.multiplyExact(super.capacityAsLong(stack), 8 << factory.tier.ordinal());
        } catch (ArithmeticException ignored) {
            return Integer.MAX_VALUE;
        }
    }
}