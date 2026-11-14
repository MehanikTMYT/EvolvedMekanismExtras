package io.github.masyumero.emextras.common.integration;

// port-26.1.2: adapt MC 26 and Mekanism 10.8 signatures while preserving behavior.
import net.minecraft.resources.Identifier;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class EMExtraHooks {

    public record IntegrationInfo(String modid, boolean isLoaded) {

        private IntegrationInfo(String modid, Predicate<String> loadedCheck) {
            this(modid, loadedCheck.test(modid));
        }

        private void sendImc(String method, Supplier<?> toSend) {
            InterModComms.sendTo(modid, method, toSend);
        }

        public Identifier rl(String path) {
            return Identifier.fromNamespaceAndPath(modid, path);
        }

        public void assertLoaded() {
            if (!isLoaded) {
                throw new IllegalStateException(modid + " is not loaded");
            }
        }
    }

    public final IntegrationInfo mekmm;

    public EMExtraHooks() {
        ModList modList = ModList.get();
        // Note: The modlist is null when running tests
        Predicate<String> loadedCheck = modList == null ? modid -> false : modList::isLoaded;
        mekmm = new IntegrationInfo("mekmm", loadedCheck);
    }
}