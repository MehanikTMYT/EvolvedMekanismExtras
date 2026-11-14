package io.github.masyumero.emextras.common.integration.mekmm.inventory.slot;

// port-26.1.2: migrate removed annotations and Mekanism/MC resource APIs.
import io.github.masyumero.emextras.common.integration.mekmm.tile.factory.TileEntityEMExtraMoreMachineFactory;

import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class EMExtraMoreMachineFactoryOutputInventorySlot extends BasicInventorySlot {

    private final TileEntityEMExtraMoreMachineFactory<?> factory;

    public static EMExtraMoreMachineFactoryOutputInventorySlot at(TileEntityEMExtraMoreMachineFactory<?> factory, @Nullable IContentsListener listener, int x, int y) {
        return new EMExtraMoreMachineFactoryOutputInventorySlot(factory, listener, x, y);
    }

    private EMExtraMoreMachineFactoryOutputInventorySlot(TileEntityEMExtraMoreMachineFactory<?> factory, @Nullable IContentsListener listener, int x, int y) {
        super(ConstantPredicates.alwaysTrueBi(), ConstantPredicates.internalOnly(), ConstantPredicates.alwaysTrue(), null, null, listener, x, y);
        setSlotType(ContainerSlotType.OUTPUT);
        this.factory = factory;
    }

    @Override
    public long capacityAsLong(net.neoforged.neoforge.transfer.item.ItemResource stack) {
        return switch (factory.tier) {
            case ABSOLUTE_OVERCLOCKED -> super.capacityAsLong(stack) * 8;
            case SUPREME_QUANTUM -> super.capacityAsLong(stack) * 16;
            case COSMIC_DENSE -> super.capacityAsLong(stack) * 32;
            case INFINITE_MULTIVERSAL -> super.capacityAsLong(stack) * 64;
        };
    }
}