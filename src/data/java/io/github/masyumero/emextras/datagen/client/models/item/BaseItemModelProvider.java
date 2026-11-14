package io.github.masyumero.emextras.datagen.client.models.item;

import com.jerry.mekaf.common.block.attribute.AttributeAdvancedFactoryType;
import com.jerry.mekaf.common.content.blocktype.AdvancedFactoryType;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import com.jerry.mekmm.common.block.attribute.MoreMachineAttributeFactoryType;
import com.jerry.mekmm.common.content.blocktype.MoreMachineFactoryType;

import io.github.masyumero.emextras.EMExtras;
import io.github.masyumero.emextras.common.block.attribute.EMExtraAttribute;
import io.github.masyumero.emextras.common.tier.EMExtraFactoryTier;

import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.registration.impl.BlockRegistryObject;
import net.minecraft.data.PackOutput;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import io.github.masyumero.emextras.datagen.client.models.BaseAssetProvider;

public abstract class BaseItemModelProvider extends BaseAssetProvider {

    public BaseItemModelProvider(PackOutput output, String modid) {
        super(output, modid);
    }

    protected abstract void registerModels();

    @Override
    protected final void registerAssets() {
        registerModels();
    }

    // port-26.1.2: items/ contains the client item definition, models/item/ the mesh.
    protected void withExistingParent(String name, Identifier parent) {
        JsonObject model = reference("type", Identifier.withDefaultNamespace("model"));
        model.addProperty("model", parent.toString());
        JsonObject item = new JsonObject();
        item.add("model", model);
        asset("items", Identifier.fromNamespaceAndPath(modid, name), item);
    }

    protected void basicItem(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        Identifier modelId = id.withPrefix("item/");
        JsonObject mesh = reference("parent", Identifier.withDefaultNamespace("item/generated"));
        mesh.add("textures", reference("layer0", modelId));
        asset("models", modelId, mesh);
        withExistingParent(id.getPath(), modelId);
    }

    public void factoryBlock(BlockRegistryObject<?, ?> blockRO) {
        FactoryType type = Attribute.get(blockRO, AttributeFactoryType.class).getFactoryType();
        EMExtraFactoryTier tier = EMExtraAttribute.getEMExtraTier(blockRO, EMExtraFactoryTier.class);

        this.withExistingParent(blockRO.getName(), EMExtras.rl("block/factory/" + type.getRegistryNameComponent() +  "/" + tier.getEMExtraTier().getLowerName()));
    }

    public void extraFactoryBlock(BlockRegistryObject<?, ?> blockRO) {
        FactoryType type = Attribute.get(blockRO, AttributeFactoryType.class).getFactoryType();
        ExtraFactoryTier tier = ExtraAttribute.getAdvancedTier(blockRO, ExtraFactoryTier.class);

        this.withExistingParent(blockRO.getName(), EMExtras.rl("block/factory/" + type.getRegistryNameComponent() +  "/" + tier.getAdvanceTier().getLowerName()));
    }

    public void advancedFactoryBlock(BlockRegistryObject<?, ?> blockRO) {
        AdvancedFactoryType type = Attribute.get(blockRO, AttributeAdvancedFactoryType.class).getAdvancedFactoryType();
        EMExtraFactoryTier tier = EMExtraAttribute.getEMExtraTier(blockRO, EMExtraFactoryTier.class);

        this.withExistingParent(blockRO.getName(), EMExtras.rl("block/factory/" + type.getRegistryNameComponent() +  "/" + tier.getEMExtraTier().getLowerName()));
    }

    public void moreMachineFactoryBlock(BlockRegistryObject<?, ?> blockRO) {
        MoreMachineFactoryType type = Attribute.get(blockRO, MoreMachineAttributeFactoryType.class).getMoreMachineFactoryType();
        EMExtraFactoryTier tier = EMExtraAttribute.getEMExtraTier(blockRO, EMExtraFactoryTier.class);

        this.withExistingParent(blockRO.getName(), EMExtras.rl("block/factory/" + type.getRegistryNameComponent() +  "/" + tier.getEMExtraTier().getLowerName()));
    }
}
