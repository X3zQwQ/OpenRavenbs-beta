package org.spongepowered.asm.mixin.extensibility;

import java.util.List;
import java.util.Set;
import org.spongepowered.asm.lib.tree.ClassNode;

public interface IMixinConfigPlugin {

    void onLoad(String text);

    String getRefMapperConfig();

    boolean shouldApplyMixin(String text2, String text3);

    void acceptTargets(Set<String> set, Set<String> set2);

    List<String> getMixins();

    void preApply(String text4, ClassNode classNode, String text5, IMixinInfo iMixinInfo);

    void postApply(String text6, ClassNode classNode2, String text7, IMixinInfo iMixinInfo2);
}
