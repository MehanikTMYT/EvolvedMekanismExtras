package io.github.masyumero.emextras.common.inventory.slot.chemical;

// port-26.1.2: migrate removed annotations and Mekanism/MC resource APIs.
import io.github.masyumero.emextras.common.tile.factory.TileEntityEMExtraFactory;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.inventory.slot.ChemicalInventorySlot;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class EMExtraFactoryChemicalInventorySlot extends ChemicalInventorySlot {

    private final TileEntityEMExtraFactory<?> factory;

    protected EMExtraFactoryChemicalInventorySlot(TileEntityEMExtraFactory<?> factory, IChemicalTank chemicalTank, Supplier<Level> worldSupplier, Predicate<net.neoforged.neoforge.transfer.item.ItemResource> canExtract, Predicate<net.neoforged.neoforge.transfer.item.ItemResource> canInsert, @Nullable IContentsListener listener, int x, int y) {
        super(chemicalTank, worldSupplier, (resource, automation) -> !automation.isExternal() || canExtract.test(resource),
                (resource, automation) -> automation.isInternal() || canInsert.test(resource), null, null, listener, x, y); // port-26.1.2: transactional slot predicates
        this.factory = factory;
    }

    public static EMExtraFactoryChemicalInventorySlot fillOrConverts(TileEntityEMExtraFactory<?> factory, IChemicalTank gasTank, Supplier<Level> worldSupplier, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(gasTank, "Gas tank cannot be null");
        Objects.requireNonNull(worldSupplier, "World supplier cannot be null");
        return new EMExtraFactoryChemicalInventorySlot(factory, gasTank, worldSupplier, getFillOrConvertExtractPredicate(gasTank, worldSupplier), getFillOrConvertInsertPredicate(gasTank, worldSupplier), listener, x, y);
    }

    private static Predicate<net.neoforged.neoforge.transfer.item.ItemResource> getFillOrConvertExtractPredicate(IChemicalTank chemicalTank, Supplier<Level> levelSupplier) {
        // port-26.1.2: Mekanism combines handler-resource and conversion validation.
        return stack -> !canFillOrConvert(chemicalTank, levelSupplier, stack);
    }

    private static Predicate<net.neoforged.neoforge.transfer.item.ItemResource> getFillOrConvertInsertPredicate(IChemicalTank chemicalTank, Supplier<Level> levelSupplier) {
        // port-26.1.2: includes a transactional simulation of tank acceptance.
        return stack -> canFillOrConvert(chemicalTank, levelSupplier, stack);
    }

    @Override
    public long capacityAsLong(net.neoforged.neoforge.transfer.item.ItemResource stack) {
        if (!stack.isEmpty() && mekanism.common.util.ItemAccessUtils.sideEffectFreeAccess(stack).getCapability(Capabilities.CHEMICAL.item()) != null) {
            return super.capacityAsLong(stack);
        } else {
            int processes = factory.tier.processes;
            return switch (factory.tier) {
                case ABSOLUTE_OVERCLOCKED -> super.capacityAsLong(stack) * 8 * processes;
                case SUPREME_QUANTUM -> super.capacityAsLong(stack) * 16 * processes;
                case COSMIC_DENSE -> super.capacityAsLong(stack) * 32 * processes;
                case INFINITE_MULTIVERSAL -> super.capacityAsLong(stack) * 64 * processes;
            };
        }
    }
}
