package io.github.masyumero.emextras.datagen;

import com.google.common.hash.Hashing;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

// port-26.1.2: separate server/client generators must not purge the other side's output.
public record ExistingOutputProvider(PackOutput output, String directory) implements DataProvider {
    @Override
    @SuppressWarnings("UnstableApiUsage")
    public CompletableFuture<?> run(CachedOutput cache) {
        Path root = output.getOutputFolder().resolve(directory);
        if (Files.isDirectory(root)) {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    byte[] bytes = Files.readAllBytes(path);
                    cache.writeIfNeeded(path, bytes, Hashing.sha1().hashBytes(bytes));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String getName() {
        return "Existing " + directory + " from the other datagen run";
    }
}
