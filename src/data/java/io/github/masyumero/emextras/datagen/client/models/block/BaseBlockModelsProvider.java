package io.github.masyumero.emextras.datagen.client.models.block;

import com.jerry.mekaf.common.block.attribute.AttributeAdvancedFactoryType;
import com.jerry.mekaf.common.content.blocktype.AdvancedFactoryType;
import com.jerry.mekextras.MekanismExtras;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import com.jerry.mekmm.Mekmm;
import com.jerry.mekmm.common.block.attribute.MoreMachineAttributeFactoryType;
import com.jerry.mekmm.common.content.blocktype.MoreMachineFactoryType;
import fr.iglee42.evolvedmekanism.EvolvedMekanism;
import fr.iglee42.evolvedmekanism.registries.EMFactoryType;
import io.github.masyumero.emextras.EMExtras;
import io.github.masyumero.emextras.common.block.attribute.EMExtraAttribute;
import io.github.masyumero.emextras.common.tier.EMExtraFactoryTier;
import mekanism.common.Mekanism;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.registration.impl.BlockRegistryObject;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import io.github.masyumero.emextras.datagen.client.models.BaseAssetProvider;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;

public abstract class BaseBlockModelsProvider extends BaseAssetProvider {
    protected BaseBlockModelsProvider(PackOutput output, String modid) {
        super(output, modid);
    }

    protected abstract void registerStatesAndModels();

    @Override
    protected final void registerAssets() {
        registerStatesAndModels();
    }

    public void simpleFactoryMachineBlock(BlockRegistryObject<?, ?> block) {
        FactoryType type = Attribute.getOrThrow(block, AttributeFactoryType.class).getFactoryType();
        String tier = EMExtraAttribute.getEMExtraTier(block, EMExtraFactoryTier.class).getEMExtraTier().getLowerName();
        factory(block, type.getRegistryNameComponent(), tier,
                type == EMFactoryType.ALLOYING ? EvolvedMekanism.MODID : Mekanism.MODID, EMExtras.MODID);
    }

    public void simpleExtraFactoryMachineBlock(BlockRegistryObject<?, ?> block) {
        FactoryType type = Attribute.getOrThrow(block, AttributeFactoryType.class).getFactoryType();
        String tier = ExtraAttribute.getAdvancedTier(block, ExtraFactoryTier.class).getAdvanceTier().getLowerName();
        factory(block, type.getRegistryNameComponent(), tier, EvolvedMekanism.MODID, MekanismExtras.MOD_ID);
    }

    public void simpleAdvancedFactoryMachineBlock(BlockRegistryObject<?, ?> block) {
        AdvancedFactoryType type = Attribute.getOrThrow(block, AttributeAdvancedFactoryType.class).getAdvancedFactoryType();
        String tier = EMExtraAttribute.getEMExtraTier(block, EMExtraFactoryTier.class).getEMExtraTier().getLowerName();
        factory(block, type.getRegistryNameComponent(), tier, Mekmm.MOD_ID, EMExtras.MODID);
    }

    public void simpleMoreMachineFactoryMachineBlock(BlockRegistryObject<?, ?> block) {
        MoreMachineFactoryType type = Attribute.getOrThrow(block, MoreMachineAttributeFactoryType.class).getMoreMachineFactoryType();
        String tier = EMExtraAttribute.getEMExtraTier(block, EMExtraFactoryTier.class).getEMExtraTier().getLowerName();
        factory(block, type.getRegistryNameComponent(), tier, Mekmm.MOD_ID, EMExtras.MODID);
    }

    private void factory(BlockRegistryObject<?, ?> block, String type, String tier, String baseNamespace, String ledNamespace) {
        JsonObject variants = new JsonObject();
        for (boolean active : new boolean[]{false, true}) {
            String path = "block/factory/" + type + (active ? "/active/" : "/") + tier;
            Identifier model = Identifier.fromNamespaceAndPath(modid, path);
            // port-26.1.2: composite mesh loader remains supported; item definitions share the mesh.
            JsonObject children = new JsonObject();
            children.add("base", reference("parent", Identifier.fromNamespaceAndPath(baseNamespace, "block/factory/" + type + "/base")));
            children.add("front_led", reference("parent", Identifier.fromNamespaceAndPath(ledNamespace, "block/factory/front_led/" + (active ? "active/" : "") + tier)));
            JsonObject mesh = reference("parent", Identifier.withDefaultNamespace("block/block"));
            mesh.addProperty("loader", "neoforge:composite");
            mesh.add("children", children);
            mesh.add("textures", reference("particle", Mekanism.rl("block/factory/factory_front_back")));
            asset("models", model, mesh);
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                JsonObject variant = reference("model", model);
                int rotation = switch (facing) {
                    case EAST -> 90;
                    case SOUTH -> 180;
                    case WEST -> 270;
                    default -> 0;
                };
                if (rotation != 0) variant.addProperty("y", rotation);
                variants.add("active=" + active + ",facing=" + facing.getSerializedName(), variant);
            }
        }
        JsonObject state = new JsonObject();
        state.add("variants", variants);
        asset("blockstates", block.getId(), state);
    }
}
