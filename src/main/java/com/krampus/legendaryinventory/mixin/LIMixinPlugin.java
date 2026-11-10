package com.krampus.legendaryinventory.mixin;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class LIMixinPlugin implements IMixinConfigPlugin {

    private static final String CLIENT_COMPAT_PACKAGE = ".mixin.client.compat.";
    private static final String COMPAT_PACKAGE = ".mixin.compat.";
    private static final String LOOT_JOURNAL = "LootJournal";
    private static final String AE2 = "AE2";
    private static final String JEI = "Jei";
    private static final String QUARK = "Quark";

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.contains(CLIENT_COMPAT_PACKAGE) && !mixinClassName.contains(COMPAT_PACKAGE)) {
            return true;
        }
        if (mixinClassName.contains(LOOT_JOURNAL)) {
            return LoadingModList.get().getModFileById("loot_journal") != null;
        }
        if (mixinClassName.contains(AE2)) {
            return LoadingModList.get().getModFileById("ae2") != null;
        }
        if (mixinClassName.contains(JEI)) {
            return LoadingModList.get().getModFileById("jei") != null;
        }
        if (mixinClassName.contains(QUARK)) {
            return LoadingModList.get().getModFileById("quark") != null;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
