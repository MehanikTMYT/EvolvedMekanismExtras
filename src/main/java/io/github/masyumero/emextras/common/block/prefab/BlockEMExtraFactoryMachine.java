package io.github.masyumero.emextras.common.block.prefab;

// port-26.1.2: adapt MC 26 and Mekanism 10.8 signatures while preserving behavior.
import io.github.masyumero.emextras.common.content.blocktype.EMExtraFactory;
import io.github.masyumero.emextras.common.content.blocktype.EMExtraMachine;
import io.github.masyumero.emextras.common.tile.factory.TileEntityEMExtraFactory;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.block.states.IStateFluidLoggable;
import mekanism.common.resource.BlockResourceInfo;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.UnaryOperator;

public class BlockEMExtraFactoryMachine<TILE extends TileEntityMekanism, MACHINE extends EMExtraMachine.EMExtraFactoryMachine<TILE>> extends BlockTile<TILE, MACHINE> {

    public BlockEMExtraFactoryMachine(MACHINE machineType, BlockBehaviour.Properties propertiesModifier) {
        super(machineType, propertiesModifier);
    }

    public static class BlockEMExtraFactoryMachineModel<TILE extends TileEntityMekanism, MACHINE extends EMExtraMachine.EMExtraFactoryMachine<TILE>> extends BlockEMExtraFactoryMachine<TILE, MACHINE> implements IStateFluidLoggable {

        public BlockEMExtraFactoryMachineModel(MACHINE machineType, BlockBehaviour.Properties propertiesModifier) {
            super(machineType, propertiesModifier);
        }
    }

    public static class BlockEMExtraFactory<TILE extends TileEntityEMExtraFactory<?>> extends BlockEMExtraFactoryMachine.BlockEMExtraFactoryMachineModel<TILE, EMExtraFactory<TILE>> {

        public BlockEMExtraFactory(EMExtraFactory<TILE> factoryType, net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
            super(factoryType, properties.mapColor(BlockResourceInfo.STEEL.getMapColor()));
        }
    }
}
