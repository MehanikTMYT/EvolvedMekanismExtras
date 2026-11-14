package io.github.masyumero.emextras.datagen.client.models;

import com.google.gson.JsonObject;
import net.minecraft.data.*;
import net.minecraft.resources.Identifier;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

// port-26.1.2: NeoForge removed the old model-generator DSL; emit assets via DataProvider.
public abstract class BaseAssetProvider implements DataProvider {
    private final PackOutput output;
    protected final String modid;
    private final Map<Path, JsonObject> assets = new LinkedHashMap<>();

    protected BaseAssetProvider(PackOutput output, String modid) {
        this.output = output;
        this.modid = modid;
    }

    protected abstract void registerAssets();

    protected void asset(String directory, Identifier id, JsonObject json) {
        assets.put(output.createPathProvider(PackOutput.Target.RESOURCE_PACK, directory).json(id), json);
    }

    protected static JsonObject reference(String key, Identifier id) {
        JsonObject json = new JsonObject();
        json.addProperty(key, id.toString());
        return json;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        assets.clear();
        registerAssets();
        return CompletableFuture.allOf(assets.entrySet().stream()
                .map(entry -> DataProvider.saveStable(cache, entry.getValue(), entry.getKey()))
                .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return getClass().getSimpleName() + ": " + modid;
    }
}
