package io.github.masyumero.emextras.common.inventory.slot;

// port-26.1.2: adapt MC 26 and Mekanism 10.8 signatures while preserving behavior.
import io.github.masyumero.emextras.common.tile.factory.TileEntityEMExtraFactory;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class EMExtraFactoryInputInventorySlot extends InputInventorySlot {

    private final TileEntityEMExtraFactory<?> factory;

    public static EMExtraFactoryInputInventorySlot create(TileEntityEMExtraFactory<?> factory, int process, IInventorySlot outputSlot, @Nullable IContentsListener listener,
                                                        int x, int y) {
        return create(factory, process, outputSlot, null, listener, x, y);
    }

    public static EMExtraFactoryInputInventorySlot create(TileEntityEMExtraFactory<?> factory, int process, IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot,
                                                        @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(factory, "Factory cannot be null");
        Objects.requireNonNull(outputSlot, "Primary output slot cannot be null");
        return new EMExtraFactoryInputInventorySlot(factory, process, outputSlot, secondaryOutputSlot, listener, x, y);
    }

    private EMExtraFactoryInputInventorySlot(TileEntityEMExtraFactory<?> factory, int process, IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot,
                                           @Nullable IContentsListener listener, int x, int y) {
        super(net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE, (stack, automationType) -> factory.isItemValidForSlot(stack) && factory.inputProducesOutput(process, stack, outputSlot, secondaryOutputSlot, false),
                factory::isValidInputItem, null, null, listener, x, y);
        this.factory = factory;
    }

    @Override
    public long capacityAsLong(@NotNull net.neoforged.neoforge.transfer.item.ItemResource stack) {
        if (factory != null) {
            return switch (factory.tier) {
                case ABSOLUTE_OVERCLOCKED -> super.capacityAsLong(stack) * 8;
                case SUPREME_QUANTUM -> super.capacityAsLong(stack) * 16;
                case COSMIC_DENSE -> super.capacityAsLong(stack) * 32;
                case INFINITE_MULTIVERSAL -> super.capacityAsLong(stack) * 64;
            };
        }
        return super.capacityAsLong(stack);
    }
}
