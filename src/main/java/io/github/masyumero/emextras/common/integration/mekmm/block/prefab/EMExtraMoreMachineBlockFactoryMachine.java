package io.github.masyumero.emextras.common.integration.mekmm.block.prefab;

// port-26.1.2: adapt MC 26 and Mekanism 10.8 signatures while preserving behavior.
import io.github.masyumero.emextras.common.content.blocktype.EMExtraMachine;
import io.github.masyumero.emextras.common.integration.mekmm.content.blocktype.EMExtraMoreMachineFactory;
import io.github.masyumero.emextras.common.integration.mekmm.tile.factory.TileEntityEMExtraMoreMachineFactory;

import mekanism.common.block.prefab.BlockTile;
import mekanism.common.block.states.IStateFluidLoggable;
import mekanism.common.resource.BlockResourceInfo;
import mekanism.common.tile.base.TileEntityMekanism;

import java.util.function.UnaryOperator;

public class EMExtraMoreMachineBlockFactoryMachine<TILE extends TileEntityMekanism, MACHINE extends EMExtraMachine.EMExtraFactoryMachine<TILE>> extends BlockTile<TILE, MACHINE> {

    public EMExtraMoreMachineBlockFactoryMachine(MACHINE machine, Properties propertiesModifier) {
        super(machine, propertiesModifier);
    }

    public static class EMExtraMoreMachineBlockFactoryMachineModel<TILE extends TileEntityMekanism, MACHINE extends EMExtraMachine.EMExtraFactoryMachine<TILE>> extends EMExtraMoreMachineBlockFactoryMachine<TILE, MACHINE> implements IStateFluidLoggable {

        public EMExtraMoreMachineBlockFactoryMachineModel(MACHINE machineType, Properties propertiesModifier) {
            super(machineType, propertiesModifier);
        }
    }

    public static class BlockEMExtraMoreMachineFactory<TILE extends TileEntityEMExtraMoreMachineFactory<?>> extends EMExtraMoreMachineBlockFactoryMachineModel<TILE, EMExtraMoreMachineFactory<TILE>> {

        public BlockEMExtraMoreMachineFactory(EMExtraMoreMachineFactory<TILE> factoryType, net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
            super(factoryType, properties.mapColor(BlockResourceInfo.STEEL.getMapColor()));
        }
    }
}