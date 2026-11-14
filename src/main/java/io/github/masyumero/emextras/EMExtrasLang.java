package io.github.masyumero.emextras;

// port-26.1.2: adapt MC 26 and Mekanism 10.8 signatures while preserving behavior.
import lombok.Getter;
import mekanism.api.text.ILangEntry;
import net.minecraft.util.Util;

@Getter
public enum EMExtrasLang implements ILangEntry {
    TAB("constants", "mod_name");

    private final String translationKey;
    EMExtrasLang(String type, String path) {
        this(Util.makeDescriptionId(type, EMExtras.rl(path)));
    }

    EMExtrasLang(String translationKey) {
        this.translationKey = translationKey;
    }
}
