package io.github.masyumero.emextras.common.tile.factory;

import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

// port-26.1.2: E-Mek's alloyer still emits ItemStack; forward it to template output handlers.
public final class LegacyAlloyerOutput implements IOutputHandler<ItemStack> {
    private final IOutputHandler<ItemStackTemplate> delegate;
    public LegacyAlloyerOutput(IOutputHandler<ItemStackTemplate> delegate) { this.delegate = delegate; }
    @Override
    public boolean handleOutput(ItemStack output, int operations, TransactionContext transaction) {
        return delegate.handleOutput(ItemStackTemplate.fromNonEmptyStack(output), operations, transaction);
    }
    @Override
    public void calculateOperationsCanSupport(OperationTracker tracker, ItemStack output) {
        delegate.calculateOperationsCanSupport(tracker, ItemStackTemplate.fromNonEmptyStack(output));
    }
}
