package io.github.masyumero.emextras.datagen;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.InMemoryCommentedFormat;
import com.electronwill.nightconfig.core.concurrent.SynchronizedConfig;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import io.github.masyumero.emextras.EMExtras;
import io.github.masyumero.emextras.datagen.client.lang.EMExtraLangProvider;
import io.github.masyumero.emextras.datagen.client.models.block.EMExtraBlockModelProvider;
import io.github.masyumero.emextras.datagen.client.models.item.EMExtraItemModelProvider;
import io.github.masyumero.emextras.datagen.common.loot.EMExtraLootProvider;
import io.github.masyumero.emextras.datagen.common.recipe.impl.EMExtraRecipeProvider;
import io.github.masyumero.emextras.datagen.common.tag.EMExtraTagProvider;
import mekanism.common.Mekanism;
import mekanism.common.lib.FieldReflectionHelper;
import net.minecraft.util.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.DeferredWorkQueue;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@EventBusSubscriber(modid = EMExtras.MODID)
public class EMExtrasDataGenerator {

    @SuppressWarnings("UnstableApiUsage")
    private static final FieldReflectionHelper<ConfigTracker, EnumMap<ModConfig.Type, Set<ModConfig>>> CONFIG_SETS =
            new FieldReflectionHelper<>(ConfigTracker.class, "configSets", () -> new EnumMap<>(ModConfig.Type.class));
    private static final Constructor<?> LOADED_CONFIG;
    private static final Method SET_CONFIG;

    static {
        Class<?> loadedConfig;
        try {
            loadedConfig = Class.forName("net.neoforged.fml.config.LoadedConfig");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        LOADED_CONFIG = ObfuscationReflectionHelper.findConstructor(loadedConfig, CommentedConfig.class, Path.class, ModConfig.class);
        SET_CONFIG = ObfuscationReflectionHelper.findMethod(ModConfig.class, "setConfig", loadedConfig, Function.class);
    }

    private EMExtrasDataGenerator() {
    }

    // port-26.1.2: subscribe to concrete events; server data cannot load client providers.
    @SubscribeEvent
    public static void gatherServerData(GatherDataEvent.Server event) {
        // port-26.1.2: fake factory tiles also read dependency configs during loot generation.
        for (String modid : List.of(Mekanism.MODID, EMExtras.MODID, "evolvedmekanism", "mekanism_extras", "mekmm")) {
            bootstrapConfigs(modid);
        }
        bootstrapIMC();
        PackOutput output = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        event.addProvider(new ExistingOutputProvider(output, "assets"));
        event.addProvider(new EMExtraLootProvider(output, lookup));
        event.addProvider(new EMExtraTagProvider(output, lookup));
        event.addProvider(new EMExtraRecipeProvider(output, lookup));
    }

    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        PackOutput output = event.getGenerator().getPackOutput();
        event.addProvider(new ExistingOutputProvider(output, "data"));
        event.addProvider(new EMExtraLangProvider(output));
        event.addProvider(new EMExtraBlockModelProvider(output));
        event.addProvider(new EMExtraItemModelProvider(output));
    }

    /**
     * Used to bootstrap configs to their default values so that if we are querying if things exist we don't have issues with it happening to early or in cases we have
     * fake tiles.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static void bootstrapConfigs(String modid) {
        for (Set<ModConfig> configs : CONFIG_SETS.getValue(ConfigTracker.INSTANCE).values()) {
            for (ModConfig config : configs) {
                if (config.getModId().equals(modid)) {
                    //Similar to how ConfigTracker#loadDefaultServerConfigs works for loading default server configs on the client
                    // except we don't bother firing an event as it is private, and we are already at defaults if we had called earlier,
                    // and we also don't fully initialize the mod config as the spec is what we care about, and we can do so without having
                    // to reflect into package private methods
                    CommentedConfig commentedConfig = new SynchronizedConfig(InMemoryCommentedFormat.defaultInstance(), LinkedHashMap::new);
                    config.getSpec().correct(commentedConfig);
                    try {
                        SET_CONFIG.invoke(config, LOADED_CONFIG.newInstance(commentedConfig, null, config),
                                (Function<ModConfig, ModConfigEvent>) ModConfigEvent.Loading::new);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private static void bootstrapIMC() {
        List<ModContainer> mods = new ArrayList<>();
        DeferredWorkQueue enqueueIMC = new DeferredWorkQueue("IMC Bootstrap: Enqueue IMC");
        for (ModContainer mod : ModList.get().getSortedMods()) {
            //Handle all our modules
            if (mod.getModId().startsWith(Mekanism.MODID)) {
                mods.add(mod);
                mod.getEventBus().post(new InterModEnqueueEvent(mod, enqueueIMC));
            }
        }
        enqueueIMC.runTasks();
        DeferredWorkQueue processIMC = new DeferredWorkQueue("IMC Bootstrap: Process IMC");
        for (ModContainer mod : mods) {
            mod.getEventBus().post(new InterModProcessEvent(mod, processIMC));
        }
        processIMC.runTasks();
    }

    /**
     * Basically a copy of {@link DataProvider#saveStable(CachedOutput, JsonElement, Path)} but it takes a consumer of the output stream instead of serializes json using
     * GSON. Use it to write arbitrary files.
     */
    @SuppressWarnings({"UnstableApiUsage", "deprecation"})
    public static CompletableFuture<?> save(CachedOutput cache, IOConsumer<OutputStream> osConsumer, Path path) {
        return CompletableFuture.runAsync(() -> {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                 HashingOutputStream hashingOutputStream = new HashingOutputStream(Hashing.sha1(), outputStream)) {
                osConsumer.accept(hashingOutputStream);
                cache.writeIfNeeded(path, outputStream.toByteArray(), hashingOutputStream.hash());
            } catch (IOException ioexception) {
                DataProvider.LOGGER.error("Failed to save file to {}", path, ioexception);
            }
        }, Util.backgroundExecutor());
    }

    @FunctionalInterface
    public interface IOConsumer<T> {

        void accept(T value) throws IOException;
    }
}
